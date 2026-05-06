import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { submitTest } from "../services/api";

const TEST_DURATION_SECONDS = 60 * 60;

function TestPage({ session, onCancel, onComplete }) {
  const { subject, questions } = session;
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState({});
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [timeLeft, setTimeLeft] = useState(TEST_DURATION_SECONDS);
  const autoSubmittedRef = useRef(false);

  const currentQuestion = questions[currentIndex];
  const answeredCount = Object.keys(answers).length;

  const progressPercent = useMemo(() => {
    if (!questions.length) return 0;
    return Math.round((answeredCount / questions.length) * 100);
  }, [answeredCount, questions.length]);

  const formattedTime = useMemo(() => {
    const minutes = Math.floor(timeLeft / 60);
    const seconds = timeLeft % 60;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  }, [timeLeft]);

  const currentAnswer = answers[currentQuestion?.id];

  const handleSubmit = useCallback(async (reason = "manual") => {
    setSubmitting(true);
    setError("");

    try {
      const result = await submitTest(answers, subject);
      onComplete({
        ...result,
        submittedAnswers: answers,
        submitReason: reason,
      });
    } catch (err) {
      setError(err.message);
      autoSubmittedRef.current = false;
    } finally {
      setSubmitting(false);
    }
  }, [answers, onComplete, subject]);

  useEffect(() => {
    if (submitting) {
      return undefined;
    }

    const timerId = window.setInterval(() => {
      setTimeLeft((previous) => {
        if (previous <= 1) {
          window.clearInterval(timerId);
          return 0;
        }

        return previous - 1;
      });
    }, 1000);

    return () => window.clearInterval(timerId);
  }, [submitting]);

  useEffect(() => {
    if (timeLeft !== 0 || autoSubmittedRef.current || submitting) {
      return;
    }

    autoSubmittedRef.current = true;
    handleSubmit("timer");
  }, [handleSubmit, submitting, timeLeft]);

  if (!questions.length) {
    return (
      <section className="panel-card empty-state">
        <h2>No questions available</h2>
        <p>The backend did not return any questions for this subject.</p>
        <button className="button button-secondary" onClick={onCancel}>
          Back to dashboard
        </button>
      </section>
    );
  }

  return (
    <main className="test-layout">
      <aside className="panel-card test-sidebar">
        <p className="eyebrow">{subject} Test</p>
        <h2>Question Navigator</h2>
        <p className="muted">
          Answered {answeredCount} of {questions.length}
        </p>
        <div className={`timer-badge ${timeLeft <= 300 ? "is-warning" : ""}`}>
          <span>Time Left</span>
          <strong>{formattedTime}</strong>
        </div>

        <div className="progress-track">
          <div className="progress-fill" style={{ width: `${progressPercent}%` }} />
        </div>

        <div className="question-grid">
          {questions.map((question, index) => (
            <button
              key={question.id}
              className={`question-chip ${
                index === currentIndex ? "is-current" : ""
              } ${answers[question.id] !== undefined ? "is-answered" : ""}`}
              onClick={() => setCurrentIndex(index)}
              type="button"
            >
              {index + 1}
            </button>
          ))}
        </div>

        <div className="sidebar-actions">
          <button className="button button-secondary" onClick={onCancel} type="button">
            Exit Test
          </button>
          <button
            className="button button-primary"
            disabled={submitting}
            onClick={() => handleSubmit("manual")}
            type="button"
          >
            {submitting ? "Submitting..." : "Submit Test"}
          </button>
        </div>
      </aside>

      <section className="panel-card test-panel">
        <div className="question-header">
          <div>
            <p className="eyebrow">
              Question {currentIndex + 1} of {questions.length}
            </p>
          </div>
          <div className="question-header-actions">
            <span className="subject-pill">{currentQuestion.subject}</span>
            <button className="button button-secondary" onClick={onCancel} type="button">
              Exit Test
            </button>
          </div>
        </div>

        <p className="question-text">{currentQuestion.question}</p>

        <div className="question-tools">
          <p className="muted answer-status">
            {currentAnswer === undefined
              ? "No option selected for this question yet."
              : `Selected option ${String.fromCharCode(65 + currentAnswer)}.`}
          </p>
          <button
            className="button button-secondary"
            disabled={currentAnswer === undefined}
            onClick={() =>
              setAnswers((prev) => {
                const next = { ...prev };
                delete next[currentQuestion.id];
                return next;
              })
            }
            type="button"
          >
            Clear Selection
          </button>
        </div>

        <div className="options-list">
          {currentQuestion.options.map((option, optionIndex) => {
            const isSelected = answers[currentQuestion.id] === optionIndex;
            return (
              <button
                key={`${currentQuestion.id}-${optionIndex}`}
                className={`option-card ${isSelected ? "is-selected" : ""}`}
                onClick={() =>
                  setAnswers((prev) => ({ ...prev, [currentQuestion.id]: optionIndex }))
                }
                type="button"
              >
                <span className="option-badge">{String.fromCharCode(65 + optionIndex)}</span>
                <span>{option}</span>
              </button>
            );
          })}
        </div>

        {timeLeft === 0 ? (
          <div className="banner banner-error">Time is up. Submitting your test...</div>
        ) : null}
        {error ? <div className="banner banner-error">{error}</div> : null}

        <div className="test-nav">
          {currentIndex > 0 ? (
            <button
              className="button button-secondary"
              onClick={() => setCurrentIndex((prev) => prev - 1)}
              type="button"
            >
              Previous
            </button>
          ) : null}
          {currentIndex < questions.length - 1 ? (
            <button
              className="button button-primary"
              onClick={() => setCurrentIndex((prev) => prev + 1)}
              type="button"
            >
              Next
            </button>
          ) : null}
        </div>
      </section>
    </main>
  );
}

export default TestPage;
