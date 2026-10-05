import assert from "node:assert/strict";
import { afterEach, test } from "node:test";

import { createBffServer } from "./server.mjs";

const serverApiKey = "server-only-test-api-key";
const credentials = {
  username: "dashboard-admin",
  password: "dashboard-password",
};

const runningServers = [];

afterEach(async () => {
  await Promise.all(
    runningServers.splice(0).map(
      (server) => new Promise((resolve) => server.close(resolve))
    )
  );
});

async function startServer(fetchImpl, overrides = {}) {
  const server = createBffServer(
    {
      backendUrl: "https://sentinel.example",
      apiKey: serverApiKey,
      username: credentials.username,
      password: credentials.password,
      secureCookies: false,
      ...overrides,
    },
    fetchImpl
  );

  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  runningServers.push(server);
  const address = server.address();
  return `http://127.0.0.1:${address.port}`;
}

async function login(baseUrl) {
  const response = await fetch(`${baseUrl}/bff/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });

  assert.equal(response.status, 200);
  const setCookie = response.headers.get("set-cookie");
  assert.match(setCookie, /HttpOnly/);
  assert.match(setCookie, /SameSite=Strict/);
  return setCookie.split(";", 1)[0];
}

function sentinelResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

test("rejects unauthenticated BFF requests", async () => {
  const calls = [];
  const baseUrl = await startServer(async (...args) => {
    calls.push(args);
    return sentinelResponse([]);
  });

  const response = await fetch(`${baseUrl}/bff/transactions`);
  assert.equal(response.status, 401);
  assert.deepEqual(await response.json(), { error: "Authentication required" });
  assert.equal(calls.length, 0);
});

test("authenticates the browser and proxies GET with the server-only API key", async () => {
  const calls = [];
  const baseUrl = await startServer(async (...args) => {
    calls.push(args);
    return sentinelResponse([{ id: 1 }]);
  });

  const cookie = await login(baseUrl);
  const response = await fetch(`${baseUrl}/bff/transactions`, {
    headers: {
      Cookie: cookie,
      "X-API-Key": "browser-supplied-key-must-be-ignored",
    },
  });

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), [{ id: 1 }]);
  assert.equal(calls.length, 1);
  assert.equal(calls[0][0], "https://sentinel.example/api/transactions");
  assert.equal(calls[0][1].headers["X-API-Key"], serverApiKey);
  assert.notEqual(
    calls[0][1].headers["X-API-Key"],
    "browser-supplied-key-must-be-ignored"
  );
});

test("proxies the supported POST and PUT routes", async () => {
  const calls = [];
  const baseUrl = await startServer(async (...args) => {
    calls.push(args);
    return sentinelResponse({ ok: true });
  });

  const cookie = await login(baseUrl);
  const payload = {
    userId: "U1001",
    amount: 1000,
    currency: "INR",
    recipient: "RECIPIENT-01",
    transactionType: "TRANSFER",
    location: "Delhi",
    deviceId: "DEVICE-01",
    protectionMode: "NORMAL",
  };

  assert.equal(
    (await fetch(`${baseUrl}/bff/transactions/analyze`, {
      method: "POST",
      headers: { Cookie: cookie, "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    })).status,
    200
  );

  assert.equal(
    (await fetch(`${baseUrl}/bff/transactions/42/fraud-action`, {
      method: "PUT",
      headers: { Cookie: cookie, "Content-Type": "application/json" },
      body: JSON.stringify({ action: "BLOCK" }),
    })).status,
    200
  );

  assert.equal(calls.length, 2);
  assert.equal(calls[0][0], "https://sentinel.example/api/transactions/analyze");
  assert.equal(calls[1][0], "https://sentinel.example/api/transactions/42/fraud-action");
  assert.equal(calls[0][1].headers["X-API-Key"], serverApiKey);
  assert.equal(calls[1][1].headers["X-API-Key"], serverApiKey);
});

test("propagates Sentinel errors without exposing the API key", async () => {
  const baseUrl = await startServer(async () =>
    sentinelResponse({ error: "Invalid API key" }, 401)
  );

  const cookie = await login(baseUrl);
  const response = await fetch(`${baseUrl}/bff/transactions/dashboard/stats`, {
    headers: { Cookie: cookie },
  });
  const body = await response.text();

  assert.equal(response.status, 401);
  assert.match(body, /Invalid API key/);
  assert.equal(body.includes(serverApiKey), false);
});

test("rejects arbitrary proxy paths, including admin and auth paths", async () => {
  const calls = [];
  const baseUrl = await startServer(async (...args) => {
    calls.push(args);
    return sentinelResponse({ unexpected: true });
  });
  const cookie = await login(baseUrl);

  for (const path of [
    "/bff/api/integrations",
    "/bff/api/auth/login",
    "/bff/transactions/42",
  ]) {
    const response = await fetch(`${baseUrl}${path}`, {
      headers: { Cookie: cookie },
    });
    assert.equal(response.status, 404, path);
  }

  assert.equal(calls.length, 0);
});

test("marks the session cookie Secure in production mode", async () => {
  const baseUrl = await startServer(
    async () => sentinelResponse({ ok: true }),
    { secureCookies: true }
  );

  const response = await fetch(`${baseUrl}/bff/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });

  assert.equal(response.status, 200);
  assert.match(response.headers.get("set-cookie"), /Secure/);
});

test("logout invalidates the browser session", async () => {
  const baseUrl = await startServer(async () => sentinelResponse([]));
  const cookie = await login(baseUrl);

  const logoutResponse = await fetch(`${baseUrl}/bff/auth/logout`, {
    method: "POST",
    headers: { Cookie: cookie },
  });
  assert.equal(logoutResponse.status, 200);

  const protectedResponse = await fetch(`${baseUrl}/bff/transactions`, {
    headers: { Cookie: cookie },
  });
  assert.equal(protectedResponse.status, 401);
});

test("malformed session cookies do not cause a server error", async () => {
  const baseUrl = await startServer(async () => sentinelResponse([]));

  const response = await fetch(`${baseUrl}/bff/transactions`, {
    headers: { Cookie: "sentinel_bff_session=%invalid" },
  });

  assert.equal(response.status, 401);
});

test("logs correlation-tracked diagnostic info for successful upstream responses without exposing secrets", async () => {
  const logs = [];
  const mockLogger = {
    log: (...args) => logs.push(args.join(" ")),
    error: (...args) => logs.push(args.join(" ")),
  };

  const baseUrl = await startServer(
    async () => sentinelResponse({ totalTransactions: 10 }),
    { logger: mockLogger }
  );

  const cookie = await login(baseUrl);
  const response = await fetch(`${baseUrl}/bff/transactions/dashboard/stats`, {
    headers: { Cookie: cookie },
  });

  assert.equal(response.status, 200);
  assert.equal(logs.length, 1);
  const logLine = logs[0];
  assert.match(
    logLine,
    /^\[BFF proxy\] id=[0-9a-f]{12} route=\/api\/transactions\/dashboard\/stats upstream_status=200 duration_ms=\d+$/
  );
  assert.equal(logLine.includes(serverApiKey), false);
  assert.equal(logLine.includes(credentials.password), false);
  assert.equal(logLine.includes(cookie), false);
});

test("logs correlation-tracked diagnostic info for upstream 429 responses without exposing secrets", async () => {
  const logs = [];
  const mockLogger = {
    log: (...args) => logs.push(args.join(" ")),
    error: (...args) => logs.push(args.join(" ")),
  };

  const baseUrl = await startServer(
    async () =>
      new Response("Too Many Requests", {
        status: 429,
        headers: { "Content-Type": "text/plain" },
      }),
    { logger: mockLogger }
  );

  const cookie = await login(baseUrl);
  const response = await fetch(`${baseUrl}/bff/transactions/dashboard/stats`, {
    headers: { Cookie: cookie },
  });

  assert.equal(response.status, 429);
  assert.equal(await response.text(), "Too Many Requests");
  assert.equal(logs.length, 1);
  const logLine = logs[0];
  assert.match(
    logLine,
    /^\[BFF proxy\] id=[0-9a-f]{12} route=\/api\/transactions\/dashboard\/stats upstream_status=429 duration_ms=\d+$/
  );
  assert.equal(logLine.includes(serverApiKey), false);
  assert.equal(logLine.includes(credentials.password), false);
  assert.equal(logLine.includes(cookie), false);
  assert.equal(logLine.includes("Too Many Requests"), false);
});

test("logs safe transport error category and returns 502 when upstream is unreachable", async () => {
  const logs = [];
  const mockLogger = {
    log: (...args) => logs.push(args.join(" ")),
    error: (...args) => logs.push(args.join(" ")),
  };

  const baseUrl = await startServer(
    async () => {
      const err = new Error("connect ECONNREFUSED 127.0.0.1:8080");
      err.code = "ECONNREFUSED";
      throw err;
    },
    { logger: mockLogger }
  );

  const cookie = await login(baseUrl);
  const response = await fetch(`${baseUrl}/bff/transactions`, {
    headers: { Cookie: cookie },
  });

  assert.equal(response.status, 502);
  assert.deepEqual(await response.json(), {
    error: "Sentinel backend unavailable",
  });
  assert.equal(logs.length, 1);
  const logLine = logs[0];
  assert.match(
    logLine,
    /^\[BFF proxy\] id=[0-9a-f]{12} route=\/api\/transactions upstream_error=connection_refused duration_ms=\d+$/
  );
  assert.equal(logLine.includes(serverApiKey), false);
  assert.equal(logLine.includes(credentials.password), false);
  assert.equal(logLine.includes(cookie), false);
  assert.equal(logLine.includes("ECONNREFUSED 127.0.0.1:8080"), false);
});

test("logs safe generic fetch_failed category and returns 502 for unrecognized transport errors", async () => {
  const logs = [];
  const mockLogger = {
    log: (...args) => logs.push(args.join(" ")),
    error: (...args) => logs.push(args.join(" ")),
  };

  const sensitiveMessage = "internal connection drop with secret token";
  const baseUrl = await startServer(
    async () => {
      throw new Error(sensitiveMessage);
    },
    { logger: mockLogger }
  );

  const cookie = await login(baseUrl);
  const response = await fetch(`${baseUrl}/bff/transactions`, {
    headers: { Cookie: cookie },
  });

  assert.equal(response.status, 502);
  assert.deepEqual(await response.json(), {
    error: "Sentinel backend unavailable",
  });
  assert.equal(logs.length, 1);
  const logLine = logs[0];
  assert.match(
    logLine,
    /^\[BFF proxy\] id=[0-9a-f]{12} route=\/api\/transactions upstream_error=fetch_failed duration_ms=\d+$/
  );
  assert.equal(logLine.includes(serverApiKey), false);
  assert.equal(logLine.includes(credentials.password), false);
  assert.equal(logLine.includes(cookie), false);
  assert.equal(logLine.includes(sensitiveMessage), false);
});
