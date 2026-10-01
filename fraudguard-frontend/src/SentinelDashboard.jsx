import { useEffect, useState } from "react";
import "./Sentinel.css";
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
  },
  SUSPICIOUS: {
    userId: "U1001",
    amount: 8000,
    currency: "INR",
    recipient: "AC8899",
    transactionType: "TRANSFER",
    location: "Delhi",
    deviceId: "DEVICE-NEW",
  },
  HIGH_RISK: {
    userId: "U1001",
    amount: 75000,
    currency: "INR",
    recipient: "AC9871",
    transactionType: "TRANSFER",
    location: "Mumbai",
    deviceId: "DEVICE-NEW",
  },
};

const initialStats = {
  totalTransactions: 0,
  highRiskTransactions: 0,
  averageRiskScore: 0,
  safeTransactions: 0,
  blockedTransactions: 0,
};

function App() {
  const [stats, setStats] = useState(initialStats);
  const [transactions, setTransactions] = useState([]);
  const [transactionData, setTransactionData] = useState({ ...scenarios.HIGH_RISK });
  const [simulationScenario, setSimulationScenario] = useState("HIGH_RISK");
  const [protectionMode, setProtectionMode] = useState("PROTECTED");
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [showAllTransactions, setShowAllTransactions] = useState(false);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [updatingAction, setUpdatingAction] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    loadDashboard();
  }, []);

  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === "Escape" && !updatingAction) setSelectedTransaction(null);
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [updatingAction]);

  async function loadDashboard() {
    try {
      setError("");
      const [statsData, transactionsData] = await Promise.all([
        getDashboardStats(),
        getTransactions(),
      ]);
      setStats(statsData);
      setTransactions(
        [...transactionsData].sort(
          (first, second) => new Date(second.analyzedAt) - new Date(first.analyzedAt),
        ),
      );
    } catch (requestError) {
      console.error("Dashboard loading failed:", requestError);
      setError("Sentinel could not reach the transaction service. Check your connection and retry.");
    } finally {
      setLoading(false);
    }
  }

  function handleScenarioChange(event) {
    const scenario = event.target.value;
    setSimulationScenario(scenario);
    if (scenarios[scenario]) setTransactionData({ ...scenarios[scenario] });
  }

  function handleFieldChange(event) {
    const { name, value } = event.target;
    setTransactionData((current) => ({
      ...current,
      [name]: value,
    }));
    setSimulationScenario("");
  }

  async function handleAnalyze(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;

    setAnalyzing(true);
    setError("");
    const payload = {
      ...transactionData,
      amount: Number(transactionData.amount),
      protectionMode,
    };

    try {
      const result = await analyzeTransaction(payload);
      setSelectedTransaction({
        id: result.transactionId,
        ...payload,
        riskScore: result.riskScore,
        riskLevel: result.riskLevel,
        reasons: result.reasons,
        fraudAction: result.action,
        analyzedAt: new Date().toISOString(),
      });
      await loadDashboard();
    } catch (requestError) {
      console.error("Transaction analysis failed:", requestError);
      setError("Analysis could not be completed. Check the service connection and try again.");
    } finally {
      setAnalyzing(false);
    }
  }

  async function handleAction(action) {
    if (!selectedTransaction?.id || updatingAction) return;
    if (
      action === "BLOCK" &&
      !window.confirm("Block this transaction? This may prevent the payment from proceeding.")
    ) return;

    setUpdatingAction(true);
    setError("");
    try {
      await updateFraudAction(selectedTransaction.id, action);
      await loadDashboard();
      setSelectedTransaction(null);
    } catch (requestError) {
      console.error("Failed to update fraud action:", requestError);
      setError("The fraud action could not be updated. Please retry.");
    } finally {
      setUpdatingAction(false);
    }
  }

  function getRiskClass(level) {
    if (level === "CRITICAL") return "critical";
    if (level === "HIGH") return "high";
    if (level === "SUSPICIOUS") return "suspicious";
    return "safe";
  }

  function getActionClass(action) {
    if (action === "BLOCK") return "blocked";
    if (action === "FLAG") return "flagged";
    return "approved";
  }

  function formatAmount(amount, currency) {
    try {
      return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: currency || "INR",
        maximumFractionDigits: 0,
      }).format(amount || 0);
    } catch {
      return `${currency || "INR"} ${Number(amount || 0).toLocaleString("en-IN")}`;
    }
  }

  function formatDate(value) {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "—";
    return date.toLocaleString([], {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  const modeCopy = {
    NORMAL: "Analyze without automatic intervention.",
    CAUTION: "Flag suspicious activity for review.",
    PROTECTED: "Automatically block high-risk activity.",
  }[protectionMode];

  const visibleTransactions = showAllTransactions ? transactions : transactions.slice(0, 6);

  return (
    <div className="sentinel-app">
      <header className="topbar">
        <a className="brand" href="#overview" aria-label="Sentinel overview">
          <img className="brand-symbol" src="/sentinel-logo.png" alt="" />
          <img className="brand-wordmark" src="/sentinel-wordmark.png" alt="Sentinel" />
        </a>
        <nav className="main-nav" aria-label="Primary navigation">
          <a className="nav-link active" href="#overview" aria-current="page">Overview</a>
          <a className="nav-link" href="#activity">Transactions</a>
          <a className="nav-link" href="#assessment">Risk assessment</a>
        </nav>
        <div className="header-status"><span className="status-pulse" />Protection monitoring</div>
      </header>

      <main className="page" id="overview">
        <section className="page-intro">
          <div>
            <p className="eyebrow">FRAUD INTELLIGENCE / OPERATIONS</p>
            <h1>Security overview</h1>
            <p className="intro-copy">Sentinel evaluates transaction behavior and surfaces risk for timely review.</p>
          </div>
          <div className="monitoring-chip"><span className="status-pulse" />Monitoring active</div>
        </section>

        {error && (
          <div className="notice" role="alert">
            <span className="notice-mark" aria-hidden="true">!</span>
            <div><strong>Service unavailable</strong><p>{error}</p></div>
            <button className="quiet-button" type="button" onClick={loadDashboard}>Retry connection</button>
          </div>
        )}

        <section className="metrics" aria-label="Dashboard statistics">
          <Metric label="Transactions analyzed" value={loading ? null : stats.totalTransactions} note="All assessed activity" tone="neutral" />
          <Metric label="Safe transactions" value={loading ? null : stats.safeTransactions} note="Classified as safe" tone="safe" />
          <Metric label="High-risk transactions" value={loading ? null : stats.highRiskTransactions} note="Requires attention" tone="high" />
          <Metric label="Blocked transactions" value={loading ? null : stats.blockedTransactions} note="Stopped by policy" tone="critical" />
          <Metric label="Average risk score" value={loading ? null : Number(stats.averageRiskScore ?? 0).toFixed(1)} note="Behavioral score · 0–100" tone="score" />
        </section>

        <section className="dashboard-grid">
          <form className="surface assessment" id="assessment" onSubmit={handleAnalyze}>
            <div className="surface-heading assessment-heading">
              <div><p className="eyebrow">ASSESSMENT CONSOLE</p><h2>Analyze a transaction</h2><p>Review payment details against the active protection policy.</p></div>
              <label className="preset-field"><span>Example scenario</span><select value={simulationScenario} onChange={handleScenarioChange}>
                <option value="">Custom transaction</option>
                <option value="SAFE">Normal activity</option>
                <option value="SUSPICIOUS">Suspicious activity</option>
                <option value="HIGH_RISK">High-risk activity</option>
              </select></label>
            </div>

            <FormSection number="01" title="Transaction" description="Payment and destination details">
              <div className="fields-grid">
                <Field label="Amount" name="amount" type="number" min="0.01" step="0.01" required value={transactionData.amount} onChange={handleFieldChange} prefix={transactionData.currency} />
                <Field label="Currency" name="currency" required value={transactionData.currency} onChange={handleFieldChange} />
                <Field label="Recipient" name="recipient" required placeholder="Recipient or account ID" value={transactionData.recipient} onChange={handleFieldChange} />
                <Field label="Transaction type" name="transactionType" required placeholder="e.g. TRANSFER" value={transactionData.transactionType} onChange={handleFieldChange} />
              </div>
            </FormSection>

            <FormSection number="02" title="Context" description="Origin and device signals">
              <div className="fields-grid">
                <Field label="User ID" name="userId" required value={transactionData.userId} onChange={handleFieldChange} />
                <Field label="Location" name="location" required placeholder="City or region" value={transactionData.location} onChange={handleFieldChange} />
                <Field label="Device ID" name="deviceId" required placeholder="Device identifier" value={transactionData.deviceId} onChange={handleFieldChange} />
              </div>
            </FormSection>

            <FormSection number="03" title="Protection mode" description={modeCopy}>
              <div className="mode-selector" role="group" aria-label="Protection mode">
                {[["NORMAL", "Normal"], ["CAUTION", "Caution"], ["PROTECTED", "Protected"]].map(([mode, label]) => (
                  <button key={mode} type="button" className={`mode-option ${protectionMode === mode ? "selected" : ""}`} aria-pressed={protectionMode === mode} onClick={() => setProtectionMode(mode)}>{label}</button>
                ))}
              </div>
            </FormSection>

            <div className="assessment-footer">
              <span>Rule-based behavioral fraud assessment</span>
              <button className="primary-button" type="submit" disabled={analyzing}>
                {analyzing ? <><span className="button-spinner" />Assessing…</> : <>Run assessment <span aria-hidden="true">↗</span></>}
              </button>
            </div>
          </form>

          <aside className="right-column">
            <section className="surface protection-card">
              <div className="surface-heading compact-heading"><div><p className="eyebrow">POLICY STATUS</p><h2>Protection posture</h2></div><span className="posture-icon" aria-hidden="true">●</span></div>
              <div className="protection-state"><span className="status-pulse" /><strong>Active</strong><span>Sentinel monitoring enabled</span></div>
              <div className="policy-line"><span>Active mode</span><strong>{protectionMode}</strong></div>
              <p className="policy-description">{modeCopy}</p>
              <div className="policy-rule"><span className="rule-mark" />Transaction context is evaluated before a decision is returned.</div>
            </section>

            <section className="surface history-card" id="activity">
              <div className="surface-heading compact-heading history-heading"><div><p className="eyebrow">MONITORING FEED</p><h2>{showAllTransactions ? "Transaction history" : "Recent transactions"}</h2></div>
                <button className="quiet-button" type="button" onClick={() => setShowAllTransactions((current) => !current)}>{showAllTransactions ? "Show recent" : "View all"}</button>
              </div>
              {loading ? <div className="history-state"><span className="loading-spinner" />Loading transaction activity</div> : transactions.length === 0 ? (
                <div className="history-state empty-state"><span className="empty-symbol" aria-hidden="true">—</span><strong>No activity to review</strong><p>Transactions appear here after an assessment is completed.</p></div>
              ) : (
                <div className="table-scroll"><table className="transaction-table">
                  <thead><tr><th>Transaction</th><th>User</th><th>Amount</th><th>Risk</th><th>Action</th><th>Analyzed</th></tr></thead>
                  <tbody>{visibleTransactions.map((transaction) => (
                    <tr key={transaction.id} tabIndex="0" onClick={() => setSelectedTransaction(transaction)} onKeyDown={(event) => {
                      if (event.key === "Enter" || event.key === " ") { event.preventDefault(); setSelectedTransaction(transaction); }
                    }} aria-label={`Open transaction ${transaction.id}`}>
                      <td><strong>{transaction.id || "—"}</strong><span>{transaction.recipient || "Unknown recipient"}</span><small>{transaction.transactionType || "Transaction"} · {transaction.location || "Unknown location"}</small></td>
                      <td>{transaction.userId || "—"}</td>
                      <td className="numeric-cell">{formatAmount(transaction.amount, transaction.currency)}</td>
                      <td><div className="risk-cell"><strong>{transaction.riskScore ?? "—"}</strong><RiskBadge level={transaction.riskLevel} riskClass={getRiskClass(transaction.riskLevel)} /></div></td>
                      <td><ActionBadge action={transaction.fraudAction} actionClass={getActionClass(transaction.fraudAction)} /></td>
                      <td className="timestamp-cell">{formatDate(transaction.analyzedAt)}</td>
                    </tr>
                  ))}</tbody>
                </table></div>
              )}
            </section>
          </aside>
        </section>
      </main>

      <footer className="footer"><span>Sentinel / Transaction security</span><span>Risk decisions are returned by the configured service.</span></footer>

      {selectedTransaction && (
        <div className="dialog-backdrop" onMouseDown={(event) => {
          if (event.target === event.currentTarget && !updatingAction) setSelectedTransaction(null);
        }}>
          <section className="transaction-dialog" role="dialog" aria-modal="true" aria-labelledby="dialog-title">
            <div className="dialog-topline"><p className="eyebrow">TRANSACTION REVIEW</p><button className="dialog-close" type="button" aria-label="Close transaction details" onClick={() => setSelectedTransaction(null)}>×</button></div>
            <div className="dialog-title-row"><div><h2 id="dialog-title">{formatAmount(selectedTransaction.amount, selectedTransaction.currency)}</h2><p>{selectedTransaction.recipient || "Unknown recipient"} <span>·</span> {selectedTransaction.transactionType || "Transaction"}</p></div><RiskBadge level={selectedTransaction.riskLevel} riskClass={getRiskClass(selectedTransaction.riskLevel)} /></div>
            <div className={`result-panel ${getRiskClass(selectedTransaction.riskLevel)}`}>
              <div className="result-score"><span className="result-score-label">RISK SCORE</span><strong>{selectedTransaction.riskScore ?? "—"}<small> / 100</small></strong></div>
              <div className="result-level"><span>Assessment level</span><strong>{selectedTransaction.riskLevel || "UNKNOWN"} RISK</strong><div className="score-track"><span style={{ width: `${Math.min(100, Math.max(0, Number(selectedTransaction.riskScore) || 0))}%` }} /></div></div>
            </div>
            <div className="detail-grid">
              <Detail label="Transaction ID" value={selectedTransaction.id} />
              <Detail label="User" value={selectedTransaction.userId} />
              <Detail label="Location" value={selectedTransaction.location} />
              <Detail label="Device" value={selectedTransaction.deviceId} />
              <Detail label="Analyzed" value={formatDate(selectedTransaction.analyzedAt)} />
            </div>
            <section className="signals-section"><p className="eyebrow">ASSESSMENT REASONS</p>
              {selectedTransaction.reasons?.length ? <ul>{selectedTransaction.reasons.map((reason, index) => <li key={`${reason}-${index}`}>{reason}</li>)}</ul> : <p className="no-signals">No additional reasons were returned for this assessment.</p>}
            </section>
            <div className="decision-footer"><div className="current-action"><span>Current action</span><ActionBadge action={selectedTransaction.fraudAction} actionClass={getActionClass(selectedTransaction.fraudAction)} /></div>
              <div className="decision-buttons">
                <button className="action-button approve" type="button" disabled={updatingAction} onClick={() => handleAction("APPROVE")}>{updatingAction ? "Updating…" : "Approve"}</button>
                <button className="action-button flag" type="button" disabled={updatingAction} onClick={() => handleAction("FLAG")}>{updatingAction ? "Updating…" : "Flag"}</button>
                <button className="action-button block" type="button" disabled={updatingAction} onClick={() => handleAction("BLOCK")}>{updatingAction ? "Updating…" : "Block"}</button>
              </div>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

function Metric({ label, value, note, tone }) {
  return <article className={`metric ${tone}`}><div className="metric-heading"><span>{label}</span><i aria-hidden="true" /></div><strong>{value === null ? <span className="metric-loading" /> : value}</strong><small>{note}</small></article>;
}

function FormSection({ number, title, description, children }) {
  return <section className="form-section"><div className="form-section-heading"><span className="form-number">{number}</span><div><h3>{title}</h3><p>{description}</p></div></div>{children}</section>;
}

function Field({ label, name, type = "text", prefix, ...props }) {
  return <label className="field"><span>{label}</span><div className={prefix ? "input-with-prefix" : ""}>{prefix && <span className="input-prefix">{prefix}</span>}<input name={name} type={type} {...props} /></div></label>;
}

function Detail({ label, value }) {
  return <div className="detail-item"><span>{label}</span><strong>{value || "—"}</strong></div>;
}

function RiskBadge({ level, riskClass }) {
  return <span className={`badge risk-badge ${riskClass}`}>{level || "UNKNOWN"}</span>;
}

function ActionBadge({ action, actionClass }) {
  return <span className={`badge action-badge ${actionClass}`}>{action || "—"}</span>;
}

export default App;