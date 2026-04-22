import { useState } from "react";
import "./App.css";
import Analysis from "./components/Analysis";
import Home from "./components/Home";
import Login from "./components/Login";
import ResultPage from "./components/ResultPage";
import TestPage from "./components/TestPage";
import { fetchTest } from "./services/api";

function App() {
  const [auth, setAuth] = useState(() => ({
    token: localStorage.getItem("token"),
    username: localStorage.getItem("username") || "",
  }));
  const [view, setView] = useState("home");
  const [testSession, setTestSession] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const isLoggedIn = Boolean(auth.token);

  const handleAuthSuccess = ({ token, username }) => {
    localStorage.setItem("token", token);
    localStorage.setItem("username", username);
    setAuth({ token, username });
    setView("home");
    setError("");
  };

  const handleStartTest = async (subject) => {
    setLoading(true);
    setError("");

    try {
      const questions = await fetchTest(subject);
      setTestSession({ subject, questions, startedAt: Date.now() });
      setView("test");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    setAuth({ token: "", username: "" });
    setView("home");
    setResult(null);
    setTestSession(null);
    setError("");
  };

  const handleFinishTest = (submissionResult) => {
    setResult({
      ...submissionResult,
      subject: testSession?.subject,
      answeredCount: Object.keys(submissionResult?.submittedAnswers ?? {}).length,
    });
    setTestSession(null);
    setView("result");
  };

  if (!isLoggedIn) {
    return <Login onAuthSuccess={handleAuthSuccess} />;
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Computerized Common Entrance Examination</p>
          <h1>CCEE Smart Practice Portal</h1>
        </div>
        <div className="topbar-actions">
          <div className="user-chip">
            <span className="user-label">Signed in as</span>
            <strong>{auth.username || "Candidate"}</strong>
          </div>
          <button className="button button-secondary" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      {error ? <div className="banner banner-error">{error}</div> : null}

      {view === "home" ? (
        <Home
          loading={loading}
          onStartTest={handleStartTest}
          onViewAnalysis={() => setView("analysis")}
        />
      ) : null}

      {view === "test" && testSession ? (
        <TestPage
          key={`${testSession.subject}-${testSession.startedAt}`}
          session={testSession}
          onCancel={() => {
            setTestSession(null);
            setView("home");
          }}
          onComplete={handleFinishTest}
        />
      ) : null}

      {view === "result" && result ? (
        <ResultPage
          result={result}
          onHome={() => setView("home")}
          onAnalysis={() => setView("analysis")}
        />
      ) : null}

      {view === "analysis" ? (
        <Analysis
          onBack={() => setView("home")}
          onRetake={handleStartTest}
        />
      ) : null}
    </div>
  );
}

export default App;
