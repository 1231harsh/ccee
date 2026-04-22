import { useEffect, useState } from "react";
import { fetchAnalysis, fetchAnalysisSummary } from "../services/api";

function Analysis({ onBack, onRetake }) {
  const [attempts, setAttempts] = useState([]);
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadAnalysis = async () => {
      setLoading(true);
      setError("");

      try {
        const [attemptData, summaryData] = await Promise.all([
          fetchAnalysis(),
          fetchAnalysisSummary(),
        ]);

        setAttempts(attemptData);
        setSummary(summaryData);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    loadAnalysis();
  }, []);

  return (
    <main className="analysis-layout">
      <section className="analysis-header">
        <div>
          <p className="eyebrow">Performance Analysis</p>
          <h2>Review your past attempts and focus on weak topics.</h2>
        </div>
        <button className="button button-secondary" onClick={onBack}>
          Back to Dashboard
        </button>
      </section>

      {error ? <div className="banner banner-error">{error}</div> : null}

      {loading ? (
        <section className="panel-card empty-state">
          <h3>Loading analysis...</h3>
        </section>
      ) : (
        <>
          <section className="summary-grid">
            <article className="panel-card summary-card">
              <span>Attempts</span>
              <strong>{summary?.attempts ?? attempts.length}</strong>
            </article>
            <article className="panel-card summary-card">
              <span>Average Score</span>
              <strong>{summary?.averageScore?.toFixed?.(2) ?? "0.00"}</strong>
            </article>
            <article className="panel-card summary-card">
              <span>Subjects Attempted</span>
              <strong>{new Set(attempts.map((attempt) => attempt.subject)).size}</strong>
            </article>
          </section>

          <section className="analysis-grid">
            <article className="panel-card">
              <h3>Weak Topic Summary</h3>
              {summary && Object.keys(summary.weakTopics || {}).length ? (
                <div className="insight-list">
                  {Object.entries(summary.weakTopics).map(([topic, count]) => (
                    <div className="insight-item" key={topic}>
                      <strong>{topic}</strong>
                      <span>{count} total mistakes</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="muted">No weak topics recorded yet. Take a test to build your report.</p>
              )}
            </article>

            <article className="panel-card">
              <h3>Attempt History</h3>
              {attempts.length ? (
                <div className="attempt-list">
                  {attempts
                    .slice()
                    .reverse()
                    .map((attempt) => (
                      <div className="attempt-card" key={attempt.id}>
                        <div>
                          <p className="attempt-subject">{attempt.subject}</p>
                          <strong>
                            {attempt.score} / {attempt.total}
                          </strong>
                        </div>
                        <div className="attempt-meta">
                          <span>{formatDate(attempt.timestamp)}</span>
                          <button
                            className="button-link"
                            onClick={() => onRetake(attempt.subject)}
                            type="button"
                          >
                            Retake
                          </button>
                        </div>
                      </div>
                    ))}
                </div>
              ) : (
                <p className="muted">No attempts found yet.</p>
              )}
            </article>
          </section>
        </>
      )}
    </main>
  );
}

function formatDate(value) {
  if (!value) return "Unknown time";

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}

export default Analysis;
