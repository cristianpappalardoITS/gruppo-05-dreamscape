# Guida ai dati per il frontend · Dreamscape

Per il team **WMD**. Spiega quali dati arrivano dal backend, come chiederli e come mandarli, pagina per pagina, così la sitemap e le pagine restano coerenti con il database.

> 🚧 **Proposta di SWD da confermare insieme.** I dati vengono dalla [proposta di database](../database/schema.md). I nomi delle API seguono [naming.md](../naming.md) e sono una proposta: quelli definitivi saranno in **Swagger** quando il backend esiste. Se qualcosa non torna con quello che vi serve, ditelo a Marco o Cristian.

## Indice

1. [Come funziona in breve](#1-come-funziona-in-breve)
2. [Regole comuni delle API](#2-regole-comuni-delle-api)
3. [Accesso con JWT](#3-accesso-con-jwt)
4. [Il file api.js](#4-il-file-apijs)
5. [Gli oggetti che ricevete](#5-gli-oggetti-che-ricevete)
6. [Pagine e chiamate](#6-pagine-e-chiamate)
7. [Come si monta una carta](#7-come-si-monta-una-carta)
8. [Elenco delle pagine per la sitemap](#8-elenco-delle-pagine-per-la-sitemap)
9. [Regole da ricordare](#9-regole-da-ricordare)
10. [Cose ancora da decidere](#10-cose-ancora-da-decidere)

---

## 1. Come funziona in breve

- Il frontend è fatto di **pagine HTML** con **CSS** e **JavaScript**. I **componenti** che si ripetono, come la carta, sono in **React** (decisione del 2/11). Gli strumenti li sceglie Gaia nella #72.
- Il JavaScript della pagina chiede i dati al backend tramite `api.js`, che usa `fetch()` e riceve **JSON**. Poi riempie la pagina e passa i dati ai componenti.
- Sito e API hanno **lo stesso indirizzo**: il container del frontend serve il sito e manda `/api` al backend. In locale si avvia tutto con `docker compose up` (decisione 7 del 2/11).
- Il backend fa tutti i conti e tutti i controlli: estrazione dei pacchetti, validità dei mazzi, partite, scambi. Il frontend **mostra** quello che riceve e **manda** quello che l'utente sceglie.
- Il giocatore si chiama **Dreamweaver**. Le fazioni sono **Dreams**, **Nightmares** e **Memories**.

```
 Pagina HTML (+ componenti React)  ──api.js──▶  Backend (Spring Boot)  ──▶  Database (PostgreSQL)
        ▲                                              │
        └──────────────────── JSON ────────────────────┘
```

---

## 2. Regole comuni delle API

| Cosa | Regola | Esempio |
|------|--------|---------|
| Indirizzo base | Tutte le API partono da `/api/v1`, sullo stesso indirizzo del sito: uguale in locale e alla consegna | `/api/v1/decks` |
| Id | Ogni id è un **UUID**: un testo di 36 caratteri. Si usa così come arriva, anche negli URL; non si calcola e non si confronta come un numero | `"019a1f2e-7c41-7d3a-9b2e-5f6a7b8c9d01"`, `/sfide/019a1f2e-…` |
| Formato | JSON in entrata e in uscita, nomi in `camelCase` | `{ "totalCards": 20, "isValid": true }` |
| Risorse | In inglese, al plurale, il verbo è il metodo HTTP | `GET /decks`, `POST /decks`, `DELETE /decks/{deckId}` |
| Date | Testo ISO 8601 in UTC: va convertito nell'ora locale per mostrarlo | `"2026-10-26T14:30:00Z"` |
| Immagini | URL relativi al backend, in WebP: si mettono così come sono in `src` o si usano nel disegno della carta. Il browser le tiene in cache (vedi [capitolo 7](#7-come-si-monta-una-carta)) | `"/images/cards/base-004-v1.webp"` |
| Liste lunghe | Con paginazione: `page` (da 0) e `size` | `GET /me/cards?page=0&size=24` |

Una lista paginata arriva così:

```json
{
  "items": [ ],
  "page": 0,
  "size": 24,
  "totalItems": 57,
  "totalPages": 3
}
```

### Errori

Ogni errore ha lo stesso formato, così si mostra il messaggio senza logica speciale:

```json
{
  "status": 409,
  "code": "USERNAME_TAKEN",
  "message": "Questo username è già usato.",
  "fieldErrors": [ { "field": "username", "message": "Questo username è già usato." } ]
}
```

| Codice HTTP | Quando | Cosa fa il frontend |
|-------------|--------|---------------------|
| `200` / `201` | Tutto bene (`201` = creato) | Mostra il risultato |
| `204` | Tutto bene, nessun dato in risposta | Mostra una conferma |
| `400` | Dati sbagliati (campo vuoto, password troppo corta, mazzo non valido) | Mostra `fieldErrors` accanto ai campi, oppure `message` |
| `401` | Token mancante o scaduto | Prova `/auth/refresh`; se fallisce, porta alla pagina di accesso |
| `403` | Accesso non permesso (es. un giocatore nel backoffice) | Mostra "non hai i permessi" |
| `404` | Non esiste (carta, mazzo, scambio...) | Pagina o messaggio "non trovato" |
| `409` | Conflitto (username già usato, crediti insufficienti, carte non più possedute) | Mostra `message` |
| `503` | Servizio momentaneamente non disponibile (es. il motore di gioco non risponde) | Mostra `message` e invita a riprovare |

Codici (`code`) che arrivano con ogni API: `VALIDATION_ERROR` (campi sbagliati, con `fieldErrors`), `MALFORMED_REQUEST` (JSON o parametri illeggibili), `UNAUTHORIZED` (token mancante o scaduto), `FORBIDDEN`, `NOT_FOUND`, `CONFLICT` (due richieste insieme sugli stessi dati: si riprova), `INTERNAL_ERROR`. I codici delle singole pagine sono scritti accanto alle loro chiamate.

---

## 3. Accesso con JWT

Il backend usa i **token JWT**. Ci sono due token:

| Token | A cosa serve | Quanto dura | Dove sta |
|-------|--------------|-------------|----------|
| **Token di accesso** (`accessToken`) | Va in **ogni** richiesta che richiede l'accesso | **15 minuti** | In una variabile JavaScript, **non** in `localStorage` |
| **Refresh token** | Serve a ottenere un nuovo token di accesso | **7 giorni** | In un **cookie `HttpOnly`** che il browser gestisce da solo: il JavaScript non lo vede e non lo tocca |

Perché non `localStorage`: se una pagina avesse un bug di sicurezza (XSS), uno script estraneo potrebbe leggere il token. Il cookie `HttpOnly` non è leggibile dal JavaScript.

Il sito è fatto di più pagine HTML, quindi la variabile si perde a ogni cambio pagina. Non è un problema: all'apertura di ogni pagina `api.js` chiama `/auth/refresh` e ottiene un token nuovo dal cookie (vedi il [capitolo 4](#4-il-file-apijs)).

### Le chiamate dell'accesso

| Azione | Chiamata | Cosa mandate | Cosa ricevete |
|--------|----------|--------------|---------------|
| Registrazione | `POST /auth/register` | `username`, `email`, `password`, `privacyAccepted`, `termsAccepted` | `201` + i dati dell'utente. Poi si fa l'accesso |
| Accesso | `POST /auth/login` | `login` (username **o** email), `password` | `accessToken` + dati dell'utente; il refresh token arriva nel cookie |
| Nuovo token di accesso | `POST /auth/refresh` | Niente: il cookie parte da solo | Un nuovo `accessToken` |
| Uscita | `POST /auth/logout` | Niente | `204`; il backend revoca il refresh token e cancella il cookie |
| Chi sono | `GET /me` | — | I dati dell'utente collegato |
| Modifica profilo | `PUT /me` | `email` | L'utente aggiornato |
| Cambio password | `PUT /me/password` | `currentPassword`, `newPassword` | `204`; per sicurezza gli altri dispositivi vengono scollegati |
| Password dimenticata (facoltativa) | `POST /auth/password-reset-requests` | `email` | Sempre `202`, anche se l'email non esiste (per non rivelare chi è registrato) |
| Nuova password (facoltativa) | `POST /auth/password-resets` | `token` (dal link dell'email), `newPassword` | `204` |

Esempio di registrazione:

```json
POST /api/v1/auth/register
{
  "username": "lucida",
  "email": "lucida@example.com",
  "password": "una-password-lunga",
  "privacyAccepted": true,
  "termsAccepted": true
}
```

Esempio di risposta all'accesso:

```json
{
  "accessToken": "eyJhbGciOi...",
  "expiresIn": 900,
  "user": { "id": "019a1f2e-7c41-7d3a-9b2e-5f6a7b8c9d01", "username": "lucida", "role": "PLAYER", "credits": 500 }
}
```

- `role` è `PLAYER` oppure `ADMIN`: con `ADMIN` si mostra il backoffice. Un solo login per tutti: l'admin entra dalla stessa pagina.
- **Regole dei campi** (il backend le ricontrolla sempre): username da 3 a 30 caratteri, lettere, numeri e trattino basso; email valida; password di almeno 8 caratteri; Privacy e Termini obbligatori.
- Username ed email sono unici **senza distinguere le maiuscole**: "Lucida" e "lucida" sono lo stesso utente.
- `/auth/refresh` risponde `{ "accessToken": "…", "expiresIn": 900 }` e manda un refresh token **nuovo** nel cookie: quello vecchio non vale più. Il cookie si chiama `dreamscape_refresh` e vale solo per `/api/v1`.
- Il primo accesso del giorno (ora italiana), con il login o con il refresh all'apertura di una pagina, dà i **50 DC** della ricompensa giornaliera: si vedono in `GET /me/credit-transactions`.
- Il link dell'email di recupero porta a `nuova-password.html?token=…` (indirizzo da confermare con chi fa la pagina): la pagina manda `token` e `newPassword` a `/auth/password-resets`. Il link vale **1 ora** e una volta sola.

Codici di errore dell'accesso:

| `code` | Stato | Quando |
|--------|-------|--------|
| `USERNAME_TAKEN`, `EMAIL_TAKEN` | 409 | Registrazione o cambio email con dati già usati (anche in `fieldErrors`) |
| `INVALID_CREDENTIALS` | 401 | Login sbagliato: stesso messaggio se manca l'utente o la password è errata |
| `INVALID_REFRESH_TOKEN` | 401 | Cookie mancante, scaduto o revocato: si va alla pagina di accesso |
| `ACCOUNT_DISABLED` | 401 | L'account è stato cancellato |
| `WRONG_PASSWORD` | 400 | Password attuale sbagliata nel cambio password o nella cancellazione dell'account |
| `INVALID_RESET_TOKEN` | 400 | Link di recupero scaduto o già usato: si chiede un link nuovo |

---

## 4. Il file api.js

Tutte le chiamate passano da un solo file, `api.js`, che aggiunge il token, rinnova quello scaduto e trasforma gli errori. Pagine e componenti non usano mai `fetch()` direttamente.

```js
// api.js: unico punto da cui passano le chiamate al backend
const API_BASE = "/api/v1"; // stesso indirizzo del sito, in locale e alla consegna

// Token di accesso solo in memoria; il refresh token sta nel cookie HttpOnly.
let accessToken = null;

async function send(path, method, body) {
  const headers = { "Content-Type": "application/json" };
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  return fetch(API_BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
    credentials: "include" // fa partire il cookie del refresh token
  });
}

export async function refresh() {
  const response = await fetch(API_BASE + "/auth/refresh", { method: "POST", credentials: "include" });
  if (!response.ok) return false;
  accessToken = (await response.json()).accessToken;
  return true;
}

export async function api(path, { method = "GET", body } = {}) {
  let response = await send(path, method, body);
  if (response.status === 401 && (await refresh())) {
    response = await send(path, method, body); // riprova una volta con il token nuovo
  }
  if (response.status === 401) {
    window.location.href = "/accesso.html";
    return null;
  }
  if (response.status === 204) return null;
  const data = await response.json();
  if (!response.ok) throw data; // { status, code, message, fieldErrors }
  return data;
}

export async function login(loginName, password) {
  const data = await api("/auth/login", { method: "POST", body: { login: loginName, password } });
  accessToken = data.accessToken;
  return data.user;
}
```

Uso in una pagina:

```js
import { api, refresh } from "./api.js";

await refresh(); // all'apertura della pagina: recupera il token dal cookie
const summary = await api("/me/summary");
document.querySelector("#credits").textContent = summary.credits;
```

Una pagina che usa un componente React gli passa i dati arrivati da `api.js`:

```jsx
import { createRoot } from "react-dom/client";
import { api, refresh } from "./api.js";
import { Card } from "./components/Card.jsx";

await refresh();
const openingId = window.location.pathname.split("/").pop(); // /negozio/apertura/{openingId}
const opening = await api(`/pack-openings/${openingId}`);
createRoot(document.querySelector("#opened-cards")).render(
  opening.cards.map((item, index) => <Card key={index} card={item.card} />)
);
```

---

## 5. Gli oggetti che ricevete

Questi oggetti tornano in più pagine: conviene scrivere una sola funzione JavaScript per mostrarli.

### Carta

```json
{
  "id": "019a1f30-0000-7000-8000-000000000004",
  "code": "BASE-004",
  "baseCardId": null,
  "variantType": null,
  "name": "Il Sogno Lucido",
  "faction": { "code": "DREAMS", "name": "Dreams" },
  "rarity": { "code": "LEGENDARY", "name": "Leggendaria" },
  "attack": 5,
  "defense": 7,
  "description": "Sai di sognare, e il sogno ti obbedisce.",
  "effect": {
    "code": "NEUTRALITY",
    "name": "Neutralità",
    "value": null,
    "text": "Questa carta non subisce il vantaggio di fazione dell'avversario."
  },
  "illustrationUrl": "/images/cards/base-004-v1.webp",
  "illustrationThumbUrl": "/images/cards/base-004-v1-thumb.webp",
  "illustrationFocusY": 0.27,
  "lordTitle": "Dreams Lord"
}
```

- `effect` è `null` se la carta non ha effetti; `value` è il numero X dell'effetto (es. `Castigo 2`), `null` se l'effetto non ha numero.
- `illustrationUrl` è la copia **grande** (larghezza 1024 px), `illustrationThumbUrl` la **miniatura** (larghezza 400 px): quale usare è spiegato nel [capitolo 7](#7-come-si-monta-una-carta).
- `illustrationFocusY` dice quale fascia dell'immagine resta visibile nel ritaglio: 0 = in alto, 1 = in basso (quasi sempre 0,40).
- **Full art:** è una carta a parte, con il suo `id` e il suo codice (`BASE-004-FA`): `variantType` è `"FULL_ART"` e `baseCardId` è l'id della carta normale. Nome, testi, ATT, DIF, fazione, rarità ed effetto sono quelli della carta normale (il backend li manda già compilati); cambiano solo `id`, `code` e l'illustrazione, verticale 2:3. Oggi solo le Leggendarie hanno la full art. Domani le skin arriveranno nello stesso modo, con un altro `variantType`.
- `lordTitle` c'è solo per le tre carte Lord.
- Codici delle rarità: `COMMON`, `RARE`, `EPIC`, `LEGENDARY`. Codici delle fazioni: `DREAMS`, `NIGHTMARES`, `MEMORIES`.

### Carta della collezione

```json
{ "card": { "id": "019a1f30-0000-7000-8000-000000000004", "name": "Il Sogno Lucido", "...": "..." }, "quantity": 1 }
```

Ogni versione è una voce a parte: se l'utente ha anche la full art del Sogno Lucido, nella collezione c'è un'altra voce con `card.variantType: "FULL_ART"`, il suo `card.id` e le sue copie.

### Utente (in liste, amici, sfide)

```json
{ "id": "019a1f2e-8d52-7e4b-8c3f-6a7b8c9d0e12", "username": "incubo_99" }
```

Degli altri giocatori si vede solo lo username: mai email o crediti.

---

## 6. Pagine e chiamate

Per ogni pagina: le chiamate da fare e cosa mostrare. 🔓 = pagina pubblica, 🔑 = serve l'accesso, 🛠️ = solo admin.

### Pagine pubbliche 🔓

| Pagina | Chiamate | Cosa mostra |
|--------|----------|-------------|
| Home | `GET /cards?featured=true` | Alcune carte del catalogo come anteprima |
| Il gioco / Lore | `GET /factions` | Le tre fazioni con forze, debolezze e il loro Lord |
| Regolamento | `GET /rules` | I numeri del regolamento, sempre aggiornati |
| Contatti | `POST /contact-messages` | Form: `name`, `email`, `message` → `201` e messaggio di conferma |
| Chi siamo, FAQ, Glossario, Privacy, Cookie, Termini, 404 | Nessuna | Pagine statiche |

Risposta di `GET /rules`:

```json
{
  "deckSize": 20,
  "startingLifePoints": 20,
  "roundLimit": 60,
  "rarities": [
    { "code": "COMMON", "name": "Comune", "maxPerDeck": null, "maxCopies": 2 },
    { "code": "RARE", "name": "Rara", "maxPerDeck": 8, "maxCopies": 2 },
    { "code": "EPIC", "name": "Epica", "maxPerDeck": 4, "maxCopies": 2 },
    { "code": "LEGENDARY", "name": "Leggendaria", "maxPerDeck": 2, "maxCopies": 1 }
  ],
  "factionAdvantages": [
    { "faction": "DREAMS", "strongAgainst": "MEMORIES", "attackBonus": 1 },
    { "faction": "MEMORIES", "strongAgainst": "NIGHTMARES", "attackBonus": 1 },
    { "faction": "NIGHTMARES", "strongAgainst": "DREAMS", "attackBonus": 1 }
  ]
}
```

`maxPerDeck: null` vuol dire "nessun limite".

Risposta di `GET /factions`:

```json
[
  {
    "code": "DREAMS",
    "name": "Dreams",
    "description": "Speranza, desiderio, creatività e protezione. Stile conservativo e curativo.",
    "lordTitle": "Dreams Lord",
    "lordCardId": "019a1f30-0000-7000-8000-000000000004",
    "strongAgainst": ["MEMORIES"],
    "weakAgainst": ["NIGHTMARES"]
  }
]
```

`lordCardId` è `null` finché il catalogo vero non è nel database: la carta del Lord si legge con `GET /cards/{cardId}`.

### Accesso 🔓

| Pagina | Chiamate |
|--------|----------|
| Registrazione | `POST /auth/register`, poi `POST /auth/login` e passaggio a **Benvenuto** |
| Accesso | `POST /auth/login` → Dashboard (o Backoffice se `role` è `ADMIN`) |
| Password dimenticata (facoltativa) | `POST /auth/password-reset-requests` |
| Nuova password (facoltativa, dal link dell'email) | `POST /auth/password-resets` |
| Esci (nel menu) | `POST /auth/logout` → Home |

### Area del Dreamweaver 🔑

#### Benvenuto (dopo la registrazione)

Il set iniziale è di **una sola fazione, scelta dal Dreamweaver** in questa pagina (decisione 15 del 2/11). I 500 DC arrivano già alla registrazione.

| Momento | Chiamata | Cosa succede |
|---------|----------|--------------|
| Apertura della pagina | `GET /me/welcome` | Se `starterFaction` è `null`, la fazione non è ancora scelta: si mostrano le tre fazioni con il loro Lord |
| Scelta della fazione | `POST /me/welcome` con `{ "starterFaction": "DREAMS" }` | Il backend dà il set iniziale di quella fazione e apre i due pacchetti di benvenuto. Risponde `201` con lo stesso oggetto di `GET /me/welcome`. La scelta si fa una volta sola: la seconda volta arriva `409` con `code: "WELCOME_ALREADY_CLAIMED"` |

```json
{
  "starterFaction": "DREAMS",
  "starterCards": [ { "card": { "...": "..." } } ],
  "welcomeOpenings": [
    { "openingId": "019a2b40-0000-7000-8000-000000000101", "packName": "Benvenuto", "cards": [ { "card": { "...": "..." } } ] },
    { "openingId": "019a2b40-0000-7000-8000-000000000102", "packName": "Benvenuto con Epica", "cards": [ { "card": { "...": "..." } } ] }
  ]
}
```

Il set iniziale ha 20 carte della fazione scelta (14 Comuni e 6 Rare), estratte senza mai superare le copie massime: con queste 20 carte si compone sempre un mazzo valido. Il mazzo **non** viene creato in automatico: dalla pagina Benvenuto conviene un pulsante "Componi il tuo primo mazzo" che apre l'editor. Le carte si possono rivelare una alla volta come in un'apertura.

- Se la fazione non è ancora scelta, `GET /me/welcome` risponde con `starterFaction: null` e le due liste vuote.
- Le carte arrivano subito nella collezione, gratis: i regali non toccano i crediti.
- Errori: fazione che non esiste → `400` con `code: "INVALID_FACTION"` e il campo `starterFaction` in `fieldErrors`; fazione vuota → `400` con `VALIDATION_ERROR`.

#### Dashboard

`GET /me/summary`

```json
{
  "username": "lucida",
  "credits": 750,
  "rating": 1040,
  "league": { "code": "DORMANT", "name": "Dormiente", "nextLeagueName": "Sognatore", "nextLeagueMinRating": 1100 },
  "hasRankedDeck": true,
  "missions": [
    { "code": "DAILY_PLAY", "name": "Missione giornaliera", "description": "Gioca una partita.", "progress": 0, "goalCount": 1, "rewardCredits": 100, "isCompleted": false, "resetsAt": "2026-11-03T23:00:00Z" },
    { "code": "WEEKLY_WIN", "name": "Obiettivo settimanale", "description": "Vinci 5 partite.", "progress": 2, "goalCount": 5, "rewardCredits": 250, "isCompleted": false, "resetsAt": "2026-11-08T23:00:00Z" }
  ],
  "pendingFriendRequests": 1,
  "pendingTrades": 2,
  "pendingChallenges": 0,
  "latestCards": [ { "card": { "...": "..." } } ]
}
```

- `pendingFriendRequests`, `pendingTrades` e `pendingChallenges` servono per il contatore nel menu: non ci sono altre notifiche.
- `resetsAt` è in UTC: la missione giornaliera si azzera a mezzanotte, ora italiana, l'obiettivo settimanale il lunedì a mezzanotte.
- Se `hasRankedDeck` è `false`, conviene un invito a scegliere il mazzo classificato: senza, il giocatore non può essere scelto come avversario.

#### Profilo

| Azione | Chiamata |
|--------|----------|
| Vedere i dati | `GET /me` → `username`, `email`, `createdAt` |
| Cambiare email | `PUT /me` |
| Cambiare password | `PUT /me/password` |
| Cancellare l'account | `DELETE /me` con `{ "password": "…" }` → `204` |

Lo username non si cambia (è il nome visibile agli amici). La cancellazione rende anonimo l'account: username ed email spariscono, lo storico di partite e scambi resta per gli altri giocatori. Chiedere conferma prima di mandarla.

#### Crediti

`GET /me/credit-transactions?page=0&size=20`

```json
{
  "items": [
    { "type": "PACK_PURCHASE", "amount": -500, "balanceAfter": 50, "createdAt": "2026-10-26T15:02:00Z" },
    { "type": "DAILY_REWARD", "amount": 50, "balanceAfter": 550, "createdAt": "2026-10-26T09:00:00Z" }
  ],
  "page": 0, "size": 20, "totalItems": 2, "totalPages": 1
}
```

Tipi possibili: `SIGNUP_BONUS`, `DAILY_REWARD`, `PACK_PURCHASE`, `MATCH_REWARD`, `MISSION_REWARD`, `LEAGUE_REWARD`, `ADMIN_ADJUSTMENT` (il frontend li traduce in italiano). Il riquadro "Acquista crediti" con denaro vero va mostrato solo come "in arrivo": in fase 1 non c'è.

#### Negozio e apertura dei pacchetti

`GET /packs` → i pacchetti in vendita:

```json
[
  {
    "id": "019a1f31-0000-7000-8000-000000000001",
    "name": "Dream Pack",
    "description": "5 carte di tutte le fazioni.",
    "priceCredits": 500,
    "fullArtChance": 20.0,
    "factionCode": null,
    "availableUntil": null,
    "covers": [
      { "id": "019a1f31-0000-7000-8000-0000000000c1", "name": "Dreams Lord", "factionCode": "DREAMS", "imageUrl": "/images/packs/base-dreams-v1.webp" },
      { "id": "019a1f31-0000-7000-8000-0000000000c2", "name": "Nightmares Lord", "factionCode": "NIGHTMARES", "imageUrl": "/images/packs/base-nightmares-v1.webp" },
      { "id": "019a1f31-0000-7000-8000-0000000000c3", "name": "Memories Lord", "factionCode": "MEMORIES", "imageUrl": "/images/packs/base-memories-v1.webp" }
    ],
    "slots": [
      { "cardCount": 3, "rarities": [ { "code": "COMMON", "chance": 100.0 } ] },
      { "cardCount": 1, "rarities": [ { "code": "RARE", "chance": 100.0 } ] },
      { "cardCount": 1, "rarities": [ { "code": "RARE", "chance": 70.0 }, { "code": "EPIC", "chance": 25.0 }, { "code": "LEGENDARY", "chance": 5.0 } ] }
    ]
  }
]
```

- Arrivano solo i pacchetti **in vendita in questo momento**, dal più economico. Set iniziale e pacchetti di benvenuto non ci sono: si ricevono nella pagina Benvenuto.
- Il giocatore sceglie una delle copertine: il contenuto è lo stesso. `covers` può essere vuoto (pacchetto senza copertine, o copertine non ancora pronte): in quel caso si mostra un'immagine generica del pacchetto.
- `slots` serve a mostrare "3 Comuni, 1 Rara, 1 carta speciale: Rara 70%, Epica 25%, Leggendaria 5% (di cui full art 1%)". Le rarità di uno slot arrivano dalla più comune alla più rara; `chance` è già in percentuale.
- `fullArtChance` è la percentuale delle **Leggendarie** che escono in full art, non del pacchetto: con 5% di Leggendaria e 20 di `fullArtChance` la full art esce nell'1% dei pacchetti (5 × 20 / 100).
- Oggi c'è solo il Dream Pack, ma il negozio va fatto per **più pacchetti**: il cliente ne vuole di più tipi, a prezzi diversi. `factionCode` dice se il pacchetto ha carte di una sola fazione (`null` = tutte); `availableUntil` (UTC) se è in vendita solo fino a una data (`null` = nessun limite).

Acquisto: `POST /packs/{packId}/open` con `{ "coverId": "019a1f31-0000-7000-8000-0000000000c2" }` (il corpo si può omettere se il pacchetto non ha copertine). Risponde `201`:

```json
{
  "openingId": "019a2b44-5c6d-7e8f-9a0b-1c2d3e4f5a6b",
  "creditsAfter": 250,
  "cards": [
    { "card": { "...": "..." } },
    { "card": { "code": "BASE-008-FA", "variantType": "FULL_ART", "...": "..." } }
  ]
}
```

Poi si va su `/negozio/apertura/{openingId}`, che legge `GET /pack-openings/{openingId}`. Con l'**id dell'apertura** nell'URL, ricaricare la pagina non compra un altro pacchetto.

| Errore | `code` | Quando |
|--------|--------|--------|
| `409` | `NOT_ENOUGH_CREDITS` | Il saldo non basta: non si estrae niente e i crediti non cambiano |
| `409` | `PACK_NOT_ON_SALE` | Il pacchetto non è più in vendita (spento o scaduto): conviene ricaricare `GET /packs` |
| `404` | `PACK_NOT_FOUND` | Il pacchetto non esiste o non si compra (set iniziale, benvenuto) |
| `400` | `INVALID_COVER` | La copertina non è di quel pacchetto o non è più attiva (`fieldErrors` sul campo `coverId`) |

`GET /pack-openings/{openingId}` → l'apertura, con le carte **nell'ordine in cui sono uscite** (l'ultima è quella dello slot speciale):

```json
{
  "openingId": "019a2b44-5c6d-7e8f-9a0b-1c2d3e4f5a6b",
  "packName": "Dream Pack",
  "openedAt": "2026-11-11T10:15:00Z",
  "cards": [
    { "card": { "code": "BASE-021", "...": "..." } },
    { "card": { "code": "BASE-008-FA", "variantType": "FULL_ART", "...": "..." } }
  ]
}
```

- Ogni `card` è l'oggetto [Carta](#carta). Una full art arriva già con nome, statistiche ed effetto della carta normale.
- L'apertura di un altro giocatore, o un id che non esiste, dà `404` con `code: "PACK_OPENING_NOT_FOUND"`: la pagina mostra "Apertura non trovata" e un link al negozio. Un id scritto male dà `400` con `MALFORMED_REQUEST`.

#### Collezione e dettaglio carta

| Azione | Chiamata |
|--------|----------|
| Collezione con filtri | `GET /me/cards?faction=DREAMS&rarity=RARE&search=sogno&fullArt=true&page=0&size=24` |
| Dettaglio di una carta | `GET /cards/{cardId}` (dati della carta) + la riga della collezione (copie possedute) |

Il dettaglio mostra anche contro chi la fazione è forte o debole (da `GET /rules`) e le altre versioni della carta che l'utente possiede (es. la full art), con le loro copie.

#### Mazzi

| Azione | Chiamata |
|--------|----------|
| I miei mazzi | `GET /decks` |
| Un mazzo | `GET /decks/{deckId}` |
| Nuovo mazzo | `POST /decks` |
| Modifica | `PUT /decks/{deckId}` |
| Elimina | `DELETE /decks/{deckId}` |
| Scegli il mazzo classificato | `PUT /me/ranked-deck` con `{ "deckId": "…" }` |

Cosa si manda al salvataggio:

```json
{ "name": "Incubi veloci", "cards": [ { "cardId": "019a1f30-0000-7000-8000-000000000005", "quantity": 2 }, { "cardId": "019a1f30-0000-7000-8000-000000000108", "quantity": 1 } ] }
```

Cosa si riceve:

```json
{
  "id": "019a2a10-1b2c-7d3e-8f40-a1b2c3d4e5f6",
  "name": "Incubi veloci",
  "totalCards": 20,
  "isValid": true,
  "problems": [],
  "cards": [ { "card": { "...": "..." }, "quantity": 2 } ]
}
```

- **Full art nel mazzo:** la full art è una carta a parte (nell'esempio sopra `…0108` è la full art di `BASE-008`). Nell'editor è una voce della collezione come le altre: il giocatore mette nel mazzo la versione che vuole mostrare, e quella si vede nella lista dei mazzi e nel replay. Normale e full art della stessa carta **contano insieme** per le copie massime: una Leggendaria al massimo 1, normale o full art. Se ne mette tutte e due, il salvataggio risponde `400` con il problema.

- Se il mazzo non rispetta le regole il salvataggio risponde `400`, e `problems` dice cosa non va, es. `"Le Rare sono 9: il massimo è 8."`.
- L'editor mostra i contatori in diretta (carte totali, copie, limiti per rarità) con i numeri di `GET /rules`, così l'utente vede l'errore prima di salvare. Il backend controlla comunque.
- Si salvano **solo i mazzi validi**: niente bozze.
- Un mazzo salvato può diventare non valido dopo uno scambio o se l'admin **disattiva** una carta che contiene: `isValid: false` e `problems` lo dicono nella lista dei mazzi (es. `"La carta Il Faro nella Nebbia non è più disponibile."`). Conviene mostrarlo bene in vista: è l'avviso al giocatore.
- Il **mazzo classificato** (`isRanked: true` nella lista) è quello con cui si gioca quando il matchmaking ti sceglie come avversario, anche se non sei collegato. Se diventa non valido, nessuno ti sceglie finché non lo sistemi: conviene segnalarlo nella lista dei mazzi.

#### Amici (pagina nuova)

| Azione | Chiamata |
|--------|----------|
| I miei amici | `GET /friends` |
| Cerca un Dreamweaver | `GET /users?search=luc` (solo username) |
| Manda una richiesta | `POST /friend-requests` con `{ "username": "lucida" }` |
| Richieste ricevute / inviate | `GET /friend-requests?direction=received` / `?direction=sent` |
| Accetta | `POST /friend-requests/{requestId}/accept` |
| Rifiuta (ricevuta) o annulla (inviata) | `DELETE /friend-requests/{requestId}` |
| Togli un amico | `DELETE /friends/{userId}`: annulla anche gli scambi e le sfide in attesa tra voi due |

```json
GET /api/v1/friends
[ { "user": { "id": "019a1f2e-8d52-7e4b-8c3f-6a7b8c9d0e12", "username": "incubo_99" }, "since": "2026-10-20T18:11:00Z" } ]
```

#### Scambi

Si scambia **solo con gli amici**. Quantità e rarità sono libere (es. 1 Epica per 5 Comuni): decide l'amico accettando o rifiutando.

| Azione | Chiamata |
|--------|----------|
| Ricevuti / Inviati / Storico | `GET /trades?box=received`, `?box=sent`, `?box=history` |
| Dettaglio | `GET /trades/{tradeId}` |
| Proponi | `POST /trades` |
| Accetta / Rifiuta (destinatario) | `POST /trades/{tradeId}/accept`, `POST /trades/{tradeId}/reject` |
| Annulla (chi propone) | `POST /trades/{tradeId}/cancel` |

Proposta di scambio:

```json
{
  "recipientId": "019a1f2e-8d52-7e4b-8c3f-6a7b8c9d0e12",
  "message": "Ti va?",
  "offered":   [ { "cardId": "019a1f30-0000-7000-8000-000000000007", "quantity": 1 } ],
  "requested": [ { "cardId": "019a1f30-0000-7000-8000-000000000001", "quantity": 5 } ]
}
```

- `offered` = carte che do io; `requested` = carte che chiedo all'amico. Almeno una carta per lato. La full art si offre o si chiede con il suo `cardId`, come ogni altra carta.
- Nella pagina "Nuovo scambio" il destinatario si sceglie **dalla lista amici** e le carte dalla propria collezione e da quella dell'amico (`GET /users/{userId}/cards`, visibile solo agli amici).
- Stati di uno scambio: `PENDING` (in attesa), `ACCEPTED`, `REJECTED`, `CANCELLED`.
- **Copie bloccate:** le copie che offri in uno scambio in attesa non le puoi offrire in un altro scambio finché il primo non si chiude (accettato, rifiutato o annullato). Restano usabili nei mazzi. Nella collezione per gli scambi conviene mostrare le copie disponibili (`availableForTrade`, come `quantity` ma senza quelle già offerte). Se offri più copie di quelle disponibili arriva un `409` con `code: "CARDS_LOCKED_IN_TRADE"`.
- **Avviso sui mazzi:** prima di proporre o accettare, `POST /trades/preview` (stesso corpo della proposta) oppure `GET /trades/{tradeId}/deck-warnings` dicono quali mazzi cambiano:

  ```json
  { "deckWarnings": [
      { "deckId": "019a2a10-1b2c-7d3e-8f40-a1b2c3d4e5f6", "deckName": "Incubi veloci", "isRanked": true, "cardName": "Il Faro nella Nebbia", "outcome": "INVALID" },
      { "deckId": "019a2a10-1b2c-7d3e-8f40-a1b2c3d4e5f7", "deckName": "Lord e amici", "isRanked": false, "cardName": "L'Uomo Senza Volto", "outcome": "NORMAL_VERSION" }
  ] }
  ```

  `INVALID`: si mostra "Il mazzo Incubi veloci diventerà non valido (è il tuo mazzo classificato)". `NORMAL_VERSION`: cedi una full art che usi nel mazzo, ma hai una copia della versione normale, quindi si mostra "Il mazzo Lord e amici userà la versione normale di L'Uomo Senza Volto". Se la versione normale non ce l'hai, l'avviso è `INVALID`. In tutti i casi si chiede conferma; lo scambio si può fare lo stesso e un mazzo non valido va poi sistemato nell'editor.
- Se all'accettazione qualcuno non ha più le carte, arriva un `409` e nessuna carta si muove.

#### Partite: classificata, rapida e sfide tra amici

Tutte le partite hanno le regole della Base e le gioca il server subito: non c'è niente da aspettare.

| Tipo | Con chi | Rank | Dream Credits |
|------|---------|------|---------------|
| **Classificata** | Un avversario con punteggio simile, scelto dal matchmaking | Sì | 30 DC per vittoria, massimo 3 al giorno. Se il matchmaking sceglie un amico: rank sì, DC no |
| **Rapida** | Un avversario a caso, scelto dal matchmaking | No | No |
| **Sfida a un amico** | Un amico, che deve accettare (è una partita rapida) | No | No |

L'avversario scelto dal matchmaking **non deve essere collegato**: si gioca con il suo mazzo classificato. Per questo ogni giocatore sceglie un mazzo classificato nella pagina Mazzi.

| Azione | Chiamata |
|--------|----------|
| Cerca una partita classificata | `POST /matches/ranked` con `{ "deckId": "…" }` → il risultato della partita |
| Cerca una partita rapida | `POST /matches/casual` con `{ "deckId": "…" }` → il risultato della partita |
| Storico | `GET /me/matches?type=RANKED&page=0&size=20` (`type` facoltativo: `RANKED`, `CASUAL`) |
| Risultato | `GET /matches/{matchId}` |
| Sfide tra amici ricevute / inviate | `GET /challenges?box=received`, `?box=sent` |
| Sfida un amico | `POST /challenges` con `{ "opponentId": "…", "deckId": "…" }` (solo amici, solo mazzi validi) |
| Accetta e gioca | `POST /challenges/{challengeId}/accept` con `{ "deckId": "…" }` → `{ "matchId": "…" }` |
| Rifiuta / Annulla | `POST /challenges/{challengeId}/reject`, `POST /challenges/{challengeId}/cancel` |

- Se nessun avversario è disponibile, `POST /matches/...` risponde `409` con `code: "NO_OPPONENT_FOUND"`: si mostra "Nessun avversario disponibile, riprova tra poco".
- Se il motore di gioco non risponde, `POST /matches/...` e `POST /challenges/{challengeId}/accept` rispondono `503` con `code: "ENGINE_UNAVAILABLE"`: la partita non si gioca, niente crediti né punti, la sfida resta in attesa. Si mostra "Partita non disponibile, riprova tra poco".
- Tra due amici c'è al massimo una sfida in attesa: la seconda risponde `409`. Le sfide non scadono: chi sfida può annullarla.
- Dopo la partita si va su `/partite/{matchId}`, che legge `GET /matches/{matchId}`: ricaricare la pagina non fa giocare un'altra partita.

Il risultato:

```json
{
  "id": "019a2c55-6d7e-7f80-9a1b-2c3d4e5f6a7b",
  "matchType": "RANKED",
  "challenger": { "id": "019a1f2e-7c41-7d3a-9b2e-5f6a7b8c9d01", "username": "lucida" },
  "opponent": { "id": "019a1f2e-8d52-7e4b-8c3f-6a7b8c9d0e12", "username": "incubo_99" },
  "winnerId": "019a1f2e-8d52-7e4b-8c3f-6a7b8c9d0e12",
  "ratingChange": { "challenger": -16, "opponent": 16 },
  "creditsEarned": 0,
  "leagueReached": null,
  "endReason": "LIFE_POINTS",
  "totalRounds": 24,
  "startingLifePoints": 20,
  "finalLifePoints": { "challenger": 0, "opponent": 6 },
  "coinTossWinner": "OPPONENT",
  "playedAt": "2026-10-26T16:40:00Z",
  "engineVersion": "1.0.0",
  "rounds": [
    {
      "round": 3,
      "clash": 2,
      "events": [
        { "phase": "START", "type": "CARD_PLAYED", "side": "CHALLENGER", "cardId": "019a1f30-0000-7000-8000-000000000003", "attack": 4, "defense": 7 },
        { "phase": "START", "type": "CARD_PLAYED", "side": "OPPONENT", "cardId": "019a1f30-0000-7000-8000-000000000008", "attack": 8, "defense": 5 },
        { "phase": "COMBAT", "type": "EFFECT", "side": "OPPONENT", "cardId": "019a1f30-0000-7000-8000-000000000008", "code": "WEAKENING", "value": 2,
          "targetSide": "CHALLENGER", "target": "CARD", "stat": "ATTACK", "before": 4, "after": 2, "untilEndOfRound": true },
        { "phase": "COMBAT", "type": "ATTACK", "side": "OPPONENT", "cardId": "019a1f30-0000-7000-8000-000000000008",
          "attack": 8, "factionBonus": 1, "attackModifier": 0, "damage": 9, "defenseBefore": 7, "defenseAfter": -2 },
        { "phase": "COMBAT", "type": "ATTACK", "side": "CHALLENGER", "cardId": "019a1f30-0000-7000-8000-000000000003",
          "attack": 4, "factionBonus": 0, "attackModifier": -2, "damage": 2, "defenseBefore": 5, "defenseAfter": 3 },
        { "phase": "END", "type": "CARD_DIED", "side": "CHALLENGER", "cardId": "019a1f30-0000-7000-8000-000000000003" },
        { "phase": "END", "type": "PLAYER_DAMAGE", "side": "CHALLENGER", "amount": 2, "lifePointsBefore": 20, "lifePointsAfter": 18 }
      ],
      "outcome": "CHALLENGER_CARD_DIED",
      "afterRound": {
        "challenger": { "cardId": null, "attack": null, "defense": null, "lifePoints": 18, "cardsInDeck": 18, "cardsInDiscard": 2 },
        "opponent":   { "cardId": "019a1f30-0000-7000-8000-000000000008", "attack": 8, "defense": 3, "lifePoints": 20, "cardsInDeck": 18, "cardsInDiscard": 1 }
      }
    }
  ],
  "decks": { "challenger": { "...": "..." }, "opponent": { "...": "..." } }
}
```

- `winnerId: null` = pareggio.
- `challenger` è chi ha avviato la partita, `opponent` l'avversario.
- `ratingChange` è `null` nelle partite rapide, e nelle classificate oltre la terza del giorno contro lo stesso avversario. `creditsEarned` sono i DC vinti da chi guarda. `leagueReached` dice se con questa partita si entra per la prima volta in una lega (`{ "name": "Sognatore", "rewardCredits": … }`): è il momento per un'animazione.
- `endReason`: `LIFE_POINTS` (punti vita a 0), `DECK_EXHAUSTED` (mazzo finito), `ROUND_LIMIT` (60 round, pareggio).
- `outcome`: `NONE_DIED`, `CHALLENGER_CARD_DIED`, `OPPONENT_CARD_DIED`, `BOTH_DIED`.
- **Replay:** ogni round ha `events`, gli eventi **in ordine**: carta giocata, effetti, i due colpi, morti, danno al giocatore, fine della partita. Il replay li mostra uno alla volta. `afterRound` è lo stato dei due lati a fine round (carta in campo, attacco, difesa, punti vita, carte nel mazzo e negli scarti): serve per saltare a un round. L'elenco dei tipi di evento e dei loro campi è in [schema.md](../database/schema.md#formato-dei-campi-json-di-matches).
- **Frasi della cronaca:** il log ha solo dati, niente testi. La frase la compone la pagina: nomi delle carte e degli effetti da `decks`, valori dall'evento. Esempio: dall'evento `EFFECT` con `code: "WEAKENING"`, `value: 2`, `before: 4`, `after: 2` → "Indebolimento 2: l'attacco del Faro nella Nebbia scende da 4 a 2".
- Un **round** è un singolo scambio di colpi; uno **scontro** (`clash`) è la serie di round tra le stesse due creature. Nella cronaca si usa "round", non "turno".
- `decks` contiene la copia dei due mazzi com'erano al momento della partita: serve per mostrare le carte anche se poi i mazzi sono cambiati.
- **Storico:** `/storico`, con un filtro per tipo di partita.

#### Classifica

`GET /leagues` → le leghe con il punteggio minimo e il premio. La lega del giocatore e il suo punteggio sono in `GET /me/summary`. Una classifica con gli altri giocatori (solo username e punteggio) si aggiunge se c'è tempo.

### Backoffice 🛠️ (solo `role: ADMIN`)

| Pagina | Chiamate | Note |
|--------|----------|------|
| Dashboard admin | `GET /admin/stats` | Numeri di utenti, carte, pacchetti, partite |
| Carte | `GET/POST /admin/cards`, `PUT /admin/cards/{cardId}` | Campi: codice, nome, descrizione, ATT, DIF, fazione, rarità, espansione, effetto e valore (solo per gli effetti con la X, `hasValue`), illustrazione 3:2 e ritaglio, attiva. La full art si crea come versione della carta (codice `-FA`, solo illustrazione 2:3 e ritaglio; oggi solo per le Leggendarie). ATT e DIF si possono cambiare anche se la carta è già posseduta e valgono anche per la full art. Disattivare una carta rende non validi i mazzi che la usano: conviene chiedere conferma dicendo quanti mazzi tocca |
| Caricamento illustrazioni | `POST /admin/images` (form `multipart/form-data` con il file, il codice della carta e il tipo: illustrazione, full art o copertina) → `{ "url": "/images/cards/base-013-v1.webp", "thumbUrl": "/images/cards/base-013-v1-thumb.webp" }` | Si carica il PNG originale: il backend lo conserva intatto e crea da solo le copie WebP. Una nuova immagine per la stessa carta diventa `-v2`. Le copertine hanno solo `url` |
| Pacchetti | `GET/POST /admin/packs`, `PUT /admin/packs/{packId}` | Tipo (negozio, set iniziale, benvenuto), prezzo, fazione (facoltativa), periodo di vendita (facoltativo), slot con rarità e probabilità (per ogni slot sommano a 100), copertine, probabilità full art, attivo |
| Configurazione | `GET/PUT /admin/factions`, `/admin/rarities`, `/admin/game-modes`, `/admin/missions`, `/admin/leagues` | Vantaggi e Lord delle fazioni, limiti delle rarità, punti vita e limite di round, premi delle missioni, soglie e premi delle leghe. Le carte per mazzo (20) sono in sola lettura |
| Messaggi | `GET /admin/contact-messages`, `PUT /admin/contact-messages/{messageId}` | Messaggi del form Contatti: letto, archiviato |

Le carte e i pacchetti non si cancellano: si disattivano (`isActive: false`).

---

## 7. Come si monta una carta

La carta la **monta il frontend** con un solo componente React (`Card`, #81): il backend manda i dati e l'illustrazione, non l'immagine finita. Così, se cambia una statistica, la carta si aggiorna da sola.

Ingredienti:

| Ingrediente | Da dove arriva |
|-------------|----------------|
| Illustrazione (`illustrationUrl` o `illustrationThumbUrl`) e ritaglio verticale (`illustrationFocusY`, da 0 = in alto a 1 = in basso) | Backend, per ogni carta |
| Cornice della fazione e della rarità, con la finestra trasparente | File del frontend: 12 cornici (3 fazioni × 4 rarità), anche queste in WebP grande e miniatura |
| Simbolo della fazione: luna (Dreams), occhio (Nightmares), clessidra (Memories) | File del frontend |
| Coordinate di nome, simbolo, testi, ATT e DIF sulla cornice | File JavaScript del frontend (es. `cardZones.js`) |
| Nome, descrizione, effetto, ATT, DIF | Backend, per ogni carta |

Il componente mette i livelli uno sopra l'altro, in elementi HTML e CSS (proporzioni 2:3, come la carta grande di 1024 × 1536):

1. Illustrazione, ritagliata in verticale con `illustrationFocusY`, senza zoom.
2. Cornice sopra (la finestra trasparente lascia vedere l'illustrazione).
3. Simbolo della fazione, nome, descrizione ed effetto, ATT e DIF nelle loro coordinate.
4. Full art (`variantType: "FULL_ART"`): l'illustrazione copre tutta la carta, senza cornice, e i testi stanno su pannelli scuri semitrasparenti.

```jsx
// ✅ Una sola Card per tutte le pagine: riceve i dati dal backend
<Card card={item.card} size="thumb" />
```

**Cornici e coordinate le prepara il frontend in JavaScript** (decisione 21 del 2/11): Web parte dalle 12 cornici di `carte/cornici/`, ne ricava le copie con la finestra trasparente e misura le coordinate delle zone. Il compositore Python non le prepara.

Il riferimento visivo sono le 15 carte del set di prova in `carte/carte-finali/`. Per l'accessibilità, ogni carta ha un testo alternativo con nome, fazione, rarità, attacco e difesa.

### Il ritaglio

L'immagine si adatta alla finestra della cornice senza deformarsi e senza zoom; quello che avanza si taglia. `illustrationFocusY` dice quanto tagliare sopra e quanto sotto. Esempio: avanzano 400 px in altezza e il focus è 0,40, quindi si tagliano 160 px sopra e 240 px sotto.

Nel componente:

```jsx
<img
  src={card.illustrationThumbUrl}
  alt=""
  style={{ objectFit: "cover", objectPosition: `50% ${card.illustrationFocusY * 100}%` }}
/>
```

### Grande o miniatura

| Dove | Copia | Peso indicativo |
|------|-------|-----------------|
| Griglie e liste: collezione, editor del mazzo, scambi, dashboard, campo di partita | **Miniatura** (`illustrationThumbUrl`, cornice piccola) | 25–80 KB l'illustrazione, 20–35 KB la cornice |
| Carta da sola in grande: dettaglio carta, apertura del pacchetto, Lord nella pagina della fazione | **Grande** (`illustrationUrl`, cornice grande) | 120–350 KB l'illustrazione, 75–140 KB la cornice |

Con le miniature la prima visita alla collezione pesa meno di 1 MB. Con i PNG originali sarebbero circa 55 MB.

Le coordinate delle zone (es. `cardZones.js`) si scrivono una volta per la carta grande (1024 × 1536). Per la miniatura si moltiplicano per 400 / 1024, oppure si scrivono in percentuale e valgono per tutte e due.

### Cache: cosa resta nel browser

| Cosa | Resta in cache? | Perché |
|------|-----------------|--------|
| Illustrazioni e copertine (`/images/...`) | **Sì, per sempre** | Il backend le manda con `Cache-Control: public, max-age=31536000, immutable`. Se un'immagine cambia, il nome cambia (`-v1` → `-v2`), quindi non si vede mai una versione vecchia |
| Cornici, simboli e font del frontend | **Sì**, se anche voi mettete la versione nel nome (es. `cornice-dreams-comune-v1.webp`) e configurate lo stesso `Cache-Control` | Stesso motivo |
| Pagine HTML | Controllo a ogni visita | Così un aggiornamento del sito arriva subito |
| `api.js`, componenti e CSS | **Sì, per sempre**, se lo strumento mette l'impronta nel nome (Vite lo fa da solo: `collezione-4f2a9c.js`). Altrimenti come le pagine | Ogni nuova versione del sito ha nomi nuovi |
| Risposte delle API (JSON) | **No** | Contengono dati personali e cambiano spesso. Spring Security manda già `no-store` |
| Carta montata | **No, non serve** | Montarla costa pochi millisecondi. A ogni pagina si rimonta con le immagini già in cache, ed è sempre aggiornata se cambiano statistiche o testi |

In una prima sessione di un nuovo utente si montano circa 150–200 carte: benvenuto ~30, collezione ~30, editor del mazzo ~50, un pacchetto 5, una partita fino a 40. In tutto meno di un secondo. Ogni immagine si scarica solo la prima volta.

Se nella stessa pagina la stessa carta compare più volte (es. due copie nel mazzo), basta usare `Card` due volte: le immagini arrivano dalla cache e non si scaricano di nuovo.

---

## 8. Elenco delle pagine per la sitemap

Le pagine che il database permette, già con le decisioni di questi giorni.

| Area | Pagine |
|------|--------|
| 🔓 Pubblica | Home · Il gioco / Lore · Regolamento · Chi siamo · Contatti · FAQ · Glossario · Privacy Policy · Cookie · Termini di servizio · 404 |
| 🔓 Accesso | Registrazione · Accesso · Password dimenticata (facoltativa) · Nuova password (facoltativa) |
| 🔑 Dreamweaver | Benvenuto (con la scelta della fazione) · Dashboard (con missioni e lega) · Profilo (con cambio email e password e cancellazione dell'account) · Crediti · Negozio · Apertura pacchetto · Collezione · Dettaglio carta · Mazzi · Editor mazzo · **Amici** · Scambi · Nuovo scambio · Dettaglio scambio · **Gioca** (classificata e rapida) · Sfide tra amici · Nuova sfida · Accetta sfida · Risultato partita · Storico · **Classifica** |
| 🛠️ Backoffice | Dashboard admin · Carte · Pacchetti · Configurazione · Messaggi |

Rispetto alla prima sitemap: nuove **Benvenuto**, **Amici**, **Nuova password**, **Messaggi**, **Gioca** e **Classifica**; tolto l'accesso admin separato (si entra dalla stessa pagina); le sfide si mandano solo agli amici; nessuna scelta della modalità (le regole sono sempre quelle della Base, cambia solo il tipo di partita). Il SiteMap lo aggiorna Alessandro.

---

## 9. Regole da ricordare

- **Il server decide**: estrazione dei pacchetti, esito delle partite, validità dei mazzi, scambi. Il frontend non calcola niente di tutto questo.
- **I controlli si fanno due volte**: nel frontend per aiutare l'utente, nel backend per sicurezza.
- **Mai token in `localStorage`**: solo in memoria, come in `api.js`.
- **Miniature nelle griglie, copia grande solo per la carta da sola**: le immagini restano leggere anche da telefono.
- **Mai dati personali negli URL** (es. email nella barra degli indirizzi).
- **Pagine dell'area utente e del backoffice in `noindex`**; `robots.txt` e `sitemap.xml` solo con le pagine pubbliche.
- **Responsive**: ATT, DIF, fazione e rarità leggibili anche su mobile.
- **Acquisto crediti con denaro reale**: solo "in arrivo", non si sviluppa in fase 1.

---

## 10. Cose ancora da decidere

| Decisione | Proposta | Chi |
|-----------|----------|-----|
| Strumenti del frontend e come si caricano i componenti React nelle pagine | Vite, con più pagine HTML, in JavaScript | Gaia (#72) |
| DC a chi viene scelto dal matchmaking e vince | No: i DC della vittoria vanno solo a chi ha avviato la partita | Maie + SWD |
| Partite che contano per le missioni | Missione giornaliera: ogni partita; obiettivo settimanale: solo vittorie classificate | Maie + Nicolas |
| Soglie e premi delle leghe | Dormiente 0, Sognatore 1100, Lucido 1300, Dreamweaver 1500 | Maie |
| Tipi di pacchetti e prezzi (richiesta del cliente del 2/11) | Oggi c'è solo il Dream Pack da 500 DC. Proposta della #129: Pacchetto di fazione (6 carte, 600 DC), piccolo (3 carte, 300 DC), grande (10 carte, 1.000 DC). Sono solo righe di dati: il negozio li mostra senza cambiare il codice | Maie, Sara, Valerio |
| Collezione con le carte mancanti in ombra (album) | Sì: invoglia a completare la collezione | WMD + DGS |
