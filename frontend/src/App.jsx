import React, { useEffect, useMemo, useState } from "react";

const DEFAULT_AUTH_SERVER = "http://localhost:9000";
const DEFAULT_SERVICE_A = "http://localhost:9000";
const DEFAULT_SERVICE_B = "http://localhost:9001";
const DEFAULT_CLIENT_ID = "pkce-client";
const DEFAULT_SCOPE = "openid service.a.read service.b.read";
const DEFAULT_USERNAME = "demo";
const DEFAULT_PASSWORD = "1234";
const TOKEN_STORAGE_KEY = "sso_pkce_token";
const CODE_VERIFIER_KEY = "sso_pkce_code_verifier";
const OAUTH_STATE_KEY = "sso_pkce_state";

function randomString(length = 96) {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
  const randomBytes = new Uint8Array(length);
  window.crypto.getRandomValues(randomBytes);
  return Array.from(randomBytes, (value) => chars[value % chars.length]).join("");
}

function base64UrlEncode(buffer) {
  const byteArray = new Uint8Array(buffer);
  let text = "";
  for (const value of byteArray) {
    text += String.fromCharCode(value);
  }
  return btoa(text).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

async function createCodeChallenge(codeVerifier) {
  const encoded = new TextEncoder().encode(codeVerifier);
  const digest = await window.crypto.subtle.digest("SHA-256", encoded);
  return base64UrlEncode(digest);
}

function parseJsonSafe(rawText) {
  try {
    return JSON.parse(rawText);
  } catch (_error) {
    return rawText;
  }
}

function decodeJwtPayload(accessToken) {
  if (!accessToken) {
    return null;
  }
  const parts = accessToken.split(".");
  if (parts.length !== 3) {
    return null;
  }
  try {
    const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const decoded = atob(payload + "===".slice((payload.length + 3) % 4));
    return JSON.parse(decoded);
  } catch (_error) {
    return null;
  }
}

function normalizeTokenResponse(token) {
  if (!token || typeof token !== "object") {
    return null;
  }

  if (token.access_token) {
    return token;
  }

  if (token.accessToken) {
    return {
      access_token: token.accessToken,
      token_type: token.tokenType ?? "Bearer",
      expires_in: token.expiresIn ?? token.expires_in ?? 0,
      scope: token.scope ?? "",
      audience: token.audience ?? []
    };
  }

  return null;
}

function extractErrorMessage(parsed) {
  if (!parsed) {
    return "Request failed.";
  }
  if (typeof parsed === "string") {
    return parsed;
  }
  if (parsed.message) {
    return parsed.message;
  }
  if (parsed.error_description) {
    return parsed.error_description;
  }
  if (parsed.error) {
    return typeof parsed.error === "string" ? parsed.error : JSON.stringify(parsed.error);
  }
  return JSON.stringify(parsed);
}

export default function App() {
  const [authServerUrl, setAuthServerUrl] = useState(DEFAULT_AUTH_SERVER);
  const [serviceAUrl, setServiceAUrl] = useState(DEFAULT_SERVICE_A);
  const [serviceBUrl, setServiceBUrl] = useState(DEFAULT_SERVICE_B);
  const [clientId, setClientId] = useState(DEFAULT_CLIENT_ID);
  const [scope, setScope] = useState(DEFAULT_SCOPE);
  const [redirectUri, setRedirectUri] = useState(`${window.location.origin}/callback`);
  const [tokenResponse, setTokenResponse] = useState(null);
  const [serviceAResponse, setServiceAResponse] = useState(null);
  const [serviceBResponse, setServiceBResponse] = useState(null);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [isCallbackProcessing, setIsCallbackProcessing] = useState(false);
  const [isDirectLoginLoading, setIsDirectLoginLoading] = useState(false);
  const [username, setUsername] = useState(DEFAULT_USERNAME);
  const [password, setPassword] = useState(DEFAULT_PASSWORD);

  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (storedToken) {
      const parsed = parseJsonSafe(storedToken);
      setTokenResponse(normalizeTokenResponse(parsed) ?? parsed);
    }
  }, []);

  useEffect(() => {
    const processCallback = async () => {
      const currentPath = window.location.pathname;
      const params = new URLSearchParams(window.location.search);
      const oauthError = params.get("error");
      const code = params.get("code");
      const returnedState = params.get("state");

      if (currentPath !== "/callback") {
        return;
      }

      if (oauthError) {
        const errorDescription = params.get("error_description") || oauthError;
        setError(`OAuth error: ${errorDescription}`);
        window.history.replaceState({}, "", "/");
        return;
      }

      if (!code) {
        setError("Authorization code missing in callback URL.");
        window.history.replaceState({}, "", "/");
        return;
      }

      setIsCallbackProcessing(true);
      setError("");
      setStatus("Exchanging authorization code for access token...");

      try {
        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);
        const savedVerifier = sessionStorage.getItem(CODE_VERIFIER_KEY);
        if (!savedState || !savedVerifier) {
          throw new Error("Missing PKCE verifier/state in browser session.");
        }
        if (savedState !== returnedState) {
          throw new Error("OAuth state mismatch.");
        }

        const body = new URLSearchParams({
          grant_type: "authorization_code",
          client_id: clientId,
          code,
          redirect_uri: redirectUri,
          code_verifier: savedVerifier
        });

        const response = await fetch(`${authServerUrl}/oauth2/token`, {
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded"
          },
          body
        });

        const raw = await response.text();
        const parsed = parseJsonSafe(raw);
        if (!response.ok) {
          throw new Error(extractErrorMessage(parsed));
        }

        const normalizedToken = normalizeTokenResponse(parsed);
        if (!normalizedToken) {
          throw new Error("Token response is missing access token.");
        }

        setTokenResponse(normalizedToken);
        localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(normalizedToken));
        setStatus("Token received successfully.");
      } catch (callbackError) {
        setError(callbackError.message || "Failed to exchange code.");
      } finally {
        sessionStorage.removeItem(CODE_VERIFIER_KEY);
        sessionStorage.removeItem(OAUTH_STATE_KEY);
        setIsCallbackProcessing(false);
        window.history.replaceState({}, "", "/");
      }
    };

    processCallback();
  }, [authServerUrl, clientId, redirectUri]);

  const accessToken = tokenResponse?.access_token ?? tokenResponse?.accessToken ?? "";
  const tokenPayload = useMemo(() => decodeJwtPayload(accessToken), [accessToken]);

  const loginWithServiceAApi = async () => {
    setError("");
    setStatus("");

    const normalizedUsername = username.trim();
    const normalizedPassword = password.trim();
    if (!normalizedUsername || !normalizedPassword) {
      setError("Username and password are required.");
      return;
    }

    setIsDirectLoginLoading(true);
    try {
      const response = await fetch(`${authServerUrl}/api/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          username: normalizedUsername,
          password: normalizedPassword
        })
      });

      const raw = await response.text();
      const parsed = parseJsonSafe(raw);
      if (!response.ok) {
        throw new Error(extractErrorMessage(parsed));
      }

      const loginPayload = parsed?.data ?? parsed;
      const normalizedToken = normalizeTokenResponse(loginPayload);
      if (!normalizedToken) {
        throw new Error("Login API did not return an access token.");
      }

      setTokenResponse(normalizedToken);
      localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(normalizedToken));
      setStatus("Direct login successful. Access token stored.");
    } catch (loginError) {
      setError(loginError.message || "Direct login failed.");
    } finally {
      setIsDirectLoginLoading(false);
    }
  };

  const startPkceLogin = async () => {
    setError("");
    setStatus("");
    const codeVerifier = randomString(96);
    const codeChallenge = await createCodeChallenge(codeVerifier);
    const state = randomString(32);

    sessionStorage.setItem(CODE_VERIFIER_KEY, codeVerifier);
    sessionStorage.setItem(OAUTH_STATE_KEY, state);

    const url = new URL(`${authServerUrl}/oauth2/authorize`);
    url.searchParams.set("response_type", "code");
    url.searchParams.set("client_id", clientId);
    url.searchParams.set("scope", scope);
    url.searchParams.set("redirect_uri", redirectUri);
    url.searchParams.set("code_challenge", codeChallenge);
    url.searchParams.set("code_challenge_method", "S256");
    url.searchParams.set("state", state);

    window.location.assign(url.toString());
  };

  const callSecureApi = async (target, setTarget) => {
    setError("");
    setStatus("");

    if (!accessToken) {
      setError("No access token found. Login first (Direct login or PKCE).");
      return;
    }

    try {
      const response = await fetch(target, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${accessToken}`
        }
      });
      const raw = await response.text();
      const parsed = parseJsonSafe(raw);
      setTarget(parsed);
      if (!response.ok) {
        setStatus(`Request failed with HTTP ${response.status}.`);
      } else {
        setStatus("Request succeeded.");
      }
    } catch (requestError) {
      setError(requestError.message || "Request failed.");
    }
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    sessionStorage.removeItem(CODE_VERIFIER_KEY);
    sessionStorage.removeItem(OAUTH_STATE_KEY);
    setTokenResponse(null);
    setServiceAResponse(null);
    setServiceBResponse(null);
    setStatus("Local token/session cleared.");
    setError("");
  };

  return (
    <div className="page">
      <main className="panel">
        <h1>OAuth2 PKCE SPA Demo</h1>
        <p className="subtitle">React app using service-a authorization server and custom login API.</p>

        <section className="section">
          <h2>Direct Login (service-a /api/auth/login)</h2>
          <div className="grid">
            <label>
              Username
              <input value={username} onChange={(event) => setUsername(event.target.value)} />
            </label>
            <label>
              Password
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>
          </div>
          <div className="actions">
            <button onClick={loginWithServiceAApi} disabled={isDirectLoginLoading || isCallbackProcessing}>
              {isDirectLoginLoading ? "Logging in..." : "Login via service-a API"}
            </button>
          </div>
        </section>

        <section className="section">
          <h2>OAuth Client Settings</h2>
          <div className="grid">
            <label>
              Auth Server
              <input value={authServerUrl} onChange={(event) => setAuthServerUrl(event.target.value)} />
            </label>
            <label>
              Client ID
              <input value={clientId} onChange={(event) => setClientId(event.target.value)} />
            </label>
            <label>
              Redirect URI
              <input value={redirectUri} onChange={(event) => setRedirectUri(event.target.value)} />
            </label>
            <label>
              Scopes
              <input value={scope} onChange={(event) => setScope(event.target.value)} />
            </label>
          </div>
          <div className="actions">
            <button onClick={startPkceLogin} disabled={isCallbackProcessing}>
              Login with PKCE
            </button>
            <button className="secondary" onClick={logout}>
              Clear Local Session
            </button>
          </div>
        </section>

        <section className="section">
          <h2>API Targets</h2>
          <div className="grid">
            <label>
              Service A Base URL
              <input value={serviceAUrl} onChange={(event) => setServiceAUrl(event.target.value)} />
            </label>
            <label>
              Service B Base URL
              <input value={serviceBUrl} onChange={(event) => setServiceBUrl(event.target.value)} />
            </label>
          </div>
          <div className="actions">
            <button onClick={() => callSecureApi(`${serviceAUrl}/api/a/secure`, setServiceAResponse)}>
              Call Service A Secure API
            </button>
            <button onClick={() => callSecureApi(`${serviceBUrl}/api/b/secure`, setServiceBResponse)}>
              Call Service B Secure API
            </button>
          </div>
        </section>

        {status ? <p className="status">{status}</p> : null}
        {error ? <p className="error">{error}</p> : null}

        <section className="section">
          <h2>Token Response</h2>
          <pre>{tokenResponse ? JSON.stringify(tokenResponse, null, 2) : "No token yet"}</pre>
        </section>

        <section className="section">
          <h2>Decoded Access Token Payload</h2>
          <pre>{tokenPayload ? JSON.stringify(tokenPayload, null, 2) : "No access token payload yet"}</pre>
        </section>

        <section className="section">
          <h2>Service A Response</h2>
          <pre>{serviceAResponse ? JSON.stringify(serviceAResponse, null, 2) : "No response yet"}</pre>
        </section>

        <section className="section">
          <h2>Service B Response</h2>
          <pre>{serviceBResponse ? JSON.stringify(serviceBResponse, null, 2) : "No response yet"}</pre>
        </section>
      </main>
    </div>
  );
}
