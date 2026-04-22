import { useState } from "react";
import { login, register } from "../services/auth";

function Login({ onAuthSuccess }) {
  const [mode, setMode] = useState("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const isRegister = mode === "register";

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");

    try {
      if (isRegister) {
        await register(username, password);
        setMessage("Registration successful. Please sign in.");
        setMode("login");
      } else {
        const data = await login(username, password);
        onAuthSuccess({ token: data.token, username });
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout">
      <section className="auth-hero">
        <p className="eyebrow">CDAC Prep Suite</p>
        <h1>Prepare with secure subject-wise mock tests and performance analysis.</h1>
        <p className="hero-copy">
          Practice JAVA, DBMS, CPP, and WEB in the same flow your backend
          supports. Review weak topics, track attempts, and stay exam-ready.
        </p>
        <div className="hero-badges">
          <span>Live scoring</span>
          <span>Topic analysis</span>
          <span>JWT-secured sessions</span>
        </div>
      </section>

      <section className="auth-panel">
        <div className="panel-card auth-card">
          <div className="auth-switch">
            <button
              className={mode === "login" ? "is-active" : ""}
              onClick={() => setMode("login")}
              type="button"
            >
              Login
            </button>
            <button
              className={mode === "register" ? "is-active" : ""}
              onClick={() => setMode("register")}
              type="button"
            >
              Register
            </button>
          </div>

          <h2>{isRegister ? "Create your account" : "Welcome back"}</h2>
          <p className="muted">
            {isRegister
              ? "Register once, then sign in to start personalized tests."
              : "Sign in to launch a test and unlock your attempt history."}
          </p>

          <form className="stack" onSubmit={handleSubmit}>
            <label className="field">
              <span>Username</span>
              <input
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                placeholder="Enter username"
                required
              />
            </label>

            <label className="field">
              <span>Password</span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter password"
                minLength={4}
                required
              />
            </label>

            {message ? <div className="banner banner-success">{message}</div> : null}
            {error ? <div className="banner banner-error">{error}</div> : null}

            <button className="button button-primary" disabled={loading} type="submit">
              {loading
                ? "Please wait..."
                : isRegister
                  ? "Create account"
                  : "Login"}
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}

export default Login;
