const BASE_URL = "/bff/transactions";

async function parseResponse(response, fallbackMessage) {
    if (response.ok) return response.json();

    const error = new Error(fallbackMessage);
    error.status = response.status;

    try {
        const body = await response.json();
        if (body?.error) error.message = body.error;
    } catch {
        // Keep the safe fallback message when the response is not JSON.
    }

    throw error;
}

export async function login(username, password) {
    const response = await fetch("/bff/auth/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        credentials: "same-origin",
        body: JSON.stringify({ username, password }),
    });

    return parseResponse(response, "Dashboard authentication failed");
}

export async function logout() {
    const response = await fetch("/bff/auth/logout", {
        method: "POST",
        credentials: "same-origin",
    });

    return parseResponse(response, "Dashboard logout failed");
}

export async function analyzeTransaction(transactionData) {
    const response = await fetch(`${BASE_URL}/analyze`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        credentials: "same-origin",
        body: JSON.stringify(transactionData),
    });

    return parseResponse(response, "Failed to analyze transaction");
}

export async function getTransactions() {
    const response = await fetch(BASE_URL, {
        credentials: "same-origin",
    });

    return parseResponse(response, "Failed to fetch transactions");
}

export async function getDashboardStats() {
    const response = await fetch(
        `${BASE_URL}/dashboard/stats`,
        { credentials: "same-origin" }
    );

    return parseResponse(response, "Failed to fetch dashboard statistics");
}

export async function updateFraudAction(
    transactionId,
    action
) {
    const response = await fetch(
        `${BASE_URL}/${transactionId}/fraud-action`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "same-origin",
            body: JSON.stringify({
                action,
            }),
        }
    );

    return parseResponse(response, "Failed to update fraud action");
}
