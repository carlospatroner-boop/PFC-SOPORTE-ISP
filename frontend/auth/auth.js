// auth.js — Login/registro para auth-service (equipo ACC, Soporte Técnico ISP)
// Sin build step, igual que frontend/app.js: JS vanilla + fetch.
//
// Los tokens se guardan en sessionStorage (no localStorage): se pierden al cerrar
// la pestaña, lo cual es un tradeoff razonable para esta demo académica frente al
// riesgo de persistir tokens de sesión indefinidamente en el navegador.

const API_BASE = "http://localhost:8001/api/v1/auth";

const state = {
  accessToken: null,
  refreshToken: null,
  accessTokenExpiresAt: null,
  countdownTimer: null,
};

// ---------- Helpers ----------

function showToast(message, type = "success") {
  const container = document.getElementById("toast-container");
  const el = document.createElement("div");
  el.className = `toast ${type}`;
  el.textContent = message;
  container.appendChild(el);
  setTimeout(() => el.remove(), 4200);
}

function showErrorBanner(message) {
  const banner = document.getElementById("error-banner");
  banner.textContent = message;
  banner.classList.remove("hidden");
}
function hideErrorBanner() {
  document.getElementById("error-banner").classList.add("hidden");
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

function decodeJwtPayload(token) {
  const payload = token.split(".")[1];
  const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
  const json = decodeURIComponent(
    atob(base64)
      .split("")
      .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
      .join("")
  );
  return JSON.parse(json);
}

// ---------- API ----------

async function apiRequest(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(body.message || `Error HTTP ${res.status}`);
  }
  return body.data;
}

async function registerUser(email, password, fullName) {
  return apiRequest("/register", { method: "POST", body: JSON.stringify({ email, password, fullName }) });
}

async function loginUser(email, password) {
  return apiRequest("/login", { method: "POST", body: JSON.stringify({ email, password }) });
}

async function refreshSession() {
  return apiRequest("/refresh", { method: "POST", body: JSON.stringify({ refreshToken: state.refreshToken }) });
}

async function logoutSession() {
  return apiRequest("/logout", { method: "POST", body: JSON.stringify({ refreshToken: state.refreshToken }) });
}

async function validateSession() {
  return apiRequest("/validate", { method: "GET", headers: { Authorization: `Bearer ${state.accessToken}` } });
}

// ---------- Session persistence ----------

function persistSession(authResponse) {
  state.accessToken = authResponse.accessToken;
  state.refreshToken = authResponse.refreshToken;
  state.accessTokenExpiresAt = authResponse.accessTokenExpiresAt;
  sessionStorage.setItem("auth.accessToken", state.accessToken);
  sessionStorage.setItem("auth.refreshToken", state.refreshToken);
  sessionStorage.setItem("auth.accessTokenExpiresAt", state.accessTokenExpiresAt);
}

function clearSession() {
  state.accessToken = null;
  state.refreshToken = null;
  state.accessTokenExpiresAt = null;
  sessionStorage.removeItem("auth.accessToken");
  sessionStorage.removeItem("auth.refreshToken");
  sessionStorage.removeItem("auth.accessTokenExpiresAt");
  if (state.countdownTimer) clearInterval(state.countdownTimer);
}

function restoreSession() {
  const accessToken = sessionStorage.getItem("auth.accessToken");
  const refreshToken = sessionStorage.getItem("auth.refreshToken");
  const accessTokenExpiresAt = sessionStorage.getItem("auth.accessTokenExpiresAt");
  if (accessToken && refreshToken) {
    state.accessToken = accessToken;
    state.refreshToken = refreshToken;
    state.accessTokenExpiresAt = accessTokenExpiresAt;
    return true;
  }
  return false;
}

// ---------- Rendering ----------

function showAuthForms() {
  document.getElementById("auth-card").classList.remove("hidden");
  document.getElementById("session-panel").classList.add("hidden");
}

function showSessionPanel() {
  document.getElementById("auth-card").classList.add("hidden");
  document.getElementById("session-panel").classList.remove("hidden");
  renderSessionPanel();
  startCountdown();
}

function renderSessionPanel(rawValidateResponse) {
  const claims = decodeJwtPayload(state.accessToken);
  document.getElementById("session-email").textContent = claims.email || "—";
  document.getElementById("session-role").textContent = claims.role || "—";
  const permissions = claims.permissions || [];
  document.getElementById("session-permissions").innerHTML = permissions
    .map((p) => `<span class="permission-chip">${escapeHtml(p)}</span>`)
    .join("");
  if (rawValidateResponse) {
    document.getElementById("session-raw").textContent = JSON.stringify(rawValidateResponse, null, 2);
  }
}

function startCountdown() {
  if (state.countdownTimer) clearInterval(state.countdownTimer);
  const el = document.getElementById("session-countdown");
  const tick = () => {
    const remainingMs = new Date(state.accessTokenExpiresAt).getTime() - Date.now();
    if (remainingMs <= 0) {
      el.textContent = "expirado";
      clearInterval(state.countdownTimer);
      return;
    }
    const minutes = Math.floor(remainingMs / 60000);
    const seconds = Math.floor((remainingMs % 60000) / 1000);
    el.textContent = `${minutes}:${String(seconds).padStart(2, "0")}`;
  };
  tick();
  state.countdownTimer = setInterval(tick, 1000);
}

// ---------- Event wiring ----------

function wireTabToggle() {
  document.querySelectorAll(".tab-btn").forEach((btn) =>
    btn.addEventListener("click", () => {
      document.querySelectorAll(".tab-btn").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      const tab = btn.dataset.tab;
      document.getElementById("login-form").classList.toggle("hidden", tab !== "login");
      document.getElementById("register-form").classList.toggle("hidden", tab !== "register");
      hideErrorBanner();
    })
  );
}

function wireForms() {
  document.getElementById("login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    hideErrorBanner();
    const form = new FormData(e.target);
    try {
      const response = await loginUser(form.get("email"), form.get("password"));
      persistSession(response);
      showToast("Sesión iniciada", "success");
      showSessionPanel();
    } catch (err) {
      showErrorBanner(`No se pudo iniciar sesión: ${err.message}`);
    }
  });

  document.getElementById("register-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    hideErrorBanner();
    const form = new FormData(e.target);
    try {
      await registerUser(form.get("email"), form.get("password"), form.get("fullName"));
      showToast("Cuenta creada, ahora inicia sesión", "success");
      document.querySelector('.tab-btn[data-tab="login"]').click();
      document.getElementById("login-form").email.value = form.get("email");
    } catch (err) {
      showErrorBanner(`No se pudo registrar: ${err.message}`);
    }
  });

  document.getElementById("btn-validate").addEventListener("click", async () => {
    try {
      const response = await validateSession();
      renderSessionPanel(response);
      showToast("Token válido", "success");
    } catch (err) {
      showToast(`Validación falló: ${err.message}`, "error");
    }
  });

  document.getElementById("btn-refresh").addEventListener("click", async () => {
    try {
      const response = await refreshSession();
      persistSession(response);
      renderSessionPanel();
      startCountdown();
      showToast("Token renovado", "success");
    } catch (err) {
      showToast(`No se pudo renovar: ${err.message}`, "error");
    }
  });

  document.getElementById("btn-logout").addEventListener("click", async () => {
    try {
      await logoutSession();
    } catch (err) {
      // Si el logout falla (p.ej. el token ya estaba revocado), igual limpiamos
      // el estado local -- el usuario quiere salir de la sesión de todas formas.
      console.warn("logout backend call failed:", err.message);
    }
    clearSession();
    showToast("Sesión cerrada", "success");
    showAuthForms();
  });
}

// ---------- Boot ----------

document.addEventListener("DOMContentLoaded", () => {
  wireTabToggle();
  wireForms();
  if (restoreSession()) {
    showSessionPanel();
  }
});
