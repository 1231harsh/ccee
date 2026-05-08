const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const getToken = () => localStorage.getItem("token");

async function request(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...options,
      headers,
    });
  } catch {
    throw new Error("Could not connect to the server. Please check your network or try again in a moment.");
  }

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    throw new Error(
      typeof payload === "string"
        ? payload || "Request failed"
        : payload?.message || "Request failed"
    );
  }

  return payload;
}

export const fetchTest = (subject) =>
  request(`/api/exam/start-test/${subject}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });

export const submitTest = (answers, sessionId) =>
  request("/api/exam/submit-test", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
    body: JSON.stringify({ answers, sessionId }),
  });

export const fetchAnalysis = () =>
  request("/api/exam/analysis", {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });

export const fetchAnalysisSummary = () =>
  request("/api/exam/analysis-summary", {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
