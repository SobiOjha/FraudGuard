const BASE_URL = "http://localhost:8080/api/transactions";

export async function analyzeTransaction(transactionData) {
    const response = await fetch(`${BASE_URL}/analyze`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(transactionData),
    });

    if (!response.ok) {
        throw new Error("Failed to analyze transaction");
    }

    return response.json();
}

export async function getTransactions() {
    const response = await fetch(BASE_URL);

    if (!response.ok) {
        throw new Error("Failed to fetch transactions");
    }

    return response.json();
}

export async function getDashboardStats() {
    const response = await fetch(
        `${BASE_URL}/dashboard/stats`
    );

    if (!response.ok) {
        throw new Error(
            "Failed to fetch dashboard statistics"
        );
    }

    return response.json();
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
            body: JSON.stringify({
                action,
            }),
        }
    );

    if (!response.ok) {
        throw new Error(
            "Failed to update fraud action"
        );
    }

    return response.json();
}