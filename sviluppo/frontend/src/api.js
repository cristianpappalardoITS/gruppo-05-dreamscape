const API_BASE = '/api/v1';

export class ApiError extends Error {
  constructor({ status, code, message, fieldErrors }) {
    super(message || 'Si è verificato un errore. Riprova.');
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors ?? [];
  }
}

let accessToken = null;

export async function api(path, { method = 'GET', body } = {}) {
  let response;
  try {
    const headers = body === undefined ? {} : { 'Content-Type': 'application/json' };
    if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
    response = await fetch(API_BASE + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: 'same-origin',
    });
  } catch {
    throw new ApiError({ status: 0, code: 'NETWORK_ERROR', message: 'Il server non risponde. Riprova tra poco.' });
  }

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError({ status: response.status, ...data });
  }
  return data;
}

export async function login(loginName, password) {
  const data = await api('/auth/login', {
    method: 'POST',
    body: { login: loginName, password },
  });
  accessToken = data.accessToken;
  return data.user;
}

export function register({ username, email, password, termsAccepted, privacyAccepted }) {
  return api('/auth/register', {
    method: 'POST',
    body: { username, email, password, termsAccepted, privacyAccepted },
  });
}

export function errorsByField(error) {
  return Object.fromEntries((error?.fieldErrors ?? []).map(({ field, message }) => [field, message]));
}
