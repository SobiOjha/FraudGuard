import { useEffect, useState } from "react";
import "./App.css";

import {
  analyzeTransaction,
  getTransactions,
  getDashboardStats,
  updateFraudAction,
} from "./api/transactions";

const scenarios = {
  SAFE: {
    userId: "U1001",
    amount: 500,
    currency: "INR",
    recipient: "AC5678",
    transactionType: "TRANSFER",
    location: "Delhi",
    deviceId: "DEVICE-01",
    isNewRecipient: false,
    isNewDevice: false,
    recentTransactionCount: 1,
    transactionsInTimeWindow: 1,
    repeatedTransactionsToRecipient: 0,
  },

  SUSPICIOUS: {
    userId: "U1001",
    amount: 8000,
    currency: "INR",
    recipient: "AC8899",
    transactionType: "TRANSFER",
    location: "Delhi",
    deviceId: "DEVICE-NEW",
    isNewRecipient: true,
    isNewDevice: true,
    recentTransactionCount: 5,
    transactionsInTimeWindow: 4,
    repeatedTransactionsToRecipient: 2,
  },

  HIGH_RISK: {
    userId: "U1001",
    amount: 75000,
    currency: "INR",
    recipient: "AC9871",
    transactionType: "TRANSFER",
    location: "Mumbai",
    deviceId: "DEVICE-NEW",
    isNewRecipient: true,
    isNewDevice: true,
    recentTransactionCount: 10,
    transactionsInTimeWindow: 8,
    repeatedTransactionsToRecipient: 5,
  },
};

function App() {
  const [stats, setStats] = useState({
    totalTransactions: 0,
    highRiskTransactions: 0,
    averageRiskScore: 0,
    safeTransactions: 0,
    blockedTransactions: 0,
  });

  const [transactions, setTransactions] = useState([]);

  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);

  const [simulationScenario, setSimulationScenario] =
    useState("HIGH_RISK");

  const [protectionMode, setProtectionMode] =
    useState("PROTECTED");

  const [selectedTransaction, setSelectedTransaction] =
    useState(null);

  const [showAllTransactions, setShowAllTransactions] =
    useState(false);

  const [error, setError] = useState("");

  useEffect(() => {
    loadDashboard();
  }, []);

  async function loadDashboard() {
    try {
      setError("");

      const [statsData, transactionsData] =
        await Promise.all([
          getDashboardStats(),
          getTransactions(),
        ]);

      setStats(statsData);

      const sortedTransactions = [...transactionsData].sort(
        (a, b) =>
          new Date(b.analyzedAt) -
          new Date(a.analyzedAt)
      );

      setTransactions(sortedTransactions);
    } catch (err) {
      console.error("Dashboard loading failed:", err);
      setError("Could not load dashboard data.");
    } finally {
      setLoading(false);
    }
  }

  async function handleSimulateTransaction() {
    setAnalyzing(true);
    setError("");

    const scenario = scenarios[simulationScenario];

    try {
      const result = await analyzeTransaction({
        ...scenario,
        protectionMode,
      });

      setSelectedTransaction({
        id: result.transactionId,
        userId: scenario.userId,
        amount: scenario.amount,
        currency: scenario.currency,
        recipient: scenario.recipient,
        transactionType: scenario.transactionType,
        location: scenario.location,
        deviceId: scenario.deviceId,
        newRecipient: scenario.isNewRecipient,
        newDevice: scenario.isNewDevice,
        recentTransactionCount:
          scenario.recentTransactionCount,
        transactionsInTimeWindow:
          scenario.transactionsInTimeWindow,
        repeatedTransactionsToRecipient:
          scenario.repeatedTransactionsToRecipient,
        riskScore: result.riskScore,
        riskLevel: result.riskLevel,
        reasons: result.reasons,
        fraudAction: result.action,
        analyzedAt: new Date().toISOString(),
      });

      await loadDashboard();
    } catch (err) {
      console.error(
        "Transaction analysis failed:",
        err
      );

      setError(
        "Could not connect to Trinetra backend."
      );
    } finally {
      setAnalyzing(false);
    }
  }

  async function handleAction(action) {
    if (!selectedTransaction?.id) {
      return;
    }

    try {
      setError("");

      await updateFraudAction(
        selectedTransaction.id,
        action
      );

      await loadDashboard();

      setSelectedTransaction(null);
    } catch (err) {
      console.error(
        "Failed to update fraud action:",
        err
      );

      setError(
        "Could not update fraud action."
      );
    }
  }

  function getRiskClass(riskLevel) {
    if (
      riskLevel === "HIGH" ||
      riskLevel === "CRITICAL"
    ) {
      return "danger";
    }

    if (riskLevel === "SUSPICIOUS") {
      return "warning";
    }

    return "safe";
  }

  function getActionClass(action) {
    if (action === "BLOCK") {
      return "danger";
    }

    if (action === "FLAG") {
      return "warning";
    }

    return "safe";
  }

  function formatAmount(amount, currency) {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: currency || "INR",
      maximumFractionDigits: 0,
    }).format(amount || 0);
  }

  function formatTime(dateTime) {
    if (!dateTime) {
      return "-";
    }

    const date = new Date(dateTime);

    if (Number.isNaN(date.getTime())) {
      return "-";
    }

    return date.toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  function formatDate(dateTime) {
    if (!dateTime) {
      return "-";
    }

    const date = new Date(dateTime);

    if (Number.isNaN(date.getTime())) {
      return "-";
    }

    return date.toLocaleDateString();
  }

  return (
    <div className="app">

      {/* NAVBAR */}

      <header className="navbar">

        <div className="brand">

          <div className="brand-icon-wrapper">
            <img
              src="/trinetra-icon.jpeg"
              alt="Trinetra Logo"
              className="brand-icon-image"
            />
          </div>

          <img
            src="/trinetra-wordmark.jpeg"
            alt="Trinetra Fraud Detection Application"
            className="brand-wordmark"
          />

        </div>

        <div className="protection-status">
          <span className="status-dot"></span>
          Fraud Protection Active
        </div>

      </header>

      {/* DASHBOARD */}

      <main className="dashboard">

        {/* HEADER */}

        <section className="welcome">

          <div>

            <p className="eyebrow">
              SECURITY OVERVIEW
            </p>

            <h2>
              Your transactions are protected.
            </h2>

            <p className="subtitle">
              Trinetra analyzes transaction
              behavior and detects suspicious
              activity.
            </p>

          </div>

          <div className="simulation-controls">

            <select
              className="simulation-select"
              value={simulationScenario}
              onChange={(event) =>
                setSimulationScenario(
                  event.target.value
                )
              }
            >
              <option value="SAFE">
                Normal Transaction
              </option>

              <option value="SUSPICIOUS">
                Suspicious Transaction
              </option>

              <option value="HIGH_RISK">
                High-Risk Transaction
              </option>
            </select>

            <button
              className="simulate-btn"
              onClick={handleSimulateTransaction}
              disabled={analyzing}
            >
              {analyzing
                ? "Analyzing..."
                : "+ Simulate Transaction"}
            </button>

          </div>

        </section>

        {/* ERROR */}

        {error && (
          <div className="error-banner">
            {error}
          </div>
        )}

        {/* STATS */}

        <section className="stats-grid">

          <div className="stat-card">

            <span className="stat-label">
              Transactions Analyzed
            </span>

            <strong>
              {loading
                ? "..."
                : stats.totalTransactions}
            </strong>

            <span className="stat-info">
              From database
            </span>

          </div>

          <div className="stat-card warning-card">

            <span className="stat-label">
              High Risk Transactions
            </span>

            <strong>
              {loading
                ? "..."
                : stats.highRiskTransactions}
            </strong>

            <span className="stat-info">
              Requires attention
            </span>

          </div>

          <div className="stat-card">

            <span className="stat-label">
              Average Fraud Risk
            </span>

            <strong>
              {loading
                ? "..."
                : Number(
                    stats.averageRiskScore
                  ).toFixed(1)}
            </strong>

            <span className="stat-info">
              Average risk score
            </span>

          </div>

          <div className="stat-card safe-card">

            <span className="stat-label">
              Blocked Transactions
            </span>

            <strong>
              {loading
                ? "..."
                : stats.blockedTransactions}
            </strong>

            <span className="stat-info">
              Prevented by protection policy
            </span>

          </div>

        </section>

        {/* MAIN GRID */}

        <section className="content-grid">

          {/* TRANSACTIONS */}

          <div className="panel">

            <div className="panel-header">

              <div>

                <p className="eyebrow">
                  RECENT ACTIVITY
                </p>

                <h3>
                  {showAllTransactions
                    ? "Transaction History"
                    : "Recent Transactions"}
                </h3>

              </div>

              <button
                className="view-btn"
                onClick={() =>
                  setShowAllTransactions(
                    !showAllTransactions
                  )
                }
              >
                {showAllTransactions
                  ? "Show Recent"
                  : "View All"}
              </button>

            </div>

            <div className="transaction-list">

              {loading ? (

                <p className="empty-text">
                  Loading transactions...
                </p>

              ) : transactions.length === 0 ? (

                <p className="empty-text">
                  No transactions analyzed yet.
                </p>

              ) : (

                (
                  showAllTransactions
                    ? transactions
                    : transactions.slice(0, 6)
                ).map((transaction) => (

                  <div
                    className="transaction-row"
                    key={transaction.id}
                    onClick={() =>
                      setSelectedTransaction(
                        transaction
                      )
                    }
                  >

                    <div className="transaction-main">

                      <div
                        className={`transaction-avatar ${
                          getRiskClass(
                            transaction.riskLevel
                          )
                        }`}
                      >
                        ₹
                      </div>

                      <div>

                        <strong>
                          {formatAmount(
                            transaction.amount,
                            transaction.currency
                          )}
                        </strong>

                        <span>
                          {transaction.recipient ||
                            "Unknown recipient"}
                        </span>

                        <small>
                          {transaction.transactionType ||
                            "Transaction"}
                        </small>

                      </div>

                    </div>

                    <div className="transaction-info">

                      <span>
                        {formatTime(
                          transaction.analyzedAt
                        )}
                      </span>

                      <small>
                        {transaction.location ||
                          "Unknown location"}
                      </small>

                    </div>

                    <div className="transaction-status">

                      <span
                        className={`badge ${getRiskClass(
                          transaction.riskLevel
                        )}`}
                      >
                        {transaction.riskLevel}
                      </span>

                      <span
                        className={`badge action ${getActionClass(
                          transaction.fraudAction
                        )}`}
                      >
                        {transaction.fraudAction}
                      </span>

                    </div>

                  </div>

                ))

              )}

            </div>

          </div>

          {/* PROTECTION MODE */}

          <div className="panel protection-panel">

            <div className="panel-header">

              <div>

                <p className="eyebrow">
                  PROTECTION MODE
                </p>

                <h3>
                  {protectionMode}
                </h3>

              </div>

              <div className="shield-large">
                🛡
              </div>

            </div>

            <p className="protection-description">

              {protectionMode === "NORMAL" &&
                "Transactions are analyzed but no automatic intervention is applied."}

              {protectionMode === "CAUTION" &&
                "Suspicious transactions are flagged for review."}

              {protectionMode === "PROTECTED" &&
                "High-risk transactions are automatically blocked."}

            </p>

            <div className="mode-options">

              <button
                className={`mode-option ${
                  protectionMode === "NORMAL"
                    ? "active"
                    : ""
                }`}
                onClick={() =>
                  setProtectionMode("NORMAL")
                }
              >

                <strong>
                  Normal
                </strong>

                <span>
                  Approve after analysis
                </span>

              </button>

              <button
                className={`mode-option ${
                  protectionMode === "CAUTION"
                    ? "active"
                    : ""
                }`}
                onClick={() =>
                  setProtectionMode("CAUTION")
                }
              >

                <strong>
                  Caution
                </strong>

                <span>
                  Flag suspicious activity
                </span>

              </button>

              <button
                className={`mode-option ${
                  protectionMode === "PROTECTED"
                    ? "active"
                    : ""
                }`}
                onClick={() =>
                  setProtectionMode("PROTECTED")
                }
              >

                <strong>
                  Protected
                </strong>

                <span>
                  Block high-risk activity
                </span>

              </button>

            </div>

          </div>

        </section>

      </main>

      {/* TRANSACTION MODAL */}

      {selectedTransaction && (

        <div
          className="modal-overlay"
          onClick={() =>
            setSelectedTransaction(null)
          }
        >

          <div
            className="analysis-modal"
            onClick={(event) =>
              event.stopPropagation()
            }
          >

            <div className="analysis-header">

              <div>

                <p className="eyebrow">
                  TRANSACTION ANALYSIS
                </p>

                <h2>
                  {formatAmount(
                    selectedTransaction.amount,
                    selectedTransaction.currency
                  )}
                </h2>

                <p className="phone-number">
                  Recipient:{" "}
                  {selectedTransaction.recipient ||
                    "Unknown"}
                </p>

                <div className="call-meta">

                  <span>
                    {selectedTransaction.transactionType ||
                      "TRANSFER"}
                  </span>

                  <span>
                    {selectedTransaction.location ||
                      "Unknown location"}
                  </span>

                  <span>
                    {formatDate(
                      selectedTransaction.analyzedAt
                    )}
                  </span>

                </div>

              </div>

              <button
                className="close-btn"
                onClick={() =>
                  setSelectedTransaction(null)
                }
              >
                ×
              </button>

            </div>

            {/* RISK */}

            <div
              className={`risk-summary ${getRiskClass(
                selectedTransaction.riskLevel
              )}`}
            >

              <div
                className={`risk-score ${getRiskClass(
                  selectedTransaction.riskLevel
                )}`}
              >

                <span>
                  {selectedTransaction.riskScore}
                </span>

                <small>
                  / 100
                </small>

              </div>

              <div>

                <span
                  className={`critical-label ${getRiskClass(
                    selectedTransaction.riskLevel
                  )}`}
                >
                  {selectedTransaction.riskLevel}
                </span>

                <p>
                  Trinetra analyzed this
                  transaction using multiple
                  behavioral risk factors.
                </p>

              </div>

            </div>

            {/* REASONS */}

            <div className="analysis-section">

              <p className="eyebrow">

                {selectedTransaction.reasons?.length
                  ? "WHY WAS THIS FLAGGED?"
                  : "ANALYSIS RESULT"}

              </p>

              {selectedTransaction.reasons?.length ? (

                selectedTransaction.reasons.map(
                  (reason, index) => (

                    <div
                      className="reason"
                      key={index}
                    >

                      <span className="reason-icon">
                        !
                      </span>

                      <div>

                        <strong>
                          Fraud factor detected
                        </strong>

                        <p>
                          {reason}
                        </p>

                      </div>

                    </div>

                  )
                )

              ) : (

                <div className="reason">

                  <span className="reason-icon">
                    ✓
                  </span>

                  <div>

                    <strong>
                      No suspicious factors detected
                    </strong>

                    <p>
                      This transaction does not
                      indicate significant fraud
                      risk.
                    </p>

                  </div>

                </div>

              )}

            </div>

            {/* DECISION */}

            <div className="analysis-section">

              <p className="eyebrow">
                PROTECTION DECISION
              </p>

              <div className="reason">

                <span className="reason-icon">

                  {selectedTransaction.fraudAction ===
                  "BLOCK"
                    ? "!"
                    : selectedTransaction.fraudAction ===
                        "FLAG"
                      ? "!"
                      : "✓"}

                </span>

                <div>

                  <strong>

                    {selectedTransaction.fraudAction ===
                    "BLOCK"
                      ? "Transaction Blocked"
                      : selectedTransaction.fraudAction ===
                          "FLAG"
                        ? "Transaction Flagged"
                        : "Transaction Approved"}

                  </strong>

                  <p>

                    {selectedTransaction.fraudAction ===
                    "BLOCK"
                      ? "Protected mode blocked this high-risk transaction."
                      : selectedTransaction.fraudAction ===
                          "FLAG"
                        ? "Caution mode flagged this transaction for review."
                        : "The transaction is currently approved."}

                  </p>

                </div>

              </div>

            </div>

            {/* ACTION */}

            <div className="analysis-actions">

              <button
                className="block-btn"
                onClick={() =>
                  handleAction("BLOCK")
                }
              >
                Block Transaction
              </button>

              <button
                className="safe-btn"
                onClick={() =>
                  handleAction("APPROVE")
                }
              >
                Approve Transaction
              </button>

            </div>

          </div>

        </div>

      )}

    </div>
  );
}

export default App;