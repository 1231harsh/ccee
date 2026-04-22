function ResultPage({ result, onHome, onAnalysis }) {
  const weakTopics = Object.entries(result.wrongPerTopic || {});
  const percentage = result.total
    ? Math.round((result.score / result.total) * 100)
    : 0;

  return (
    <main className="result-layout">
      <section className="panel-card result-hero">
        <p className="eyebrow">{result.subject} Submission Complete</p>
        <h2>Your score is {result.score} / {result.total}</h2>
        <p className="result-percentage">{percentage}% accuracy</p>
        <p className="muted">
          Submitted answers: {result.answeredCount}. Your attempt has been stored
          for future analysis.
        </p>

        <div className="result-actions">
          <button className="button button-secondary" onClick={onHome}>
            Back to Dashboard
          </button>
          <button className="button button-primary" onClick={onAnalysis}>
            View Analysis
          </button>
        </div>
      </section>

      <section className="panel-card">
        <h3>Weak Topics</h3>
        {weakTopics.length ? (
          <div className="insight-list">
            {weakTopics.map(([topic, wrongCount]) => (
              <div className="insight-item" key={topic}>
                <strong>{topic}</strong>
                <span>{wrongCount} incorrect answer(s)</span>
              </div>
            ))}
          </div>
        ) : (
          <p className="muted">Great work. No weak topics were recorded in this attempt.</p>
        )}
      </section>
    </main>
  );
}

export default ResultPage;
