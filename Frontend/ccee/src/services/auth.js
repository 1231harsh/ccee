const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

async function authRequest(path, body) {
  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
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
        ? payload || "Authentication failed"
        : payload?.message || "Authentication failed"
    );
  }

  return payload;
}

export const login = (username, password) =>
  authRequest("/api/auth/login", { username, password });

export const register = (username, password) =>
  authRequest("/api/auth/register", { username, password });
