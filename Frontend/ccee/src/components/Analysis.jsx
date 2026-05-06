import { useEffect, useMemo, useState } from "react";
import { fetchAnalysis, fetchAnalysisSummary } from "../services/api";

const SUBJECTS = [
  { code: "ALL", title: "All Subjects" },
  { code: "JAVA", title: "Java" },
  { code: "DBMS", title: "DBMS" },
  { code: "CPP", title: "C++" },
  { code: "WEB", title: "WPT" },
  { code: "DSA", title: "DSA" },
];

function Analysis({ onBack, onRetake }) {
  const [attempts, setAttempts] = useState([]);
  const [summary, setSummary] = useState(null);
  const [selectedSubject, setSelectedSubject] = useState("ALL");
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

  const filteredAttempts = useMemo(() => {
    const base =
      selectedSubject === "ALL"
        ? attempts
        : attempts.filter((attempt) => attempt.subject === selectedSubject);

    return base.slice().sort(sortAttemptsDescending);
  }, [attempts, selectedSubject]);

  const selectedSubjectSummary = useMemo(() => {
    if (selectedSubject === "ALL") {
      return null;
    }

    return summary?.subjectSummaries?.[selectedSubject] ?? null;
  }, [selectedSubject, summary]);

  const selectedWeakTopics = useMemo(() => {
    if (selectedSubject === "ALL") {
      return summary?.weakTopics ?? {};
    }

    return selectedSubjectSummary?.weakTopics ?? {};
  }, [selectedSubject, selectedSubjectSummary, summary]);

  const growthTimeline = useMemo(() => {
    if (selectedSubject === "ALL") {
      return [];
    }

    return attempts
      .filter((attempt) => attempt.subject === selectedSubject)
      .slice()
      .sort(sortAttemptsAscending)
      .map((attempt, index, subjectAttempts) => {
        const previous = index > 0 ? subjectAttempts[index - 1] : null;
        const growth = previous ? attempt.score - previous.score : 0;

        return {
          id: attempt.id,
          label: `Attempt ${index + 1}`,
          score: attempt.score,
          total: attempt.total,
          growth,
          timestamp: attempt.timestamp,
        };
      });
  }, [attempts, selectedSubject]);

  const subjectAttemptCount = selectedSubject === "ALL"
    ? attempts.length
    : filteredAttempts.length;

  return (
    <main className="analysis-layout">
      <section className="analysis-header">
        <div>
          <p className="eyebrow">Performance Analysis</p>
          <h2>Every attempt is stored, grouped by subject, and tracked over time.</h2>
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
              <span>Total Attempts</span>
              <strong>{summary?.attempts ?? attempts.length}</strong>
            </article>
            <article className="panel-card summary-card">
              <span>Average Score</span>
              <strong>{summary?.averageScore?.toFixed?.(2) ?? "0.00"}</strong>
            </article>
            <article className="panel-card summary-card">
              <span>Latest CCEE Composite</span>
              <strong>
                {summary?.latestComposite?.score ?? 0} / {summary?.maxCceeScore ?? 225}
              </strong>
              <small className="muted">
                Latest attempt from each of the 5 subjects
              </small>
            </article>
            <article className="panel-card summary-card">
              <span>Subjects Completed</span>
              <strong>
                {summary?.latestComposite?.completedSubjects ?? 0} / 5
              </strong>
            </article>
          </section>

          <section className="panel-card">
            <div className="subject-filter-bar">
              {SUBJECTS.map((subject) => (
                <button
                  key={subject.code}
                  className={`subject-filter ${selectedSubject === subject.code ? "is-active" : ""}`}
                  onClick={() => setSelectedSubject(subject.code)}
                  type="button"
                >
                  {subject.title}
                </button>
              ))}
            </div>
          </section>

          {selectedSubject === "ALL" ? (
            <section className="analysis-grid">
              <article className="panel-card">
                <h3>Latest Subject Scores</h3>
                <div className="insight-list">
                  {SUBJECTS.filter((subject) => subject.code !== "ALL").map((subject) => {
                    const latest = summary?.latestComposite?.latestPerSubject?.[subject.code];
                    return (
                      <div className="insight-item" key={subject.code}>
                        <div>
                          <strong>{subject.title}</strong>
                          <p className="muted insight-meta">
                            {latest ? formatDate(latest.timestamp) : "No attempt yet"}
                          </p>
                        </div>
                        <span>
                          {latest ? `${latest.score} / ${latest.total}` : "Pending"}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </article>

              <article className="panel-card">
                <h3>Overall Weak Topic Summary</h3>
                {Object.keys(selectedWeakTopics).length ? (
                  <div className="insight-list">
                    {Object.entries(selectedWeakTopics)
                      .sort((left, right) => right[1] - left[1])
                      .map(([topic, count]) => (
                        <div className="insight-item" key={topic}>
                          <strong>{topic}</strong>
                          <span>{count} total mistakes</span>
                        </div>
                      ))}
                  </div>
                ) : (
                  <p className="muted">No weak topics recorded yet.</p>
                )}
              </article>
            </section>
          ) : (
            <section className="summary-grid">
              <article className="panel-card summary-card">
                <span>{selectedSubject} Attempts</span>
                <strong>{selectedSubjectSummary?.attempts ?? 0}</strong>
              </article>
              <article className="panel-card summary-card">
                <span>Latest Score</span>
                <strong>
                  {selectedSubjectSummary
                    ? `${selectedSubjectSummary.latestScore} / ${selectedSubjectSummary.latestTotal}`
                    : "0 / 45"}
                </strong>
              </article>
              <article className="panel-card summary-card">
                <span>Best Score</span>
                <strong>{selectedSubjectSummary?.bestScore ?? 0}</strong>
              </article>
              <article className="panel-card summary-card">
                <span>Growth vs Previous</span>
                <strong>{formatGrowth(selectedSubjectSummary?.growth ?? 0)}</strong>
              </article>
            </section>
          )}

          {selectedSubject !== "ALL" ? (
            <section className="analysis-grid">
              <article className="panel-card">
                <h3>{selectedSubject} Growth Timeline</h3>
                {growthTimeline.length ? (
                  <div className="insight-list">
                    {growthTimeline.map((entry) => (
                      <div className="insight-item" key={entry.id}>
                        <div>
                          <strong>{entry.label}</strong>
                          <p className="muted insight-meta">{formatDate(entry.timestamp)}</p>
                        </div>
                        <span>
                          {entry.score} / {entry.total} ({formatGrowth(entry.growth)})
                        </span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="muted">No attempts found for this subject yet.</p>
                )}
              </article>

              <article className="panel-card">
                <h3>{selectedSubject} Weak Topics</h3>
                {Object.keys(selectedWeakTopics).length ? (
                  <div className="insight-list">
                    {Object.entries(selectedWeakTopics)
                      .sort((left, right) => right[1] - left[1])
                      .map(([topic, count]) => (
                        <div className="insight-item" key={topic}>
                          <strong>{topic}</strong>
                          <span>{count} total mistakes</span>
                        </div>
                      ))}
                  </div>
                ) : (
                  <p className="muted">No weak topics recorded for this subject.</p>
                )}
              </article>
            </section>
          ) : null}

          <section className="panel-card">
            <h3>
              {selectedSubject === "ALL"
                ? "Complete Attempt History"
                : `${selectedSubject} Attempt History`}
            </h3>
            <p className="muted">
              Showing {subjectAttemptCount} stored attempt{subjectAttemptCount === 1 ? "" : "s"}.
            </p>

            {filteredAttempts.length ? (
              <div className="attempt-list">
                {filteredAttempts.map((attempt) => (
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
                    <div className="attempt-review-list">
                      {(attempt.questionReviews || []).map((review, index) => (
                        <div
                          className="attempt-review-item"
                          key={`${attempt.id}-${review.questionId}-${index}`}
                        >
                          <div>
                            <p className="review-topic">{review.topic}</p>
                            <strong>
                              {index + 1}. {review.questionText}
                            </strong>
                          </div>
                          <div className="review-answer-grid">
                            <div className={review.correct ? "review-answer is-correct" : "review-answer is-wrong"}>
                              <span>Your response</span>
                              <strong>{review.selectedAnswer || "Not answered"}</strong>
                            </div>
                            <div className="review-answer is-correct">
                              <span>Correct response</span>
                              <strong>{review.correctAnswer}</strong>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="muted">No attempts found for this selection yet.</p>
            )}
          </section>
        </>
      )}
    </main>
  );
}

function sortAttemptsAscending(left, right) {
  return new Date(left.timestamp).getTime() - new Date(right.timestamp).getTime();
}

function sortAttemptsDescending(left, right) {
  return new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime();
}

function formatDate(value) {
  if (!value) return "Unknown time";

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}

function formatGrowth(value) {
  if (value > 0) {
    return `+${value}`;
  }

  return String(value);
}

export default Analysis;
