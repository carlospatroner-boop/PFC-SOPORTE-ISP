// app.js — Panel de Tickets del Sistema de Soporte Técnico ISP (equipo ACC)
// Consume la API real de ticket-service (Java 21 + Spring Boot) conectada al
// cluster CockroachDB fragmentado por zona. Sin build step: JS vanilla + Chart.js.

const API_BASE = "http://localhost:8002/api/v1";
const AUTH_API_BASE = "http://localhost:8001/api/v1/auth";
const REPORTS_API_BASE = "http://localhost:8005/api/v1/reports";
const POLL_INTERVAL_MS = 15000;

const ZONES = [
  { value: "QUEVEDO_CENTRO", label: "Centro", color: "#4f46e5" },
  { value: "QUEVEDO_NORTE", label: "Norte", color: "#06b6d4" },
  { value: "QUEVEDO_SUR", label: "Sur", color: "#f59e0b" },
];

const STATUSES = [
  { value: "NUEVO", label: "Nuevo", color: "#64748b" },
  { value: "ASIGNADO", label: "Asignado", color: "#7c3aed" },
  { value: "EN_PROGRESO", label: "En progreso", color: "#f59e0b" },
  { value: "ESCALADO", label: "Escalado", color: "#ef4444" },
  { value: "RESUELTO", label: "Resuelto", color: "#10b981" },
  { value: "CERRADO", label: "Cerrado", color: "#94a3b8" },
];

const OPEN_STATUSES = ["NUEVO", "ASIGNADO", "EN_PROGRESO", "ESCALADO"];
const CLOSED_STATUSES = ["RESUELTO", "CERRADO"];

const state = {
  tickets: [],
  zoneFilter: "ALL",
  view: "dashboard",
  charts: { zone: null, status: null, reportStatus: null, reportCategory: null },
  role: null,
  zone: null,
  users: [],
  reportSummary: null,
  reportTickets: [],
};

// ---------- Helpers ----------

const zoneMeta = (value) => ZONES.find((z) => z.value === value) || { label: value, color: "#94a3b8" };
const statusMeta = (value) => STATUSES.find((s) => s.value === value) || { label: value, color: "#94a3b8" };

function shortId(uuid) {
  return uuid ? uuid.slice(0, 8) : "—";
}

function formatDateTime(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  return d.toLocaleString("es-EC", { dateStyle: "medium", timeStyle: "short" });
}

function showToast(message, type = "success") {
  const container = document.getElementById("toast-container");
  const el = document.createElement("div");
  el.className = `toast ${type}`;
  el.textContent = message;
  container.appendChild(el);
  setTimeout(() => el.remove(), 4200);
}

function setConnectionState(status) {
  const el = document.getElementById("conn-indicator");
  el.classList.remove("conn-ok", "conn-error", "conn-unknown");
  if (status === "ok") {
    el.classList.add("conn-ok");
    el.innerHTML = `<span class="dot"></span> Conectado a ticket-service`;
  } else if (status === "error") {
    el.classList.add("conn-error");
    el.innerHTML = `<span class="dot"></span> Sin conexión al backend`;
  } else {
    el.classList.add("conn-unknown");
    el.innerHTML = `<span class="dot"></span> Verificando conexión…`;
  }
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split(".")[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

function renderSessionIndicator() {
  const el = document.getElementById("session-indicator");
  const token = sessionStorage.getItem("auth.accessToken");
  const claims = token ? decodeJwtPayload(token) : null;
  state.role = claims ? claims.role : null;
  state.zone = claims ? claims.zone : null;
  if (claims) {
    el.innerHTML = `${escapeHtml(claims.email)} (${escapeHtml(claims.role)}) · <a href="#" id="logout-link" style="color:#9ca3d0">Cerrar sesión</a>`;
    document.getElementById("logout-link").addEventListener("click", (e) => {
      e.preventDefault();
      sessionStorage.removeItem("auth.accessToken");
      sessionStorage.removeItem("auth.refreshToken");
      sessionStorage.removeItem("auth.accessTokenExpiresAt");
      window.location.href = "auth/index.html";
    });
  } else {
    el.innerHTML = `<a href="auth/index.html" style="color:#9ca3d0">Iniciar sesión</a>`;
  }
  applyRoleVisibility();
}

// El backend ya filtra los DATOS por rol (ver TicketService.listTickets); esto solo
// oculta CONTROLES que fuera de tu rol darian 403 (filtro de zona, crear, cambiar
// estado), para que la UI no invite a hacer algo que el backend va a rechazar.
function applyRoleVisibility() {
  const zoneFilterEl = document.getElementById("zone-filter");
  const scopeLabel = document.getElementById("scope-label");
  const newTicketBtn = document.getElementById("new-ticket-btn");
  const isCliente = state.role === "CLIENTE";

  const isAdmin = state.role === "ADMIN" || !state.role;
  zoneFilterEl.classList.toggle("hidden", !isAdmin);
  if (isAdmin) {
    scopeLabel.classList.add("hidden");
  } else {
    scopeLabel.classList.remove("hidden");
    scopeLabel.textContent = isCliente
      ? "Mostrando tus solicitudes"
      : `Zona: ${zoneMeta(state.zone).label}`;
  }

  newTicketBtn.classList.toggle("hidden", state.role === "TECNICO");
  newTicketBtn.textContent = isCliente ? "+ Nueva solicitud" : "+ Nuevo ticket";

  const navAdmin = document.getElementById("nav-admin");
  navAdmin.classList.toggle("hidden", state.role !== "ADMIN");
  if (state.role !== "ADMIN" && state.view === "admin") {
    setView("dashboard");
  }

  const navReports = document.getElementById("nav-reports");
  navReports.classList.toggle("hidden", state.role !== "ADMIN");
  if (state.role !== "ADMIN" && state.view === "reports") {
    setView("dashboard");
  }

  // CLIENTE: los KPIs/graficos y la jerga tecnica (zonas del cluster, particiones,
  // nombre del microservicio) son ruido para un usuario final que solo quiere ver
  // el estado de sus propias solicitudes -- se simplifica a una sola vista.
  document.getElementById("nav-dashboard").classList.toggle("hidden", isCliente);
  document.getElementById("sidebar-zones-section").classList.toggle("hidden", isCliente);
  document.getElementById("sidebar-tech-footer").classList.toggle("hidden", isCliente);
  document.getElementById("nav-board-label").textContent = isCliente ? "Mis Solicitudes" : "Tablero de Tickets";
  if (isCliente && state.view === "dashboard") {
    setView("board");
  }
}

function showErrorBanner(message) {
  const banner = document.getElementById("error-banner");
  banner.textContent = message;
  banner.classList.remove("hidden");
}
function hideErrorBanner() {
  document.getElementById("error-banner").classList.add("hidden");
}

// ---------- API ----------

function authHeaders() {
  const token = sessionStorage.getItem("auth.accessToken");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function apiRequest(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...authHeaders() },
    ...options,
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    if (res.status === 401) {
      throw new Error(`${body.message || "No autenticado"} — inicia sesión en auth/index.html`);
    }
    throw new Error(body.message || `Error HTTP ${res.status}`);
  }
  return body.data;
}

async function apiRequestAuth(path, options = {}) {
  const res = await fetch(`${AUTH_API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...authHeaders() },
    ...options,
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(body.message || `Error HTTP ${res.status}`);
  }
  return body.data;
}

async function createUserAsAdmin(payload) {
  return apiRequestAuth("/admin/users", { method: "POST", body: JSON.stringify(payload) });
}

async function fetchUsersAsAdmin() {
  return apiRequestAuth("/admin/users");
}

// report-service exige rol ADMIN (ver AuthGatewayFilter) -- el envoltorio es igual
// a apiRequestAuth, solo que no hereda su base URL.
async function apiRequestReports(path, options = {}) {
  const res = await fetch(`${REPORTS_API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...authHeaders() },
    ...options,
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(body.message || `Error HTTP ${res.status}`);
  }
  return body.data;
}

async function fetchReportSummary() {
  return apiRequestReports("/summary");
}

async function fetchReportTickets() {
  return apiRequestReports("/tickets");
}

// El CSV no puede abrirse con un <a href> directo porque report-service exige el
// header Authorization -- se pide como blob y se dispara la descarga a mano.
async function downloadReportCsv() {
  const res = await fetch(`${REPORTS_API_BASE}/export.csv`, { headers: authHeaders() });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || `Error HTTP ${res.status}`);
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `tickets-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

async function fetchTickets() {
  const query = state.zoneFilter !== "ALL" ? `?zone=${state.zoneFilter}` : "";
  return apiRequest(`/tickets${query}`);
}

async function createTicketRequest(payload) {
  return apiRequest("/tickets", { method: "POST", body: JSON.stringify(payload) });
}

// Desde que ticket-service particiona por fecha_apertura (ya no por zona, ver
// ADR-0003), la ruta ya no necesita la zona -- el backend resuelve el ticket por
// su id via un indice unico secundario.
async function updateStatusRequest(id, status) {
  return apiRequest(`/tickets/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

// ---------- Data loading ----------

async function loadTickets({ silent = false } = {}) {
  const refreshBtn = document.getElementById("refresh-btn");
  if (!silent) refreshBtn.classList.add("spinning");
  try {
    const data = await fetchTickets();
    state.tickets = Array.isArray(data) ? data : [];
    setConnectionState("ok");
    hideErrorBanner();
  } catch (err) {
    setConnectionState("error");
    showErrorBanner(
      `No se pudo conectar con ticket-service en ${API_BASE}. ` +
      `¿Está corriendo el cluster CockroachDB y el microservicio? Detalle: ${err.message}`
    );
  } finally {
    renderAll();
    if (!silent) setTimeout(() => refreshBtn.classList.remove("spinning"), 500);
  }
}

// ---------- Rendering ----------

function renderAll() {
  renderZoneLegend();
  renderKPIs();
  try {
    renderCharts();
  } catch (err) {
    // Si Chart.js no llego a cargar (p.ej. sin internet en el aula), no debe
    // romper el resto del panel -- KPIs y tablero deben seguir funcionando.
    console.warn("No se pudieron renderizar los gráficos (¿Chart.js no cargó desde el CDN?):", err);
  }
  renderBoard();
}

function renderZoneLegend() {
  const el = document.getElementById("zone-legend");
  el.innerHTML = ZONES.map(
    (z) => `<li><span class="swatch" style="background:${z.color}"></span>${z.label} <small style="color:#7d82ab">(zona)</small></li>`
  ).join("");
}

function renderKPIs() {
  const t = state.tickets;
  document.getElementById("kpi-total").textContent = t.length;
  document.getElementById("kpi-open").textContent = t.filter((x) => OPEN_STATUSES.includes(x.status)).length;
  document.getElementById("kpi-sla").textContent = t.filter((x) => x.slaBreached).length;
  document.getElementById("kpi-resolved").textContent = t.filter((x) => CLOSED_STATUSES.includes(x.status)).length;
}

function renderCharts() {
  const zoneCounts = ZONES.map((z) => state.tickets.filter((t) => t.zone === z.value).length);
  const statusCounts = STATUSES.map((s) => state.tickets.filter((t) => t.status === s.value).length);

  const zoneCtx = document.getElementById("chart-zone");
  const statusCtx = document.getElementById("chart-status");

  if (state.charts.zone) {
    state.charts.zone.data.datasets[0].data = zoneCounts;
    state.charts.zone.update();
  } else {
    state.charts.zone = new Chart(zoneCtx, {
      type: "doughnut",
      data: {
        labels: ZONES.map((z) => z.label),
        datasets: [{ data: zoneCounts, backgroundColor: ZONES.map((z) => z.color), borderWidth: 0 }],
      },
      options: {
        plugins: { legend: { position: "bottom", labels: { boxWidth: 10, font: { size: 11 } } } },
        cutout: "62%",
      },
    });
  }

  if (state.charts.status) {
    state.charts.status.data.datasets[0].data = statusCounts;
    state.charts.status.update();
  } else {
    state.charts.status = new Chart(statusCtx, {
      type: "bar",
      data: {
        labels: STATUSES.map((s) => s.label),
        datasets: [{ data: statusCounts, backgroundColor: STATUSES.map((s) => s.color), borderRadius: 6 }],
      },
      options: {
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } },
      },
    });
  }
}

function renderBoard() {
  const board = document.getElementById("board");
  board.innerHTML = STATUSES.map((status) => {
    const ticketsInColumn = state.tickets.filter((t) => t.status === status.value);
    const cards = ticketsInColumn.length
      ? ticketsInColumn.map(renderCard).join("")
      : `<div class="empty-column">Sin tickets</div>`;
    return `
      <div class="board-column">
        <div class="board-column-header" style="color:${status.color}">
          ${status.label} <span class="count">${ticketsInColumn.length}</span>
        </div>
        ${cards}
      </div>`;
  }).join("");

  // listeners: click en tarjeta abre detalle, cambio en select actualiza estado
  board.querySelectorAll(".ticket-card").forEach((card) => {
    card.addEventListener("click", (e) => {
      if (e.target.tagName === "SELECT") return;
      const ticket = state.tickets.find((t) => t.ticketId === card.dataset.id && t.zone === card.dataset.zone);
      if (ticket) openDetailModal(ticket);
    });
  });
  board.querySelectorAll(".t-actions select").forEach((select) => {
    select.addEventListener("click", (e) => e.stopPropagation());
    select.addEventListener("change", async (e) => {
      const { id } = e.target.dataset;
      try {
        await updateStatusRequest(id, e.target.value);
        showToast(`Ticket ${shortId(id)} → ${statusMeta(e.target.value).label}`, "success");
        await loadTickets({ silent: true });
      } catch (err) {
        showToast(`No se pudo actualizar: ${err.message}`, "error");
      }
    });
  });
}

function renderCard(t) {
  const zone = zoneMeta(t.zone);
  const isCliente = state.role === "CLIENTE";

  // Para CLIENTE se omite la jerga interna (categoria/prioridad "pendiente IA",
  // que habla del pipeline de clasificacion, no del estado de su solicitud).
  const category = t.category
    ? `<span class="badge" style="background:${zone.color}">${t.category}</span>`
    : isCliente ? "" : `<span class="badge badge-pending">categoría: pendiente IA</span>`;
  const priority = t.priority
    ? `<span class="badge badge-outline">${t.priority}</span>`
    : isCliente ? "" : `<span class="badge badge-pending">prioridad: pendiente IA</span>`;
  const slaBadge = t.slaBreached
    ? `<span class="badge" style="background:#ef4444">${isCliente ? "Atención urgente" : "SLA vencido"}</span>`
    : "";

  // CLIENTE no tiene el permiso ticket:update:status (ticket-service responde 403),
  // asi que se muestra el estado como badge de solo lectura en vez de un <select>.
  const statusMetaValue = statusMeta(t.status);
  const statusControl = isCliente
    ? `<span class="badge" style="background:${statusMetaValue.color}">${statusMetaValue.label}</span>`
    : `<select data-zone="${t.zone}" data-id="${t.ticketId}">
        ${STATUSES.map((s) => `<option value="${s.value}" ${s.value === t.status ? "selected" : ""}>${s.label}</option>`).join("")}
      </select>`;

  // El identificador corto y la zona/particion son ruido tecnico para un cliente
  // final que solo tiene una o pocas solicitudes -- se muestra solo la fecha.
  const cardHeader = isCliente
    ? `Reportado el ${formatDateTime(t.createdAt).split(",")[0]}`
    : `#${shortId(t.ticketId)} · ${zone.label}`;

  return `
    <div class="ticket-card" style="border-left-color:${zone.color}" data-id="${t.ticketId}" data-zone="${t.zone}">
      <div class="t-id">${cardHeader}</div>
      <div class="t-desc">${escapeHtml(t.description || "(sin descripción)")}</div>
      <div class="t-meta">${category}${priority}${slaBadge}</div>
      <div class="t-actions">
        ${statusControl}
      </div>
    </div>`;
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

// ---------- Detail modal ----------

function openDetailModal(t) {
  const zone = zoneMeta(t.zone);
  const status = statusMeta(t.status);
  const isCliente = state.role === "CLIENTE";

  // A un CLIENTE no le sirve ver UUIDs crudos, el nombre de la particion, ni el
  // estado interno de un pipeline de IA que todavia no existe -- se muestra solo
  // lo que le importa a alguien reportando/siguiendo su propia solicitud.
  const rows = isCliente
    ? `
      <dt>Zona</dt><dd><span class="badge" style="background:${zone.color}">${zone.label}</span></dd>
      <dt>Estado</dt><dd><span class="badge" style="background:${status.color}">${status.label}</span></dd>
      <dt>Técnico asignado</dt><dd>${t.technicianId ? "Sí" : "Todavía no"}</dd>
      <dt>Reportado</dt><dd>${formatDateTime(t.createdAt)}</dd>
      <dt>Tiempo estimado</dt><dd>${formatDateTime(t.slaDeadline)} ${t.slaBreached ? "⚠ fuera de plazo" : ""}</dd>
    `
    : `
      <dt>Ticket</dt><dd>#${t.ticketId}</dd>
      <dt>Zona</dt><dd><span class="badge" style="background:${zone.color}">${zone.label}</span> · partición ${t.zone}</dd>
      <dt>Estado</dt><dd><span class="badge" style="background:${status.color}">${status.label}</span></dd>
      <dt>Cliente</dt><dd>${t.clientId}</dd>
      <dt>Técnico</dt><dd>${t.technicianId || "sin asignar"}</dd>
      <dt>Categoría</dt><dd>${t.category || "pendiente de clasificación por ai-service"}</dd>
      <dt>Prioridad</dt><dd>${t.priority || "pendiente de clasificación por ai-service"}</dd>
      <dt>Creado</dt><dd>${formatDateTime(t.createdAt)}</dd>
      <dt>Vence SLA</dt><dd>${formatDateTime(t.slaDeadline)} ${t.slaBreached ? "⚠ vencido" : ""}</dd>
    `;

  document.getElementById("detail-content").innerHTML = `
    <dl>${rows}</dl>
    <div class="desc-block">${escapeHtml(t.description || "")}</div>
  `;
  document.getElementById("detail-modal-title").textContent = isCliente ? "Detalle de tu solicitud" : "Detalle del ticket";
  document.getElementById("modal-detail-overlay").classList.remove("hidden");
}

// ---------- View switching ----------

function setView(view) {
  state.view = view;
  document.querySelectorAll(".nav-item").forEach((n) => n.classList.toggle("active", n.dataset.view === view));
  document.getElementById("view-dashboard").classList.toggle("hidden", view !== "dashboard");
  document.getElementById("view-board").classList.toggle("hidden", view !== "board");
  document.getElementById("view-admin").classList.toggle("hidden", view !== "admin");
  document.getElementById("view-reports").classList.toggle("hidden", view !== "reports");
  if (view === "admin") loadUsers();
  if (view === "reports") loadReports();
  const titles = {
    dashboard: ["Dashboard", "Vista general del sistema distribuido de soporte técnico"],
    board: state.role === "CLIENTE"
      ? ["Mis Solicitudes", "Aquí puedes ver el estado de las solicitudes de soporte que has reportado"]
      : ["Tablero de Tickets", "Ciclo de vida de cada solicitud, agrupado por estado"],
    admin: ["Administración", "Crear cuentas CLIENTE/TECNICO/ADMIN (solo visible para ADMIN)"],
    reports: ["Reportes", "Modelo de lectura CQRS de report-service (solo visible para ADMIN)"],
  };
  document.getElementById("view-title").textContent = titles[view][0];
  document.getElementById("view-subtitle").textContent = titles[view][1];
  // El filtro de zona / boton de nuevo ticket son de las vistas dashboard y tablero;
  // no aplican en administracion ni en reportes.
  document.querySelector(".topbar-actions").classList.toggle("hidden", view === "admin" || view === "reports");
}

// ---------- Event wiring ----------

function wireEvents() {
  document.querySelectorAll(".nav-item").forEach((n) =>
    n.addEventListener("click", (e) => {
      e.preventDefault();
      setView(n.dataset.view);
    })
  );

  document.querySelectorAll(".zone-filter .chip").forEach((chip) =>
    chip.addEventListener("click", () => {
      document.querySelectorAll(".zone-filter .chip").forEach((c) => c.classList.remove("active"));
      chip.classList.add("active");
      state.zoneFilter = chip.dataset.zone;
      loadTickets();
    })
  );

  document.getElementById("refresh-btn").addEventListener("click", () => loadTickets());

  const modalOverlay = document.getElementById("modal-overlay");
  document.getElementById("new-ticket-btn").addEventListener("click", () => {
    document.getElementById("create-form").reset();
    modalOverlay.classList.remove("hidden");
  });
  document.getElementById("modal-close").addEventListener("click", () => modalOverlay.classList.add("hidden"));
  document.getElementById("modal-cancel").addEventListener("click", () => modalOverlay.classList.add("hidden"));
  modalOverlay.addEventListener("click", (e) => { if (e.target === modalOverlay) modalOverlay.classList.add("hidden"); });

  const detailOverlay = document.getElementById("modal-detail-overlay");
  document.getElementById("detail-close").addEventListener("click", () => detailOverlay.classList.add("hidden"));
  detailOverlay.addEventListener("click", (e) => { if (e.target === detailOverlay) detailOverlay.classList.add("hidden"); });

  document.getElementById("create-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = new FormData(e.target);
    const payload = Object.fromEntries(form.entries());
    try {
      await createTicketRequest(payload);
      showToast("Ticket creado correctamente", "success");
      modalOverlay.classList.add("hidden");
      await loadTickets();
    } catch (err) {
      showToast(`No se pudo crear el ticket: ${err.message}`, "error");
    }
  });

  wireAdminView();
  wireReportsView();
}

// ---------- Vista de administracion ----------

function wireAdminView() {
  const roleInput = document.getElementById("admin-role-input");
  const zoneField = document.getElementById("admin-zone-field");

  const syncZoneField = () => zoneField.classList.toggle("hidden", roleInput.value !== "TECNICO");

  document.querySelectorAll("#admin-role-toggle .role-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll("#admin-role-toggle .role-btn").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      roleInput.value = btn.dataset.role;
      syncZoneField();
    });
  });
  syncZoneField();

  document.getElementById("admin-create-user-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = new FormData(e.target);
    const payload = Object.fromEntries(form.entries());
    if (payload.role !== "TECNICO") {
      delete payload.zone; // el backend rechaza "zone" para roles distintos de TECNICO
    }
    try {
      const created = await createUserAsAdmin(payload);
      showToast(`Cuenta creada: ${created.email} (${created.role})`, "success");
      e.target.reset();
      document.querySelectorAll("#admin-role-toggle .role-btn").forEach((b) =>
        b.classList.toggle("active", b.dataset.role === "CLIENTE"));
      syncZoneField();
      await loadUsers();
    } catch (err) {
      showToast(`No se pudo crear la cuenta: ${err.message}`, "error");
    }
  });
}

async function loadUsers() {
  const list = document.getElementById("admin-created-users");
  list.innerHTML = `<li class="empty-column">Cargando…</li>`;
  try {
    state.users = await fetchUsersAsAdmin();
    renderUsers();
  } catch (err) {
    list.innerHTML = `<li class="empty-column">No se pudo cargar: ${escapeHtml(err.message)}</li>`;
  }
}

function initials(name) {
  return (name || "?").trim().split(/\s+/).slice(0, 2).map((w) => w[0].toUpperCase()).join("");
}

function renderUsers() {
  const list = document.getElementById("admin-created-users");
  if (!state.users.length) {
    list.innerHTML = `<li class="empty-column">No hay cuentas todavia</li>`;
    return;
  }
  list.innerHTML = state.users.map((u) => `
    <li class="user-row">
      <div class="user-avatar role-${u.role}">${initials(u.fullName)}</div>
      <div class="user-info">
        <div class="u-email">${u.active ? "" : `<span class="user-inactive-dot" title="Inactivo"></span>`}${escapeHtml(u.email)}</div>
        <div class="u-name">${escapeHtml(u.fullName)}</div>
      </div>
      <div class="user-meta">
        <span class="role-pill role-${u.role}">${escapeHtml(u.role)}</span>
        ${u.zone ? `<span class="user-zone">${zoneMeta(u.zone).label}</span>` : ""}
      </div>
    </li>`).join("");
}

// ---------- Vista de reportes (report-service, lado de lectura CQRS) ----------

function showReportsError(message) {
  const banner = document.getElementById("reports-error");
  banner.textContent = message;
  banner.classList.remove("hidden");
}
function hideReportsError() {
  document.getElementById("reports-error").classList.add("hidden");
}

async function loadReports() {
  hideReportsError();
  try {
    const [summary, tickets] = await Promise.all([fetchReportSummary(), fetchReportTickets()]);
    state.reportSummary = summary;
    state.reportTickets = Array.isArray(tickets) ? tickets : [];
  } catch (err) {
    showReportsError(
      `No se pudo conectar con report-service en ${REPORTS_API_BASE}. ` +
      `¿Está corriendo el servicio? Detalle: ${err.message}`
    );
    state.reportSummary = null;
    state.reportTickets = [];
  }
  renderReportKPIs();
  try {
    renderReportCharts();
  } catch (err) {
    console.warn("No se pudieron renderizar los gráficos de reportes:", err);
  }
  renderReportTable();
}

function renderReportKPIs() {
  const s = state.reportSummary;
  document.getElementById("rpt-kpi-total").textContent = s ? s.totalTickets : "–";
  document.getElementById("rpt-kpi-zones").textContent = s ? Object.keys(s.byZone || {}).length : "–";
  document.getElementById("rpt-kpi-categories").textContent = s ? Object.keys(s.byCategory || {}).length : "–";
}

function renderReportCharts() {
  const s = state.reportSummary;
  const byStatus = s ? s.byStatus || {} : {};
  const byCategory = s ? s.byCategory || {} : {};

  const statusLabels = Object.keys(byStatus).map((k) => statusMeta(k).label);
  const statusColors = Object.keys(byStatus).map((k) => statusMeta(k).color);
  const statusData = Object.values(byStatus);

  const categoryLabels = Object.keys(byCategory);
  const categoryData = Object.values(byCategory);
  const categoryColors = ["#4f46e5", "#06b6d4", "#f59e0b", "#10b981", "#ef4444", "#8b5cf6"];

  const statusCtx = document.getElementById("chart-report-status");
  if (state.charts.reportStatus) {
    state.charts.reportStatus.data.labels = statusLabels;
    state.charts.reportStatus.data.datasets[0].data = statusData;
    state.charts.reportStatus.data.datasets[0].backgroundColor = statusColors;
    state.charts.reportStatus.update();
  } else {
    state.charts.reportStatus = new Chart(statusCtx, {
      type: "bar",
      data: { labels: statusLabels, datasets: [{ data: statusData, backgroundColor: statusColors, borderRadius: 6 }] },
      options: {
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } },
      },
    });
  }

  const categoryCtx = document.getElementById("chart-report-category");
  if (state.charts.reportCategory) {
    state.charts.reportCategory.data.labels = categoryLabels;
    state.charts.reportCategory.data.datasets[0].data = categoryData;
    state.charts.reportCategory.update();
  } else {
    state.charts.reportCategory = new Chart(categoryCtx, {
      type: "doughnut",
      data: { labels: categoryLabels, datasets: [{ data: categoryData, backgroundColor: categoryColors, borderWidth: 0 }] },
      options: {
        plugins: { legend: { position: "bottom", labels: { boxWidth: 10, font: { size: 11 } } } },
        cutout: "62%",
      },
    });
  }
}

function renderReportTable() {
  const body = document.getElementById("reports-table-body");
  if (!state.reportTickets.length) {
    body.innerHTML = `<tr><td colspan="7" class="empty-column">Sin datos todavía</td></tr>`;
    return;
  }
  body.innerHTML = state.reportTickets.map((t) => `
    <tr>
      <td>${zoneMeta(t.zone).label}</td>
      <td>#${shortId(t.ticketId)}</td>
      <td><span class="badge" style="background:${statusMeta(t.status).color}">${statusMeta(t.status).label}</span></td>
      <td>${t.category || "—"}</td>
      <td>${t.priority || "—"}</td>
      <td>${t.technicianId ? shortId(t.technicianId) : "sin asignar"}</td>
      <td>${formatDateTime(t.createdAt)}</td>
    </tr>`).join("");
}

function wireReportsView() {
  document.getElementById("reports-refresh-btn").addEventListener("click", () => loadReports());
  document.getElementById("reports-export-btn").addEventListener("click", async () => {
    try {
      await downloadReportCsv();
      showToast("CSV descargado", "success");
    } catch (err) {
      showToast(`No se pudo descargar el CSV: ${err.message}`, "error");
    }
  });
}

// ---------- Boot ----------

document.addEventListener("DOMContentLoaded", () => {
  wireEvents();
  renderZoneLegend();
  renderSessionIndicator();
  loadTickets();
  setInterval(() => loadTickets({ silent: true }), POLL_INTERVAL_MS);
});
