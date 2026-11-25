// api.js: l'unico file che parla con il backend.
// Il token di accesso resta in memoria; il refresh token è nel cookie HttpOnly.

const API_BASE = '/api/v1';
// Pagina di accesso: cambiatela se il percorso è un altro
const LOGIN_PAGE = '/login';

let accessToken = null;
// Rinnovo del token in corso: tutte le chiamate che lo aspettano usano lo stesso
let refreshing = null;

// Errore delle API: stessi campi della risposta del backend
export class ApiError extends Error {
  constructor({ status, code, message, fieldErrors, problems }) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors ?? [];
    // Solo negli errori con un elenco di problemi (es. INVALID_DECK)
    this.problems = problems ?? [];
  }
}

// Errore senza risposta del backend (spento, rete assente)
const NETWORK_ERROR = {
  status: 0,
  code: 'NETWORK_ERROR',
  message: 'Il server non risponde. Riprova tra poco.',
};

// Il corpo è JSON, oppure un FormData per i file (caricamento delle immagini del backoffice):
// con FormData il Content-Type lo mette il browser, con il separatore delle parti
function send(path, method, body) {
  const headers = {};
  const isForm = body instanceof FormData;
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json';
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  return fetch(API_BASE + path, {
    method,
    headers,
    body: body === undefined || isForm ? body : JSON.stringify(body),
    credentials: 'same-origin', // manda il cookie del refresh token
  });
}

// Chiede un token di accesso nuovo con il cookie. Restituisce true se l'utente è collegato.
// Mai due rinnovi insieme: il backend cambia il cookie a ogni rinnovo, e se riceve due volte
// lo stesso cookie scollega l'utente da tutti i dispositivi.
export function refresh() {
  if (!refreshing) {
    refreshing = fetch(API_BASE + '/auth/refresh', { method: 'POST', credentials: 'same-origin' })
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

// Chiamata alle API. Esempi:
//   await api('/packs')
//   await api(`/packs/${packId}/open`, { method: 'POST' })
//   await api('/me', { method: 'PUT', body: { email } })
//   await api('/admin/images', { method: 'POST', body: formData })   (FormData con il file)
// Restituisce i dati della risposta, oppure null se la risposta è vuota (202, 204).
// Se qualcosa va storto lancia un ApiError.
export async function api(path, { method = 'GET', body } = {}) {
  const isAuthCall = path.startsWith('/auth/');
  let response;
  try {
    response = await send(path, method, body);
    if (response.status === 401 && !isAuthCall && (await refresh())) {
      response = await send(path, method, body); // riprova una volta con il token nuovo
    }
  } catch {
    throw new ApiError(NETWORK_ERROR);
  }

  if (response.status === 401 && !isAuthCall) {
    window.location.href = LOGIN_PAGE; // sessione finita: si torna all'accesso
    return new Promise(() => {}); // la pagina sta cambiando: non continua
  }
  if (response.status === 202 || response.status === 204) return null;

  const data = await response.json().catch(() => null);
  if (!response.ok) throw new ApiError(data ?? { ...NETWORK_ERROR, status: response.status });
  return data;
}

// true se c'è un token di accesso: all'avvio main.jsx lo chiede con refresh()
export function isLoggedIn() {
  return accessToken !== null;
}

// Accesso con username o email. Restituisce l'utente: { id, username, role, credits }
export async function login(loginName, password) {
  const data = await api('/auth/login', { method: 'POST', body: { login: loginName, password } });
  accessToken = data.accessToken;
  return data.user;
}

// Uscita: il backend cancella il cookie, qui si dimentica il token
export async function logout() {
  try {
    await api('/auth/logout', { method: 'POST' });
  } finally {
    accessToken = null;
  }
}

// Trasforma fieldErrors in un oggetto { nomeCampo: messaggio }, comodo nei form
export function errorsByField(error) {
  return Object.fromEntries((error?.fieldErrors ?? []).map((item) => [item.field, item.message]));
}
