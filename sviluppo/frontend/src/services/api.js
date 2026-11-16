// src/api.js
// api.js: l'unico file che parla con il backend.
// Il token di accesso resta in memoria; il refresh token è nel cookie HttpOnly.

const API_BASE = "/api/v1";
// Pagina di accesso: cambiatela se il percorso è un altro
const LOGIN_PAGE = "/login";

let accessToken = null;
let refreshing = null;

export class ApiError extends Error {
  constructor({ status, code, message, fieldErrors }) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors ?? [];
  }
}

const NETWORK_ERROR = {
  status: 0,
  code: "NETWORK_ERROR",
  message: "Il server non risponde. Riprova tra poco.",
};

function send(path, method, body) {
  const headers = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  return fetch(API_BASE + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    credentials: "same-origin",
  });
}

export function refresh() {
  if (!refreshing) {
    refreshing = fetch(API_BASE + "/auth/refresh", { method: "POST", credentials: "same-origin" })
      .then(async (response) => {
        accessToken = response.ok ? (await response.json()).accessToken : null;
        return accessToken !== null;
      })
      .catch(() => false)
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

export async function api(path, { method = "GET", body } = {}) {
  const isAuthCall = path.startsWith("/auth/");
  let response;
  try {
    response = await send(path, method, body);
    if (response.status === 401 && !isAuthCall && (await refresh())) {
      response = await send(path, method, body);
    }
  } catch {
    throw new ApiError(NETWORK_ERROR);
  }

  if (response.status === 401 && !isAuthCall) {
    window.location.href = LOGIN_PAGE;
    return new Promise(() => {});
  }
  if (response.status === 202 || response.status === 204) return null;

  const data = await response.json().catch(() => null);
  if (!response.ok) throw new ApiError(data ?? { ...NETWORK_ERROR, status: response.status });
  return data;
}

export async function login(loginName, password) {
  const data = await api("/auth/login", { method: "POST", body: { login: loginName, password } });
  accessToken = data.accessToken;
  return data.user;
}

export async function logout() {
  try {
    await api("/auth/logout", { method: "POST" });
  } finally {
    accessToken = null;
  }
}

export function errorsByField(error) {
  return Object.fromEntries((error?.fieldErrors ?? []).map((item) => [item.field, item.message]));
}