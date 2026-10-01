import { createHash, randomBytes, timingSafeEqual } from "node:crypto";
import { promises as fs } from "node:fs";
import { createServer } from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = path.dirname(fileURLToPath(import.meta.url));
const distRoot = path.join(projectRoot, "dist");
const sessionCookieName = "sentinel_bff_session";
const sessionTtlMs = 8 * 60 * 60 * 1000;
const maxRequestBodyBytes = 1024 * 1024;

const contentTypes = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".webp": "image/webp",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
};

function createConfig(overrides = {}) {
  const config = {
    port: Number(process.env.PORT || 3000),
    backendUrl: process.env.SENTINEL_BACKEND_URL || "",
    apiKey: process.env.SENTINEL_API_KEY,
    username: process.env.BFF_USERNAME,
    password: process.env.BFF_PASSWORD,
    secureCookies: process.env.NODE_ENV === "production",
    ...overrides,
  };

  config.backendUrl = config.backendUrl.replace(/\/+$/, "");
  return config;
}

function assertRequiredConfig(config) {
  const missing = [
    ["SENTINEL_BACKEND_URL", config.backendUrl],
    ["SENTINEL_API_KEY", config.apiKey],
    ["BFF_USERNAME", config.username],
    ["BFF_PASSWORD", config.password],
  ]
    .filter(([, value]) => typeof value !== "string" || value.length === 0)
    .map(([name]) => name);

  if (missing.length > 0) {
    throw new Error(`Missing required BFF configuration: ${missing.join(", ")}`);
  }

  try {
    const backendUrl = new URL(config.backendUrl);
    if (!/^https?:$/.test(backendUrl.protocol)) throw new Error();
  } catch {
    throw new Error("SENTINEL_BACKEND_URL must be an HTTP(S) URL");
  }
}

function setSecurityHeaders(response) {
  response.setHeader("X-Content-Type-Options", "nosniff");
  response.setHeader("X-Frame-Options", "DENY");
  response.setHeader("Referrer-Policy", "no-referrer");
}

function writeJson(response, statusCode, body, headers = {}) {
  setSecurityHeaders(response);
  response.statusCode = statusCode;
  response.setHeader("Content-Type", "application/json; charset=utf-8");
  response.setHeader("Cache-Control", "no-store");
  Object.entries(headers).forEach(([name, value]) => {
    response.setHeader(name, value);
  });
  response.end(JSON.stringify(body));
}

function writeError(response, statusCode, message) {
  writeJson(response, statusCode, { error: message });
}

function hashSecret(value) {
  return createHash("sha256").update(value, "utf8").digest();
}

function secretsMatch(actual, expected) {
  const actualHash = hashSecret(actual);
  const expectedHash = hashSecret(expected);
  return timingSafeEqual(actualHash, expectedHash);
}

function parseCookies(request) {
  const cookies = {};
  const header = request.headers.cookie || "";

  header.split(";").forEach((part) => {
    const separator = part.indexOf("=");
    if (separator === -1) return;

    const name = part.slice(0, separator).trim();
    const value = part.slice(separator + 1).trim();
    if (!name) return;

    try {
      cookies[name] = decodeURIComponent(value);
    } catch {
      // Ignore malformed cookie values and let protected routes require a new session.
    }
  });

  return cookies;
}

function sessionCookie(token, secureCookies) {
  const attributes = [
    `${sessionCookieName}=${encodeURIComponent(token)}`,
    "Path=/",
    `Max-Age=${sessionTtlMs / 1000}`,
    "HttpOnly",
    "SameSite=Strict",
  ];

  if (secureCookies) attributes.push("Secure");
  return attributes.join("; ");
}

function clearedSessionCookie(secureCookies) {
  const attributes = [
    `${sessionCookieName}=`,
    "Path=/",
    "Max-Age=0",
    "HttpOnly",
    "SameSite=Strict",
  ];

  if (secureCookies) attributes.push("Secure");
  return attributes.join("; ");
}

function getActiveSession(request, sessions) {
  const token = parseCookies(request)[sessionCookieName];
  if (!token) return false;

  const expiresAt = sessions.get(token);
  if (!expiresAt || expiresAt <= Date.now()) {
    sessions.delete(token);
    return false;
  }

  sessions.set(token, Date.now() + sessionTtlMs);
  return true;
}

function readBody(request) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];

    request.on("data", (chunk) => {
      size += chunk.length;
      if (size > maxRequestBodyBytes) {
        reject(new Error("Request body is too large"));
        request.destroy();
        return;
      }
      chunks.push(chunk);
    });

    request.on("end", () => resolve(Buffer.concat(chunks)));
    request.on("error", reject);
  });
}

async function readJsonBody(request, response) {
  let body;
  try {
    body = await readBody(request);
  } catch {
    writeError(response, 413, "Request body is too large");
    return null;
  }

  try {
    return JSON.parse(body.toString("utf8"));
  } catch {
    writeError(response, 400, "Request body must be valid JSON");
    return null;
  }
}

function matchProxyRoute(method, pathname) {
  if (method === "GET" && pathname === "/bff/transactions") {
    return { backendPath: "/api/transactions" };
  }

  if (
    method === "GET" &&
    pathname === "/bff/transactions/dashboard/stats"
  ) {
    return { backendPath: "/api/transactions/dashboard/stats" };
  }

  if (method === "POST" && pathname === "/bff/transactions/analyze") {
    return { backendPath: "/api/transactions/analyze" };
  }

  const fraudActionMatch = pathname.match(
    /^\/bff\/transactions\/(\d+)\/fraud-action$/
  );
  if (method === "PUT" && fraudActionMatch) {
    return {
      backendPath: `/api/transactions/${fraudActionMatch[1]}/fraud-action`,
    };
  }

  return null;
}

async function proxyToSentinel(
  request,
  response,
  route,
  config,
  fetchImpl
) {
  let body;
  if (request.method === "POST" || request.method === "PUT") {
    try {
      body = await readBody(request);
    } catch {
      writeError(response, 413, "Request body is too large");
      return;
    }
  }

  const headers = {
    Accept: "application/json",
    "X-API-Key": config.apiKey,
  };
  if (body) headers["Content-Type"] = "application/json";

  let sentinelResponse;
  try {
    sentinelResponse = await fetchImpl(
      `${config.backendUrl}${route.backendPath}`,
      {
        method: request.method,
        headers,
        body,
      }
    );
  } catch {
    writeError(response, 502, "Sentinel backend unavailable");
    return;
  }

  const responseBody = Buffer.from(await sentinelResponse.arrayBuffer());
  setSecurityHeaders(response);
  response.statusCode = sentinelResponse.status;
  response.setHeader("Cache-Control", "no-store");

  const contentType = sentinelResponse.headers.get("content-type");
  if (contentType) response.setHeader("Content-Type", contentType);

  response.end(responseBody);
}

async function handleLogin(request, response, config, sessions) {
  const credentials = await readJsonBody(request, response);
  if (!credentials) return;

  const username = typeof credentials.username === "string"
    ? credentials.username
    : "";
  const password = typeof credentials.password === "string"
    ? credentials.password
    : "";

  if (
    !secretsMatch(username, config.username) ||
    !secretsMatch(password, config.password)
  ) {
    writeError(response, 401, "Invalid dashboard credentials");
    return;
  }

  const token = randomBytes(32).toString("base64url");
  sessions.set(token, Date.now() + sessionTtlMs);
  writeJson(
    response,
    200,
    { authenticated: true },
    { "Set-Cookie": sessionCookie(token, config.secureCookies) }
  );
}

function handleLogout(request, response, config, sessions) {
  const token = parseCookies(request)[sessionCookieName];
  if (token) sessions.delete(token);

  writeJson(
    response,
    200,
    { authenticated: false },
    { "Set-Cookie": clearedSessionCookie(config.secureCookies) }
  );
}

async function serveStatic(request, response) {
  if (request.method !== "GET" && request.method !== "HEAD") {
    writeError(response, 404, "Not found");
    return;
  }

  const requestUrl = new URL(request.url, "http://bff.local");
  let relativePath;
  try {
    relativePath = decodeURIComponent(requestUrl.pathname);
  } catch {
    writeError(response, 400, "Invalid URL");
    return;
  }

  const candidate = path.resolve(distRoot, `.${relativePath}`);
  const normalizedRoot = `${path.resolve(distRoot)}${path.sep}`;
  if (candidate !== path.resolve(distRoot) && !candidate.startsWith(normalizedRoot)) {
    writeError(response, 403, "Forbidden");
    return;
  }

  let filePath = candidate;
  try {
    const stat = await fs.stat(filePath);
    if (stat.isDirectory()) filePath = path.join(filePath, "index.html");
  } catch {
    filePath = path.join(distRoot, "index.html");
  }

  let file;
  try {
    file = await fs.readFile(filePath);
  } catch {
    writeError(response, 503, "Frontend build is unavailable");
    return;
  }

  setSecurityHeaders(response);
  response.statusCode = 200;
  response.setHeader(
    "Content-Type",
    contentTypes[path.extname(filePath).toLowerCase()] ||
      "application/octet-stream"
  );
  response.setHeader("Cache-Control", "no-cache");
  if (request.method === "HEAD") response.end();
  else response.end(file);
}

export function createBffServer(overrides = {}, fetchImpl = fetch) {
  const config = createConfig(overrides);
  assertRequiredConfig(config);
  const sessions = new Map();

  return createServer(async (request, response) => {
    try {
      const requestUrl = new URL(request.url, "http://bff.local");
      const { pathname } = requestUrl;

      if (pathname === "/bff/auth/login" && request.method === "POST") {
        await handleLogin(request, response, config, sessions);
        return;
      }

      if (pathname === "/bff/auth/logout" && request.method === "POST") {
        handleLogout(request, response, config, sessions);
        return;
      }

      if (pathname.startsWith("/bff/")) {
        if (!getActiveSession(request, sessions)) {
          writeError(response, 401, "Authentication required");
          return;
        }

        const route = matchProxyRoute(request.method, pathname);
        if (!route) {
          writeError(response, 404, "BFF route not found");
          return;
        }

        await proxyToSentinel(
          request,
          response,
          route,
          config,
          fetchImpl
        );
        return;
      }

      await serveStatic(request, response);
    } catch {
      if (!response.headersSent) writeError(response, 500, "BFF request failed");
      else response.destroy();
    }
  });
}

const isMainModule =
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isMainModule) {
  const config = createConfig();
  const server = createBffServer(config);
  server.listen(config.port, () => {
    console.log(`Sentinel BFF listening on port ${config.port}`);
  });
}
