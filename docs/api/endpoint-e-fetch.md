# Endpoint e chiamate per il frontend · Dreamscape

Per il team **WMD**. Qui c'è tutto quello che serve per collegare le pagine React al backend:

- come installare i programmi, scaricare il progetto e avviarlo, con i link per scaricare tutto;
- come preparare il file `api.js` e fare le chiamate, passo per passo;
- l'elenco completo degli endpoint, quelli già pronti e quelli ancora da fare;
- per ognuno, cosa mandate voi e cosa vi rimanda il backend;
- i dati della carta, per montarla con il componente `Card`.

La guida segue le regole già scritte nella [guida ai dati per il frontend](guida-frontend.md) e in [regole-codice.md](../regole-codice.md#frontend). Là ci sono anche le pagine una per una, il replay e i dettagli sulle immagini. Qui c'è la parte pratica.

**Come leggere lo stato degli endpoint:**

| Stato | Cosa vuol dire |
|-------|----------------|
| **Pronto** | È in `develop` e ha i test: si può usare subito, i dati sono quelli scritti qui. |
| **Da fare** | Non esiste ancora. Quello che c'è scritto è la proposta già approvata: si possono preparare le pagine, ma qualche nome può cambiare. Quando arriva, lo stato diventa "Pronto". |

Se qualcosa non torna, o vi serve un dato che non c'è, ditelo a Marco o Cristian.

## Indice

1. [Passo 1: installare i programmi](#passo-1-installare-i-programmi)
2. [Passo 2: scaricare il progetto](#passo-2-scaricare-il-progetto)
3. [Passo 3: avviare il backend](#passo-3-avviare-il-backend)
4. [Passo 4: avviare il frontend collegato al backend](#passo-4-avviare-il-frontend-collegato-al-backend)
5. [Passo 5: le regole delle chiamate](#passo-5-le-regole-delle-chiamate)
6. [Passo 6: il file api.js](#passo-6-il-file-apijs)
7. [Passo 7: usare api.js nelle pagine](#passo-7-usare-apijs-nelle-pagine)
8. [Elenco completo degli endpoint](#elenco-completo-degli-endpoint)
9. [Endpoint pronti, nel dettaglio](#endpoint-pronti-nel-dettaglio)
10. [Endpoint da fare, nel dettaglio](#endpoint-da-fare-nel-dettaglio)
11. [I dati per montare la carta](#i-dati-per-montare-la-carta)
12. [Tutti i codici di errore](#tutti-i-codici-di-errore)
13. [Controllo finale](#controllo-finale)

---

## Passo 1: installare i programmi

Si fa **una volta sola** per computer. Servono cinque programmi: quattro obbligatori e un editor consigliato.

| Programma | A cosa serve | Versione | Dove si scarica |
|-----------|--------------|----------|-----------------|
| **Git** | Scaricare il progetto e mandare le modifiche | Qualsiasi recente | [git-scm.com/downloads](https://git-scm.com/downloads) |
| **Java (Temurin)** | Far partire il backend | **25**, non un'altra | [adoptium.net/temurin/releases/?version=25](https://adoptium.net/temurin/releases/?version=25) |
| **Docker Desktop** | Far partire il database (PostgreSQL) e Mailpit, che il backend avvia da solo | Qualsiasi recente | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) |
| **Node.js** | Far partire il frontend (Vite) | La **LTS**; almeno la 22.12 | [nodejs.org/en/download](https://nodejs.org/en/download) |
| Visual Studio Code (consigliato) | Scrivere il codice | Qualsiasi recente | [code.visualstudio.com/download](https://code.visualstudio.com/download) |

**Prima di iniziare, sul Mac:** controllate che chip avete. Menu Apple (la mela in alto a sinistra) → **Informazioni su questo Mac**. Se alla voce "Chip" c'è scritto "Apple M1", "M2", "M3" o simili avete un **Mac Apple Silicon**; se c'è "Intel" avete un **Mac Intel**. Vi serve per scegliere i file giusti di Java e Docker.

**Dove si scrivono i comandi:**

- **Mac:** l'app **Terminale**: premete Cmd+Spazio, scrivete "Terminale" e premete Invio (si trova anche in Applicazioni → Utility).
- **Windows:** **Git Bash**, che si installa insieme a Git (Start → scrivete "Git Bash"). Usate Git Bash e non PowerShell: così i comandi di questa guida sono identici per Mac e Windows.

Dopo aver installato un programma, **chiudete e riaprite il terminale**: altrimenti il terminale non lo trova.

### 1.1 Git

- **Mac:** aprite il Terminale e scrivete `git --version`. Se Git non c'è, il Mac propone di installare gli "Strumenti della riga di comando": cliccate **Installa** e aspettate la fine.
- **Windows:** andate su [git-scm.com/downloads/win](https://git-scm.com/downloads/win), cliccate "Click here to download" e aprite il file. Nell'installazione lasciate tutte le opzioni come sono e cliccate sempre **Next**, poi **Install**. Insieme a Git si installa **Git Bash**.

### 1.2 Java 25 (Temurin)

1. Andate su [adoptium.net/temurin/releases/?version=25](https://adoptium.net/temurin/releases/?version=25). In alto dev'essere scelto **JDK 25 - LTS**.
2. Scorrete fino a "All platforms and packages" e cercate il vostro sistema:
   - **Mac:** nel riquadro **macOS** scegliete **Apple Silicon** oppure **Intel** (in base al vostro chip) e **JDK**, poi scaricate il file **PKG**;
   - **Windows:** nel riquadro **Windows** scegliete **x64** e **JDK**, poi scaricate il file **MSI**.
3. Aprite il file scaricato.
4. **Solo su Windows:** nella schermata "Custom Setup" cliccate su **Set JAVA_HOME variable** e scegliete "Will be installed on local hard drive". Senza questa opzione il backend potrebbe non trovare Java.
5. Finite l'installazione con **Next** (o **Continua** sul Mac) e **Install**.

Guida ufficiale, se serve: [adoptium.net/installation](https://adoptium.net/installation/).

### 1.3 Docker Desktop

1. Andate su [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) e cliccate **Download Docker Desktop**. Scegliete:
   - **Mac:** "Download for Mac – Apple Silicon" oppure "Download for Mac – Intel Chip", in base al vostro chip;
   - **Windows:** "Download for Windows – AMD64" (quasi tutti i PC).
2. **Mac:** aprite il file `.dmg` e trascinate Docker nella cartella **Applicazioni**. Poi aprite Docker dalle Applicazioni.
3. **Windows:** aprite il file `.exe` e lasciate spuntata l'opzione **Use WSL 2 instead of Hyper-V**. Alla fine riavviate il computer, poi aprite Docker Desktop dal menu Start.
4. Al primo avvio accettate le condizioni d'uso. L'accesso con un account Docker **non serve**: potete saltarlo.
5. Aspettate che in basso a sinistra Docker Desktop scriva **Engine running** (sul Mac, la balena nella barra in alto smette di muoversi).

Docker Desktop **deve essere aperto** ogni volta che avviate il backend.

Guide ufficiali, se serve: [Mac](https://docs.docker.com/desktop/setup/install/mac-install/) e [Windows](https://docs.docker.com/desktop/setup/install/windows-install/). Su Windows, se Docker dice che manca WSL, seguite [questa guida](https://learn.microsoft.com/it-it/windows/wsl/install): aprite PowerShell come amministratore, scrivete `wsl --install`, riavviate.

### 1.4 Node.js

1. Andate su [nodejs.org/en/download](https://nodejs.org/en/download).
2. Lasciate selezionata la versione con scritto **LTS** e il vostro sistema (macOS o Windows).
3. Scaricate l'installatore (**`.pkg`** sul Mac, **`.msi`** su Windows), apritelo e installate con le opzioni già scelte.

Con Node.js si installa anche `npm`, il programma che scarica le librerie del frontend.

### 1.5 Visual Studio Code (consigliato)

Da [code.visualstudio.com/download](https://code.visualstudio.com/download). Dentro VS Code conviene l'estensione **ESLint** (pannello Estensioni, a sinistra, cercate "ESLint"): segnala gli errori con le regole già messe nel progetto.

### 1.6 Controllo: è tutto installato?

Chiudete e riaprite il terminale (Terminale sul Mac, Git Bash su Windows) e scrivete un comando alla volta:

```bash
git --version
java -version
docker --version
node -v
npm -v
```

| Comando | Va bene se scrive | Se non va |
|---------|-------------------|-----------|
| `git --version` | `git version 2.` e un numero | Rifate il punto 1.1 |
| `java -version` | una riga con **`25`**, es. `openjdk version "25.0.1"` | Se c'è un altro numero (es. 17 o 21) o "command not found", rifate il punto 1.2 |
| `docker --version` | `Docker version` e un numero | Aprite Docker Desktop e riprovate; altrimenti rifate il punto 1.3 |
| `node -v` | `v22.12` o un numero più alto (es. `v24...`) | Rifate il punto 1.4 |
| `npm -v` | un numero, es. `11.6.2` | Rifate il punto 1.4 |

---

## Passo 2: scaricare il progetto

Il progetto è su GitHub: [github.com/cristianpappalardoITS/gruppo-05-dreamscape](https://github.com/cristianpappalardoITS/gruppo-05-dreamscape).

**La prima volta**, nel terminale, scaricatelo nella vostra cartella utente:

```bash
cd ~
git clone https://github.com/cristianpappalardoITS/gruppo-05-dreamscape.git
cd gruppo-05-dreamscape
git switch develop
```

Si crea la cartella `gruppo-05-dreamscape` dentro la cartella utente: sul Mac `/Users/vostro-nome/gruppo-05-dreamscape`, su Windows `C:\Users\vostro-nome\gruppo-05-dreamscape`. Si lavora sempre partendo da `develop`.

**Se il progetto ce l'avete già** (anche in un'altra cartella), entrate nella sua cartella e aggiornatelo prima di iniziare:

```bash
cd ~/gruppo-05-dreamscape
git switch develop
git pull
```

Se l'avete messo altrove, cambiate il percorso del `cd` (es. `cd ~/Desktop/gruppo-05-dreamscape`).

In VS Code: **File → Apri cartella** e scegliete `gruppo-05-dreamscape`. Il terminale si apre anche dentro VS Code (**Terminale → Nuovo terminale**; su Windows scegliete "Git Bash" dalla freccia accanto al +).

Per creare i branch e aprire le pull request seguite [workflow-github.md](../workflow-github.md).

---

## Passo 3: avviare il backend

1. Aprite **Docker Desktop** e aspettate **Engine running**.
2. Nel terminale, dalla cartella del progetto:

   ```bash
   cd sviluppo/backend
   cp .env.example .env
   ./gradlew bootRun
   ```

   `cp .env.example .env` si fa **solo la prima volta**: crea il file con i dati del database del vostro computer. Le volte dopo bastano `cd sviluppo/backend` e `./gradlew bootRun`.

3. La prima volta ci vogliono alcuni minuti: si scaricano Gradle, le librerie e le immagini di PostgreSQL e Mailpit. Serve internet.
4. Il backend è pronto quando nel terminale compare una riga con **`Started DreamscapeApiApplication`**. **Lasciate aperto quel terminale**: se lo chiudete, il backend si ferma. Per fermarlo voi: **Ctrl+C**.
5. Controllo: aprite [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) nel browser. Deve comparire `{"status":"UP"}`.

Quando il backend è acceso:

| Cosa | Indirizzo |
|------|-----------|
| API | `http://localhost:8080/api/v1` |
| Swagger (tutte le API, si possono provare) | `http://localhost:8080/swagger-ui.html` |
| Email di prova (recupero password) | `http://localhost:8025` |

Nel database di sviluppo ci sono tre utenti di prova, con 500 DC e la fazione ancora da scegliere:

| Username | Email | Password |
|----------|-------|----------|
| `anna_test` | `anna_test@example.test` | `prova-dreamscape` |
| `bruno_test` | `bruno_test@example.test` | `prova-dreamscape` |
| `carla_test` | `carla_test@example.test` | `prova-dreamscape` |

Sono password di prova, valide solo sul database del vostro computer. Le carte di prova si chiamano "(prova)" e **non hanno ancora le illustrazioni**: arrivano con `illustrationUrl: null` (vedi [I dati per montare la carta](#i-dati-per-montare-la-carta)).

---

## Passo 4: avviare il frontend collegato al backend

### 4.1 Collegare Vite al backend (una volta sola)

Il frontend (Vite, `http://localhost:5173`) e il backend (`http://localhost:8080`) in sviluppo sono su due indirizzi diversi. Alla consegna invece sono sullo **stesso indirizzo**. Per avere in sviluppo la stessa situazione si dice a Vite di passare al backend tutto quello che inizia con `/api` e `/images`.

Sostituite il contenuto di `sviluppo/frontend/vite.config.js` con questo:

```js
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react()],
  server: {
    // Le chiamate a /api e le immagini delle carte vanno al backend:
    // così sito e API hanno lo stesso indirizzo, come alla consegna.
    proxy: {
      "/api": "http://localhost:8080",
      "/images": "http://localhost:8080",
    },
  },
});
```

Basta farlo una volta per tutto il gruppo: chi lo fa per primo lo mette in una pull request, gli altri lo ricevono con `git pull`.

Perché serve:

- **Niente CORS:** per il browser le chiamate partono dallo stesso indirizzo della pagina.
- **Il cookie del refresh token funziona:** il backend lo manda con `SameSite=Strict`, quindi il browser lo rimanda solo allo stesso sito.
- Negli indirizzi si scrive sempre `/api/v1/...`, mai `http://localhost:8080/...`: lo stesso codice funziona in sviluppo e alla consegna.

### 4.2 Avviare il frontend

Con il backend acceso, aprite un **secondo terminale** (il primo resta occupato dal backend) e, dalla cartella del progetto:

```bash
cd sviluppo/frontend
npm install
npm run dev
```

- `npm install` scarica le librerie del frontend. Serve la prima volta e ogni volta che `package.json` cambia (es. dopo un `git pull` che aggiunge una libreria). Se non siete sicuri, rifatelo: non fa danni.
- `npm run dev` avvia Vite. Quando scrive `Local: http://localhost:5173/`, aprite quell'indirizzo nel browser. Ogni modifica ai file si vede subito, senza ricaricare.
- Per fermarlo: **Ctrl+C** nel suo terminale.

In sviluppo usate **Chrome o Firefox**. Il cookie del refresh token ha l'opzione `Secure`: questi due browser lo accettano anche su `http://localhost`. Se dopo l'accesso il cookie non si salva (può succedere con Safari), ditelo a SWD.

### 4.3 Controllo: frontend e backend si parlano?

Con tutti e due accesi, aprite nel browser [http://localhost:5173/api/v1/packs](http://localhost:5173/api/v1/packs). Deve comparire:

```json
{"status":401,"code":"UNAUTHORIZED","message":"Accesso scaduto o mancante. Accedi di nuovo.","fieldErrors":[]}
```

È giusto così: la richiesta è passata da Vite al backend, che risponde "serve l'accesso". Se invece compare la pagina del frontend, un 404 o una pagina vuota, il proxy del punto 4.1 manca o il backend è spento.

### 4.4 Se qualcosa non parte

| Cosa succede | Cosa fare |
|--------------|-----------|
| `./gradlew bootRun` dà un errore che parla di **Java**, di **toolchain** o di **languageVersion 25** | Manca Java 25 o il terminale non lo vede. Controllate `java -version` (punto 1.6); se serve reinstallate Java 25 (punto 1.2) e riaprite il terminale |
| `./gradlew bootRun` dà un errore che parla di **Docker** | Docker Desktop è chiuso o non ancora pronto: apritelo, aspettate **Engine running** e riprovate |
| Sul Mac `docker --version` funziona ma il backend dice che **non trova `docker`** | In Docker Desktop: **Settings** (l'ingranaggio) → **Advanced** → scegliete **System** → **Apply & restart**. Poi riaprite il terminale |
| Dopo un `git pull`, `./gradlew bootRun` si ferma con **Validate failed** o **Migrations have failed validation** | È arrivata una migration nuova e il database di sviluppo è vecchio. Da `sviluppo/backend`: `docker compose down -v`, poi di nuovo `./gradlew bootRun`. Il database di sviluppo si ricrea con i dati di prova (quello che avevate scritto a mano si perde) |
| **Port 8080 was already in use** | Il backend è già acceso in un altro terminale: usate quello, oppure fermatelo con Ctrl+C |
| `./gradlew: Permission denied` (Mac) | Scrivete `chmod +x gradlew` una volta, poi riprovate |
| `npm` o `node`: **command not found** | Chiudete e riaprite il terminale. Se non basta, reinstallate Node.js (punto 1.4) |
| Vite dice che serve **Node.js 20.19+ o 22.12+** | Node.js è vecchio: installate la LTS (punto 1.4) |
| Nel terminale di Vite compare **ECONNREFUSED** o **proxy error** | Il backend è spento: avviatelo (passo 3) |
| Dopo l'accesso, ricaricando la pagina si torna al login | Il cookie non si è salvato: usate Chrome o Firefox (punto 4.2) |
| Su Windows Docker Desktop dice che manca **WSL** | PowerShell come amministratore → `wsl --install` → riavviate (punto 1.3) |

Se dopo questi passi qualcosa ancora non va, mandate a Marco o Cristian la foto dell'errore con le ultime righe del terminale.

---

## Passo 5: le regole delle chiamate

Sono le regole già decise ([guida ai dati, capitolo 2](guida-frontend.md#2-regole-comuni-delle-api)), in breve.

**Indirizzi e dati**

| Regola | Esempio |
|--------|---------|
| Tutte le API iniziano con `/api/v1` | `/api/v1/packs` |
| Si manda e si riceve JSON, con nomi in `camelCase` | `{ "priceCredits": 500 }` |
| Gli id sono **UUID**: testo di 36 caratteri, si usa così com'è | `"019a1f31-0000-7000-8000-000000000001"` |
| Le date sono testo in UTC: per mostrarle si convertono nell'ora italiana | `"2026-11-11T10:15:00Z"` |
| I campi senza valore arrivano come `null`, non spariscono | `"imageUrl": null` |
| Le liste lunghe sono a pagine: `page` (da 0) e `size` | `?page=0&size=20` |

Una lista a pagine arriva così:

```json
{ "items": [ ], "page": 0, "size": 20, "totalItems": 57, "totalPages": 3 }
```

**Accesso**

- Il **token di accesso** dura 15 minuti e si tiene **solo in una variabile JavaScript**. Mai in `localStorage` o `sessionStorage`.
- Il **refresh token** dura 7 giorni ed è in un cookie `HttpOnly`: il browser lo manda da solo, il JavaScript non lo vede.
- Quando il token di accesso scade, il backend risponde `401`: si chiede un token nuovo a `/auth/refresh` e si riprova.

**Le cinque regole del codice**

1. Tutte le chiamate passano da **un solo file**, `api.js` (passo 6). Mai `fetch()` nelle pagine o nei componenti.
2. Il **server decide**: contenuto dei pacchetti, esito delle partite, validità dei mazzi, scambi. Il frontend mostra quello che riceve.
3. **I controlli si fanno due volte:** nel frontend per aiutare l'utente (campo vuoto, password corta), nel backend per sicurezza.
4. **Mai dati personali negli indirizzi**: niente email o password nella barra degli indirizzi o nei parametri `?`.
5. **Niente dati finti** nel frontend: le pagine usano solo le API.

**Gli errori** hanno sempre la stessa forma:

```json
{
  "status": 409,
  "code": "USERNAME_TAKEN",
  "message": "Questo username è già usato.",
  "fieldErrors": [ { "field": "username", "message": "Questo username è già usato." } ]
}
```

- `message` è già in italiano: si può mostrare così com'è.
- `code` serve al codice per capire cosa è successo (es. `NOT_ENOUGH_CREDITS`): mai confrontare il testo di `message`.
- `fieldErrors` è sempre una lista, vuota se l'errore non riguarda un campo. Ogni voce va mostrata accanto al suo campo.

| Stato HTTP | Vuol dire | Cosa fa la pagina |
|------------|-----------|-------------------|
| `200`, `201` | Tutto bene (`201` = creato) | Mostra il risultato |
| `202`, `204` | Tutto bene, nessun dato in risposta | Mostra una conferma |
| `400` | Dati sbagliati | Mostra `fieldErrors` accanto ai campi, oppure `message` |
| `401` | Token scaduto o mancante | Ci pensa `api.js`: rinnova il token o porta all'accesso |
| `403` | Non permesso (es. un giocatore nel backoffice) | "Non hai i permessi" |
| `404` | Non esiste | "Non trovato" |
| `409` | Conflitto (crediti che non bastano, username già usato) | Mostra `message` |
| `503` | Servizio fermo (es. il motore di gioco) | Mostra `message` e invita a riprovare |

---

## Passo 6: il file api.js

Create `sviluppo/frontend/src/api.js` e copiate questo codice. Fa quattro cose:

1. aggiunge il token di accesso a ogni chiamata;
2. se il token è scaduto, ne chiede uno nuovo e riprova **una** volta;
3. trasforma ogni errore in un `ApiError` con `status`, `code`, `message` e `fieldErrors`;
4. tiene il token solo in memoria.

```js
// api.js: l'unico file che parla con il backend.
// Il token di accesso resta in memoria; il refresh token è nel cookie HttpOnly.

const API_BASE = "/api/v1";
// Pagina di accesso: cambiatela se il percorso è un altro
const LOGIN_PAGE = "/login";

let accessToken = null;
// Rinnovo del token in corso: tutte le chiamate che lo aspettano usano lo stesso
let refreshing = null;

// Errore delle API: stessi campi della risposta del backend
export class ApiError extends Error {
  constructor({ status, code, message, fieldErrors }) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors ?? [];
  }
}

// Errore senza risposta del backend (spento, rete assente)
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
    credentials: "same-origin", // manda il cookie del refresh token
  });
}

// Chiede un token di accesso nuovo con il cookie. Restituisce true se l'utente è collegato.
// Mai due rinnovi insieme: il backend cambia il cookie a ogni rinnovo, e se riceve due volte
// lo stesso cookie scollega l'utente da tutti i dispositivi.
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

// Chiamata alle API. Esempi:
//   await api("/packs")
//   await api(`/packs/${packId}/open`, { method: "POST" })
//   await api("/me", { method: "PUT", body: { email } })
// Restituisce i dati della risposta, oppure null se la risposta è vuota (202, 204).
// Se qualcosa va storto lancia un ApiError.
export async function api(path, { method = "GET", body } = {}) {
  const isAuthCall = path.startsWith("/auth/");
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

// Accesso con username o email. Restituisce l'utente: { id, username, role, credits }
export async function login(loginName, password) {
  const data = await api("/auth/login", { method: "POST", body: { login: loginName, password } });
  accessToken = data.accessToken;
  return data.user;
}

// Uscita: il backend cancella il cookie, qui si dimentica il token
export async function logout() {
  try {
    await api("/auth/logout", { method: "POST" });
  } finally {
    accessToken = null;
  }
}

// Trasforma fieldErrors in un oggetto { nomeCampo: messaggio }, comodo nei form
export function errorsByField(error) {
  return Object.fromEntries((error?.fieldErrors ?? []).map((item) => [item.field, item.message]));
}
```

Due cose da sapere:

- **`NETWORK_ERROR` non viene dal backend:** lo crea `api.js` quando il backend non risponde.
- **Perché un solo rinnovo alla volta:** in sviluppo React (`StrictMode`) esegue due volte ogni `useEffect`. Senza `refreshing`, all'apertura partirebbero due rinnovi con lo stesso cookie, e il backend li prenderebbe per un furto di token, scollegando l'utente.

---

## Passo 7: usare api.js nelle pagine

### 7.1 All'apertura del sito: l'utente è ancora collegato?

Il token è in memoria, quindi si perde quando si ricarica la pagina. Il cookie invece resta. All'avvio si chiama `refresh()` una volta, prima di mostrare le pagine dell'area utente. In `main.jsx`:

```jsx
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { refresh } from "./api.js";
import App from "./App.jsx";
import "./index.css";

// isLoggedIn è true se il cookie è ancora valido: l'utente non deve rifare l'accesso
refresh().then((isLoggedIn) => {
  createRoot(document.getElementById("root")).render(
    <StrictMode>
      <App isLoggedIn={isLoggedIn} />
    </StrictMode>,
  );
});
```

Il primo accesso del giorno, anche con questo `refresh()`, dà i 50 DC della ricompensa giornaliera.

Il `refresh()` restituisce solo il token, non il saldo: dopo un ricaricamento i crediti per l'intestazione si leggono con [`GET /me/summary`](#dashboard-get-mesummary).

### 7.2 Leggere dati: GET in un useEffect

Esempio della pagina Negozio. È lo schema da usare per ogni pagina che **legge** dati: stato di caricamento, errore, dati.

```jsx
import { useEffect, useState } from "react";
import { api } from "../api.js";

export default function ShopPage() {
  const [packs, setPacks] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api("/packs").then(setPacks).catch(setError);
  }, []);

  if (error) return <p role="alert">{error.message}</p>;
  if (!packs) return <p>Caricamento...</p>;

  return (
    <ul>
      {packs.map((pack) => (
        <li key={pack.id}>
          {pack.name}: {pack.priceCredits} DC
        </li>
      ))}
    </ul>
  );
}
```

### 7.3 Mandare dati: un form con gli errori accanto ai campi

Esempio della pagina di accesso già fatta (`LoginPage.jsx`). Ai campi serve un `name`, e il campo "Email" accetta **username o email**: conviene chiamarlo "Username o email".

```jsx
import { useState } from "react";
import { login } from "../api.js";

const LoginPage = () => {
  const [error, setError] = useState(null);
  const [sending, setSending] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const form = new FormData(event.target);
    setSending(true);
    setError(null);
    try {
      const user = await login(form.get("login"), form.get("password"));
      // ADMIN va al backoffice, PLAYER alla dashboard
      window.location.href = user.role === "ADMIN" ? "/admin" : "/dashboard";
    } catch (err) {
      setError(err.message); // es. "Username, email o password non corretti."
    } finally {
      setSending(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <InputField label="Username o email" name="login" autoComplete="username" required />
      <PasswordField label="Password" name="password" autoComplete="current-password" required />
      {error && <p role="alert">{error}</p>}
      <LoginButton type="submit" disabled={sending}>ACCEDI</LoginButton>
    </form>
  );
};
```

Con più campi, come nella registrazione, si usa `errorsByField`:

```jsx
import { useState } from "react";
import { api, errorsByField } from "../api.js";

export default function RegisterPage() {
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
      await api("/auth/register", {
        method: "POST",
        body: {
          username: form.get("username"),
          email: form.get("email"),
          password: form.get("password"),
          privacyAccepted: form.get("privacy") === "on",
          termsAccepted: form.get("terms") === "on",
        },
      });
      // Registrato: si fa l'accesso e si va al Benvenuto
    } catch (err) {
      setErrors(errorsByField(err)); // { username: "Questo username è già usato." }
      setMessage(err.message);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input name="username" />
      {errors.username && <small>{errors.username}</small>}
      {/* ...gli altri campi allo stesso modo... */}
      {message && <p role="alert">{message}</p>}
      <button type="submit">Registrati</button>
    </form>
  );
}
```

### 7.4 Azioni che cambiano dati: comprare un pacchetto

```jsx
const [buying, setBuying] = useState(false);

async function buy(pack) {
  setBuying(true);
  try {
    const result = await api(`/packs/${pack.id}/open`, { method: "POST" });
    // Si va alla pagina dell'apertura: ricaricarla non compra un altro pacchetto
    window.location.href = `/negozio/apertura/${result.openingId}`;
  } catch (err) {
    if (err.code === "NOT_ENOUGH_CREDITS") {
      setMessage("Non hai abbastanza Dream Credits.");
    } else {
      setMessage(err.message);
    }
  } finally {
    setBuying(false);
  }
}

// Il pulsante si spegne mentre la richiesta è in corso: niente doppi acquisti
<button onClick={() => buy(pack)} disabled={buying}>Compra</button>;
```

**Regola importante:** una chiamata che cambia dati (`POST`, `PUT`, `DELETE`) parte **solo da un clic** dell'utente, mai da un `useEffect`. In sviluppo React esegue due volte gli `useEffect`: si comprerebbero due pacchetti.

### 7.5 Liste a pagine: i crediti

```jsx
const [page, setPage] = useState(0);
const [data, setData] = useState(null);

useEffect(() => {
  api(`/me/credit-transactions?page=${page}&size=20`).then(setData);
}, [page]);

// data.items = le righe; data.totalPages = quante pagine ci sono
<button disabled={page === 0} onClick={() => setPage(page - 1)}>Indietro</button>
<button disabled={!data || page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Avanti</button>
```

### 7.6 Date e numeri

```js
// "2026-11-11T10:15:00Z" -> "11/11/2026, 11:15"
const formatDate = (iso) =>
  new Date(iso).toLocaleString("it-IT", { timeZone: "Europe/Rome", dateStyle: "short", timeStyle: "short" });

// 1000 -> "1.000"
const formatCredits = (credits) => credits.toLocaleString("it-IT");
```

---

## Elenco completo degli endpoint

Accesso: **pubblico** = senza accesso; **utente** = serve il token; **admin** = solo `role: ADMIN`. Gli indirizzi sono dopo `/api/v1`.

### Pronti (in develop)

| Metodo e indirizzo | Accesso | A cosa serve |
|--------------------|---------|--------------|
| `POST /auth/register` | pubblico | Registrazione |
| `POST /auth/login` | pubblico | Accesso |
| `POST /auth/refresh` | pubblico (cookie) | Token di accesso nuovo |
| `POST /auth/logout` | pubblico (cookie) | Uscita |
| `POST /auth/password-reset-requests` | pubblico | Password dimenticata: manda l'email |
| `POST /auth/password-resets` | pubblico | Nuova password dal link dell'email |
| `GET /me` | utente | Dati del profilo |
| `PUT /me` | utente | Cambio email |
| `PUT /me/password` | utente | Cambio password |
| `DELETE /me` | utente | Cancellazione dell'account |
| `GET /me/credit-transactions` | utente | Movimenti dei Dream Credits |
| `GET /me/welcome` | utente | Pagina Benvenuto: fazione scelta e carte ricevute |
| `POST /me/welcome` | utente | Scelta della fazione: set iniziale e pacchetti di benvenuto |
| `GET /packs` | utente | Pacchetti in vendita nel negozio |
| `POST /packs/{packId}/open` | utente | Compra e apre un pacchetto |
| `GET /pack-openings/{openingId}` | utente | Rilegge un'apertura |
| `GET /me/summary` | utente | Dashboard: crediti, punteggio, lega, ultime carte (missioni e contatori arrivano con #125 e #93) |

### Da fare

| Metodo e indirizzo | Accesso | A cosa serve | Issue | Chi | Sprint |
|--------------------|---------|--------------|-------|-----|--------|
| `GET /cards` | pubblico | Catalogo (anche `?featured=true` per la Home) | #77 | Cristian | 4 |
| `GET /cards/{cardId}` | pubblico | Una carta | #77 | Cristian | 4 |
| `GET /factions` | pubblico | Le tre fazioni con il loro Lord | #77 | Cristian | 4 |
| `GET /rules` | pubblico | I numeri del regolamento | #77 | Cristian | 4 |
| `GET /me/cards` | utente | La mia collezione, con filtri | #80 | Cristian | 4 |
| `GET /decks` | utente | I miei mazzi | #80 | Cristian | 4 |
| `GET /decks/{deckId}` | utente | Un mazzo | #80 | Cristian | 4 |
| `POST /decks` | utente | Nuovo mazzo | #80 | Cristian | 4 |
| `PUT /decks/{deckId}` | utente | Modifica di un mazzo | #80 | Cristian | 4 |
| `DELETE /decks/{deckId}` | utente | Elimina un mazzo | #80 | Cristian | 4 |
| `GET /friends` | utente | I miei amici | #93 | Marco | 6 |
| `DELETE /friends/{userId}` | utente | Togli un amico | #93 | Marco | 6 |
| `GET /users?search=` | utente | Cerca un Dreamweaver per username | #93 | Marco | 6 |
| `GET /users/{userId}/cards` | utente | Collezione di un amico (per gli scambi) | #93 | Marco | 6 |
| `GET /friend-requests?direction=` | utente | Richieste di amicizia ricevute o inviate | #93 | Marco | 6 |
| `POST /friend-requests` | utente | Manda una richiesta | #93 | Marco | 6 |
| `POST /friend-requests/{requestId}/accept` | utente | Accetta | #93 | Marco | 6 |
| `DELETE /friend-requests/{requestId}` | utente | Rifiuta o annulla | #93 | Marco | 6 |
| `GET /trades?box=` | utente | Scambi ricevuti, inviati, storico | #93 | Marco | 6 |
| `GET /trades/{tradeId}` | utente | Uno scambio | #93 | Marco | 6 |
| `POST /trades` | utente | Proponi uno scambio | #93 | Marco | 6 |
| `POST /trades/preview` | utente | Mazzi che cambierebbero con la proposta | #93 | Marco | 6 |
| `GET /trades/{tradeId}/deck-warnings` | utente | Mazzi che cambierebbero accettando | #93 | Marco | 6 |
| `POST /trades/{tradeId}/accept` | utente | Accetta | #93 | Marco | 6 |
| `POST /trades/{tradeId}/reject` | utente | Rifiuta | #93 | Marco | 6 |
| `POST /trades/{tradeId}/cancel` | utente | Annulla (chi propone) | #93 | Marco | 6 |
| `PUT /me/ranked-deck` | utente | Scegli il mazzo classificato | #127 | Cristian | 6 |
| `POST /matches/ranked` | utente | Gioca una classificata | #127 | Cristian | 6 |
| `POST /matches/casual` | utente | Gioca una rapida | #127 | Cristian | 6 |
| `GET /me/matches` | utente | Storico delle partite | #127 | Cristian | 6 |
| `GET /matches/{matchId}` | utente | Risultato e replay | #94 | Cristian | 6 |
| `GET /challenges?box=` | utente | Sfide ricevute o inviate | #94 | Cristian | 6 |
| `POST /challenges` | utente | Sfida un amico | #94 | Cristian | 6 |
| `POST /challenges/{challengeId}/accept` | utente | Accetta e gioca | #94 | Cristian | 6 |
| `POST /challenges/{challengeId}/reject` | utente | Rifiuta | #94 | Cristian | 6 |
| `POST /challenges/{challengeId}/cancel` | utente | Annulla | #94 | Cristian | 6 |
| `GET /leagues` | utente | Le leghe con soglie e premi | #106 | Cristian | 6 |
| `POST /contact-messages` | pubblico | Form Contatti | #95 | Cristian | 6 |
| `GET /images/...` | pubblico | Illustrazioni e immagini dei pacchetti | #95 | Cristian | 6 |
| `/admin/...` | admin | Backoffice (elenco nel [dettaglio](#backoffice)) | #95 | Cristian | 6 |

Sprint 4: 9-15/11. Sprint 5: 16-22/11. Sprint 6: 23-29/11.

---

## Endpoint pronti, nel dettaglio

Per ognuno: cosa **mandate**, cosa **ricevete** e gli errori possibili. Gli errori comuni a tutti (`VALIDATION_ERROR`, `UNAUTHORIZED`, ...) sono nella [tabella finale](#tutti-i-codici-di-errore).

### Registrazione: `POST /auth/register`

Mandate:

```json
{
  "username": "lucida",
  "email": "lucida@example.com",
  "password": "una-password-lunga",
  "privacyAccepted": true,
  "termsAccepted": true
}
```

| Campo | Regola |
|-------|--------|
| `username` | Da 3 a 30 caratteri: lettere, numeri e trattino basso |
| `email` | Email valida, al massimo 255 caratteri |
| `password` | Da 8 a 100 caratteri |
| `privacyAccepted`, `termsAccepted` | Devono essere `true` |

Username ed email sono unici **senza distinguere le maiuscole** ("Lucida" e "lucida" sono lo stesso utente).

Ricevete `201`:

```json
{ "id": "019a1f2e-7c41-7d3a-9b2e-5f6a7b8c9d01", "username": "lucida", "role": "PLAYER", "credits": 500 }
```

La registrazione **non** fa l'accesso: dopo si chiama `login()` e si va alla pagina Benvenuto.

Errori: `409 USERNAME_TAKEN`, `409 EMAIL_TAKEN` (anche in `fieldErrors`), `400 VALIDATION_ERROR`.

### Accesso: `POST /auth/login`

Mandate (con `login()` di `api.js`):

```json
{ "login": "lucida", "password": "una-password-lunga" }
```

`login` può essere lo username **o** l'email.

Ricevete `200`, e il refresh token arriva nel cookie:

```json
{
  "accessToken": "eyJhbGciOi...",
  "expiresIn": 900,
  "user": { "id": "019a1f2e-7c41-7d3a-9b2e-5f6a7b8c9d01", "username": "lucida", "role": "PLAYER", "credits": 500 }
}
```

- `expiresIn` è in secondi (900 = 15 minuti).
- `role`: `PLAYER` o `ADMIN`. Con `ADMIN` si va al backoffice. L'admin entra dalla stessa pagina.
- `credits` è il saldo: si mostra nell'intestazione. Dopo un ricaricamento della pagina si rilegge con `GET /me/summary`.

Errori: `401 INVALID_CREDENTIALS` (stesso messaggio se l'utente non esiste o la password è sbagliata), `401 ACCOUNT_DISABLED` (account cancellato).

### Token nuovo: `POST /auth/refresh`

Lo chiama solo `api.js`. Non si manda niente: parte il cookie.

Ricevete `200` e un cookie nuovo:

```json
{ "accessToken": "eyJhbGciOi...", "expiresIn": 900 }
```

Errore: `401 INVALID_REFRESH_TOKEN` (cookie mancante, scaduto o revocato): l'utente deve rifare l'accesso.

### Uscita: `POST /auth/logout`

Con `logout()` di `api.js`. Non si manda niente. Ricevete `204`: il backend revoca il refresh token e cancella il cookie. Poi si va alla Home.

### Password dimenticata: `POST /auth/password-reset-requests`

Mandate:

```json
{ "email": "lucida@example.com" }
```

Ricevete **sempre** `202`, anche se l'email non è registrata: così nessuno può scoprire chi è iscritto. La pagina mostra sempre lo stesso messaggio, es. "Se l'email è registrata, ti abbiamo mandato un link".

L'email contiene un link a `nuova-password.html?token=...` (indirizzo da confermare con chi fa la pagina). Il link vale **1 ora** e una volta sola. In sviluppo l'email si legge in Mailpit, `http://localhost:8025`.

### Nuova password: `POST /auth/password-resets`

La pagina legge il token dall'indirizzo e lo manda con la password nuova:

```js
const token = new URLSearchParams(window.location.search).get("token");
await api("/auth/password-resets", { method: "POST", body: { token, newPassword } });
```

Ricevete `204`. Tutti i dispositivi vengono scollegati: si va all'accesso.

Errore: `400 INVALID_RESET_TOKEN` (link scaduto o già usato): si propone di chiederne uno nuovo.

### Profilo: `GET /me`

Ricevete:

```json
{
  "id": "019a1f2e-7c41-7d3a-9b2e-5f6a7b8c9d01",
  "username": "lucida",
  "email": "lucida@example.com",
  "role": "PLAYER",
  "createdAt": "2026-10-26T14:30:00Z"
}
```

Qui **non** ci sono i crediti: arrivano con l'accesso e con `GET /me/summary`.

### Cambio email: `PUT /me`

Mandate:

```json
{ "email": "nuova@example.com" }
```

Ricevete `200` con il profilo aggiornato (stessa forma di `GET /me`). Lo username non si cambia.

Errori: `409 EMAIL_TAKEN`, `400 VALIDATION_ERROR`.

### Cambio password: `PUT /me/password`

Mandate:

```json
{ "currentPassword": "una-password-lunga", "newPassword": "una-password-nuova" }
```

Ricevete `204`. Gli **altri** dispositivi vengono scollegati; questo resta collegato.

Errore: `400 WRONG_PASSWORD` (password attuale sbagliata).

### Cancellazione dell'account: `DELETE /me`

Mandate:

```json
{ "password": "una-password-lunga" }
```

Ricevete `204` e il cookie viene cancellato: si va alla Home. L'account diventa anonimo e non si può recuperare: **chiedete conferma prima**.

Errore: `400 WRONG_PASSWORD`.

### Movimenti dei crediti: `GET /me/credit-transactions?page=0&size=20`

`page` parte da 0 (di base 0); `size` va da 1 a 100 (di base 20). Ricevete una lista a pagine, dal movimento più recente:

```json
{
  "items": [
    { "type": "PACK_PURCHASE", "amount": -500, "balanceAfter": 50, "createdAt": "2026-11-11T10:15:00Z" },
    { "type": "DAILY_REWARD", "amount": 50, "balanceAfter": 550, "createdAt": "2026-11-11T08:00:00Z" },
    { "type": "SIGNUP_BONUS", "amount": 500, "balanceAfter": 500, "createdAt": "2026-11-10T18:00:00Z" }
  ],
  "page": 0, "size": 20, "totalItems": 3, "totalPages": 1
}
```

| `type` | Da mostrare come |
|--------|------------------|
| `SIGNUP_BONUS` | Regalo di benvenuto |
| `DAILY_REWARD` | Ricompensa giornaliera |
| `PACK_PURCHASE` | Acquisto pacchetto |
| `MATCH_REWARD` | Vittoria in partita |
| `MISSION_REWARD` | Missione completata |
| `LEAGUE_REWARD` | Premio di lega |
| `ADMIN_ADJUSTMENT` | Correzione dell'amministratore |

`amount` è negativo per le spese. Gli ultimi quattro tipi arriveranno con le partite, le missioni e il backoffice.

### Benvenuto: `GET /me/welcome` e `POST /me/welcome`

**All'apertura della pagina:** `GET /me/welcome`. Se la fazione non è ancora scelta:

```json
{ "starterFaction": null, "starterCards": [], "welcomeOpenings": [] }
```

Allora si mostrano le tre fazioni.

**Scelta della fazione:** `POST /me/welcome`. Mandate:

```json
{ "starterFaction": "DREAMS" }
```

Valori: `DREAMS`, `NIGHTMARES`, `MEMORIES`. Ricevete `201` (e da quel momento anche `GET /me/welcome` risponde così):

```json
{
  "starterFaction": "DREAMS",
  "starterCards": [ { "card": { "...": "..." } } ],
  "welcomeOpenings": [
    { "openingId": "019a2b40-0000-7000-8000-000000000101", "packName": "Welcome Pack", "cards": [ { "card": { "...": "..." } } ] },
    { "openingId": "019a2b40-0000-7000-8000-000000000102", "packName": "Welcome Pack Epico", "cards": [ { "card": { "...": "..." } } ] }
  ]
}
```

- `starterCards`: le 20 carte del set iniziale (14 Comuni e 6 Rare della fazione scelta). Bastano per un mazzo valido. Il mazzo **non** si crea da solo: conviene un pulsante "Componi il tuo primo mazzo".
- `welcomeOpenings`: i due pacchetti di benvenuto, già aperti.
- Ogni `card` è l'oggetto [Carta](#i-dati-per-montare-la-carta).
- Tutto gratis: i crediti non cambiano.

Errori: `409 WELCOME_ALREADY_CLAIMED` (la fazione si sceglie una volta sola), `400 INVALID_FACTION`, `400 VALIDATION_ERROR` (fazione vuota).

### Negozio: `GET /packs`

Ricevete i pacchetti in vendita **adesso**, dal più economico. Oggi sono sei: Nap Pack (300 DC), Dream Pack (500), Faceless, Lucid e Still Time Pack (600), REM Pack (1.000). Nomi, prezzi e percentuali si leggono sempre da qui, mai scritti a mano.

```json
[
  {
    "id": "019a1f31-0000-7000-8000-000000000001",
    "name": "Dream Pack",
    "description": "Il pacchetto di ogni Dreamweaver: 5 carte, con una Rara sicura e uno slot speciale.",
    "priceCredits": 500,
    "fullArtChance": 20.0,
    "factionCode": null,
    "availableUntil": null,
    "imageUrl": null,
    "covers": [],
    "slots": [
      { "cardCount": 3, "rarities": [ { "code": "COMMON", "chance": 100.0 } ] },
      { "cardCount": 1, "rarities": [ { "code": "RARE", "chance": 100.0 } ] },
      { "cardCount": 1, "rarities": [
          { "code": "RARE", "chance": 70.0 },
          { "code": "EPIC", "chance": 25.0 },
          { "code": "LEGENDARY", "chance": 5.0 } ] }
    ]
  }
]
```

| Campo | Cosa vuol dire |
|-------|----------------|
| `imageUrl` | Immagine del pacchetto. Oggi è `null` per tutti: si mostra un'immagine generica |
| `factionCode` | `null` = carte di tutte le fazioni; altrimenti la fazione del pacchetto |
| `availableUntil` | `null` = sempre in vendita; altrimenti la data di fine (UTC) |
| `covers` | Copertine da scegliere. Oggi sempre vuota |
| `slots` | Cosa contiene: "3 Comuni, 1 Rara, 1 carta speciale: Rara 70%, Epica 25%, Leggendaria 5%". `chance` è già in percentuale |
| `fullArtChance` | Percentuale delle **Leggendarie** che escono in full art. Con 5% di Leggendaria e 20 di `fullArtChance`, la full art esce nell'1% dei pacchetti (5 × 20 / 100) |

### Comprare e aprire: `POST /packs/{packId}/open`

Non si manda niente (se un giorno un pacchetto avrà le copertine: `{ "coverId": "..." }`). Ricevete `201`:

```json
{
  "openingId": "019a2b44-5c6d-7e8f-9a0b-1c2d3e4f5a6b",
  "creditsAfter": 0,
  "cards": [
    { "card": { "code": "TEST-001", "...": "..." } },
    { "card": { "code": "TEST-013-FA", "variantType": "FULL_ART", "...": "..." } }
  ]
}
```

- `creditsAfter`: il saldo nuovo, da mostrare subito nell'intestazione.
- `cards`: nell'ordine di uscita; l'ultima è quella dello slot speciale. Sono già nella collezione.
- Poi si va su `/negozio/apertura/{openingId}`, che legge `GET /pack-openings/{openingId}`.

Errori: `409 NOT_ENOUGH_CREDITS` (non si compra niente), `409 PACK_NOT_ON_SALE` (non più in vendita: ricaricate `GET /packs`), `404 PACK_NOT_FOUND`, `400 INVALID_COVER`.

### Rileggere un'apertura: `GET /pack-openings/{openingId}`

Ricevete:

```json
{
  "openingId": "019a2b44-5c6d-7e8f-9a0b-1c2d3e4f5a6b",
  "packName": "Dream Pack",
  "openedAt": "2026-11-11T10:15:00Z",
  "cards": [ { "card": { "...": "..." } } ]
}
```

Errori: `404 PACK_OPENING_NOT_FOUND` (non esiste o è di un altro utente): "Apertura non trovata" e un link al negozio. `400 MALFORMED_REQUEST` se l'id nell'indirizzo è scritto male.

### Dashboard: `GET /me/summary`

Ricevete:

```json
{
  "username": "lucida",
  "credits": 750,
  "rating": 1040,
  "league": { "code": "DORMANT", "name": "Dormiente", "nextLeagueName": "Sognatore", "nextLeagueMinRating": 1100 },
  "hasRankedDeck": true,
  "missions": [],
  "pendingFriendRequests": 0,
  "pendingTrades": 0,
  "pendingChallenges": 0,
  "latestCards": [ { "card": { "...": "..." } } ]
}
```

- È anche il modo per avere i crediti dopo un ricaricamento della pagina: `GET /me` non li ha.
- `rating` è il punteggio del rank: si parte da 1000.
- `league` è la lega del punteggio attuale. Nella lega più alta (Dreamweaver) `nextLeagueName` e `nextLeagueMinRating` sono `null`: al posto di "prossima lega" si mostra che è la più alta.
- `hasRankedDeck`: se è `false`, conviene un invito a scegliere il mazzo classificato. Senza, il giocatore non può essere scelto come avversario.
- `latestCards` sono le carte dell'ultima apertura (comprata o regalata), nell'ordine di uscita, con la stessa forma di [`GET /pack-openings/{openingId}`](#rileggere-unapertura-get-pack-openingsopeningid). Lista vuota se il giocatore non ha mai aperto pacchetti.
- **Per ora** `missions` è una lista vuota e i tre contatori valgono `0`: si riempiono quando sono pronte le missioni (#125), amici e scambi (#93) e le sfide. La forma della risposta non cambia, quindi la Dashboard si può già fare tutta. Una missione avrà questa forma:

```json
{ "code": "DAILY_PLAY", "name": "Missione giornaliera", "description": "Gioca una partita.", "progress": 0, "goalCount": 1, "rewardCredits": 100, "isCompleted": false, "resetsAt": "2026-11-17T23:00:00Z" }
```

`resetsAt` è in UTC: la missione giornaliera si azzera a mezzanotte, ora italiana, l'obiettivo settimanale il lunedì a mezzanotte. I contatori `pendingFriendRequests`, `pendingTrades` e `pendingChallenges` servono per il numero nel menu: non ci sono altre notifiche.

---

## Endpoint da fare, nel dettaglio

Sono le proposte già approvate. Per i più grandi c'è l'esempio completo nella [guida ai dati](guida-frontend.md).

### Catalogo (#77)

`GET /cards`: lista a pagine di oggetti [Carta](#i-dati-per-montare-la-carta). Con `?featured=true` alcune carte per la Home.

`GET /cards/{cardId}`: una Carta.

`GET /factions`:

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

`lordCardId` è `null` finché non c'è il catalogo vero.

`GET /rules`:

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

`maxPerDeck: null` = nessun limite. I numeri servono all'editor dei mazzi: mai scriverli a mano.

Gli oggetti di questi tre endpoint sono già scritti nel backend: la forma non cambierà.

### Collezione e mazzi (#80)

`GET /me/cards?faction=DREAMS&rarity=RARE&search=sogno&fullArt=true&page=0&size=24`: lista a pagine. Ogni voce:

```json
{ "card": { "...": "..." }, "quantity": 2 }
```

Tutti i filtri sono facoltativi. La full art è una voce a parte, con il suo `card.id`.

`GET /decks`, `GET /decks/{deckId}`. Un mazzo:

```json
{
  "id": "019a2a10-1b2c-7d3e-8f40-a1b2c3d4e5f6",
  "name": "Incubi veloci",
  "totalCards": 20,
  "isValid": true,
  "isRanked": false,
  "problems": [],
  "cards": [ { "card": { "...": "..." }, "quantity": 2 } ]
}
```

`POST /decks` (risponde `201`) e `PUT /decks/{deckId}`. Mandate:

```json
{
  "name": "Incubi veloci",
  "cards": [
    { "cardId": "019a1f30-0000-7000-8000-000000000005", "quantity": 2 },
    { "cardId": "019a1f30-0000-7000-8000-000000000108", "quantity": 1 }
  ]
}
```

- Si salvano solo mazzi validi: se non lo è, `400` e `problems` dice cosa non va (es. "Le Rare sono 9: il massimo è 8.").
- Normale e full art della stessa carta contano insieme per le copie massime.
- Un mazzo salvato può diventare non valido (scambio, carta disattivata): `isValid: false` e `problems` vanno mostrati bene nella lista.

`DELETE /decks/{deckId}`: `204`.

### Amici (#93)

| Chiamata | Mandate | Ricevete |
|----------|---------|----------|
| `GET /friends` | | `[ { "user": { "id": "…", "username": "incubo_99" }, "since": "2026-10-20T18:11:00Z" } ]` |
| `GET /users?search=luc` | | Lista di `{ "id", "username" }` |
| `POST /friend-requests` | `{ "username": "lucida" }` | `201` con la richiesta |
| `GET /friend-requests?direction=received` (o `sent`) | | Lista di richieste: `{ "id", "user": { "id", "username" }, "createdAt" }` |
| `POST /friend-requests/{requestId}/accept` | | `200` con l'amico |
| `DELETE /friend-requests/{requestId}` | | `204` (rifiutata o annullata) |
| `DELETE /friends/{userId}` | | `204`; annulla anche scambi e sfide in attesa tra voi due |

Degli altri giocatori si vede solo lo username: mai email o crediti.

### Scambi (#93)

Si scambia **solo con gli amici**. Proposta, `POST /trades` (risponde `201`):

```json
{
  "recipientId": "019a1f2e-8d52-7e4b-8c3f-6a7b8c9d0e12",
  "message": "Ti va?",
  "offered":   [ { "cardId": "019a1f30-0000-7000-8000-000000000007", "quantity": 1 } ],
  "requested": [ { "cardId": "019a1f30-0000-7000-8000-000000000001", "quantity": 5 } ]
}
```

| Chiamata | Cosa fa |
|----------|---------|
| `GET /trades?box=received` (o `sent`, `history`) | Lista di scambi |
| `GET /trades/{tradeId}` | Uno scambio, con le carte e lo stato: `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED` |
| `GET /users/{userId}/cards` | Collezione di un amico, per scegliere cosa chiedere |
| `POST /trades/preview` (stesso corpo della proposta) | Mazzi che cambierebbero: `{ "deckWarnings": [ ... ] }` |
| `GET /trades/{tradeId}/deck-warnings` | Lo stesso, prima di accettare |
| `POST /trades/{tradeId}/accept`, `/reject`, `/cancel` | Accetta, rifiuta (chi riceve), annulla (chi propone) |

Errori: `409 CARDS_LOCKED_IN_TRADE` (copie già offerte in un altro scambio), `409` se all'accettazione qualcuno non ha più le carte. Gli avvisi sui mazzi (`INVALID`, `NORMAL_VERSION`) sono spiegati nella [guida ai dati](guida-frontend.md#scambi).

### Partite e sfide (#127, #94)

| Chiamata | Mandate | Ricevete |
|----------|---------|----------|
| `PUT /me/ranked-deck` | `{ "deckId": "…" }` | `204` |
| `POST /matches/ranked` | `{ "deckId": "…" }` | Il risultato della partita |
| `POST /matches/casual` | `{ "deckId": "…" }` | Il risultato della partita |
| `GET /me/matches?type=RANKED&page=0&size=20` | | Lista a pagine (`type` facoltativo: `RANKED`, `CASUAL`) |
| `GET /matches/{matchId}` | | Il risultato, con il replay |
| `GET /challenges?box=received` (o `sent`) | | Lista di sfide |
| `POST /challenges` | `{ "opponentId": "…", "deckId": "…" }` | `201` con la sfida |
| `POST /challenges/{challengeId}/accept` | `{ "deckId": "…" }` | `{ "matchId": "…" }` |
| `POST /challenges/{challengeId}/reject`, `/cancel` | | `204` |

- La partita la gioca il server subito: dopo la chiamata si va su `/partite/{matchId}`.
- Errori: `409 NO_OPPONENT_FOUND` (nessun avversario), `503 ENGINE_UNAVAILABLE` (motore fermo: niente partita, niente crediti), `409` per una seconda sfida in attesa con lo stesso amico.
- Il risultato completo, con gli eventi del replay, è nella [guida ai dati](guida-frontend.md#partite-classificata-rapida-e-sfide-tra-amici).

### Leghe (#106)

`GET /leagues`: le leghe con il punteggio minimo e il premio. Proposta: Dormiente 0, Sognatore 1100, Lucido 1300, Dreamweaver 1500.

### Contatti (#95)

`POST /contact-messages`, pubblico. Mandate `{ "name": "…", "email": "…", "message": "…" }`, ricevete `201`.

### Backoffice

Solo `role: ADMIN` (#95, Cristian, Sprint 6). Un giocatore riceve `403`.

| Chiamata | A cosa serve |
|----------|--------------|
| `GET /admin/stats` | Numeri di utenti, carte, pacchetti, partite |
| `GET`, `POST /admin/cards`; `PUT /admin/cards/{cardId}` | Carte (si disattivano, non si cancellano) |
| `POST /admin/images` (`multipart/form-data`) | Caricamento delle illustrazioni: risponde `{ "url", "thumbUrl" }` |
| `GET`, `POST /admin/packs`; `PUT /admin/packs/{packId}` | Pacchetti |
| `GET`, `PUT /admin/factions`, `/admin/rarities`, `/admin/game-modes`, `/admin/missions`, `/admin/leagues` | Configurazione |
| `GET /admin/contact-messages`; `PUT /admin/contact-messages/{messageId}` | Messaggi del form Contatti |

Per caricare un file non si usa JSON: si manda un `FormData`. Quando arriverà, `api.js` avrà una funzione apposta.

---

## I dati per montare la carta

La carta la **monta il frontend** con un solo componente React, `Card` (#81). Il backend manda i dati e l'illustrazione; cornici, simboli e posizione dei testi sono file del frontend. Così, se cambia una statistica, la carta si aggiorna da sola.

### L'oggetto Carta

Arriva uguale ovunque ci sia una carta: Benvenuto, pacchetti, collezione, mazzi, scambi, partite. Quasi sempre dentro una voce, es. `{ "card": { ... } }` o `{ "card": { ... }, "quantity": 2 }`.

Esempio di una carta di prova come arriva oggi (l'id nel vostro database è diverso):

```json
{
  "id": "019a1f30-0000-7000-8000-000000000013",
  "code": "TEST-013",
  "baseCardId": null,
  "variantType": null,
  "name": "Sognatrice Lucida (prova)",
  "faction": { "code": "DREAMS", "name": "Dreams" },
  "rarity": { "code": "LEGENDARY", "name": "Leggendaria" },
  "attack": 6,
  "defense": 8,
  "description": null,
  "effect": {
    "code": "REST",
    "name": "Riposo",
    "value": 3,
    "text": "Quando viene giocata: il tuo giocatore guadagna 3 punti vita."
  },
  "illustrationUrl": null,
  "illustrationThumbUrl": null,
  "illustrationFocusY": 0.40,
  "lordTitle": null
}
```

E con il catalogo vero:

```json
"illustrationUrl": "/images/cards/base-004-v1.webp",
"illustrationThumbUrl": "/images/cards/base-004-v1-thumb.webp",
"illustrationFocusY": 0.27,
"lordTitle": "Dreams Lord"
```

### Campo per campo

| Campo | Tipo | Cosa ci fate | Può essere `null`? |
|-------|------|--------------|--------------------|
| `id` | UUID | Chiave nelle liste React (`key`), e il `cardId` da mandare per mazzi e scambi | No |
| `code` | testo | Codice della carta, es. `BASE-004`; la full art finisce con `-FA` | No |
| `baseCardId` | UUID | Per la full art: l'id della carta normale | Sì, nelle carte normali |
| `variantType` | testo | `"FULL_ART"` = carta senza cornice | Sì, nelle carte normali |
| `name` | testo | Nome sulla carta | No |
| `faction.code` | testo | Sceglie cornice e simbolo: `DREAMS` (luna), `NIGHTMARES` (occhio), `MEMORIES` (clessidra) | No |
| `faction.name` | testo | Nome da mostrare | No |
| `rarity.code` | testo | Sceglie la cornice: `COMMON`, `RARE`, `EPIC`, `LEGENDARY` | No |
| `rarity.name` | testo | "Comune", "Rara", "Epica", "Leggendaria" | No |
| `attack`, `defense` | numero | ATT e DIF | No |
| `description` | testo | Testo di atmosfera | Sì: allora non si mostra |
| `effect` | oggetto | Effetto della carta | Sì: carta senza effetto |
| `effect.code` | testo | Codice, es. `REST` (per un'icona, se ci sarà) | No |
| `effect.name` | testo | Nome, es. "Riposo" | No |
| `effect.value` | numero | Il numero X, es. 3 | Sì: effetti senza numero |
| `effect.text` | testo | Testo per il giocatore, **con il numero già dentro** | No |
| `illustrationUrl` | testo | Illustrazione grande (1024 px di larghezza) | Sì, finché non c'è |
| `illustrationThumbUrl` | testo | Miniatura (400 px) | Sì, insieme a quella grande |
| `illustrationFocusY` | numero da 0 a 1 | Quale fascia dell'immagine resta visibile nel ritaglio (0 = in alto, 1 = in basso). Di base 0,40 | No |
| `lordTitle` | testo | Solo per le tre carte Lord, es. "Dreams Lord" | Sì, nelle altre carte |

Da ricordare:

- **Full art:** è una carta a parte, con il suo `id`. Nome, testi, ATT, DIF, fazione, rarità ed effetto sono quelli della carta normale (il backend li manda già compilati). Cambiano solo `id`, `code` e l'illustrazione, verticale 2:3: copre tutta la carta, senza cornice, con i testi su pannelli scuri semitrasparenti.
- **Grande o miniatura:** miniatura nelle griglie e nelle liste (collezione, editor, scambi, campo di partita), grande per la carta da sola (dettaglio, apertura del pacchetto).
- **Illustrazione mancante:** oggi le carte di prova non hanno illustrazioni. Il componente deve mostrare un'immagine di riserva quando `illustrationUrl` è `null`.
- Le immagini restano in cache per sempre: quando un'immagine cambia, cambia anche il nome (`-v1` diventa `-v2`).

### Esempio di componente

Una base da cui partire per la #81. Le classi CSS e i nomi dei file delle cornici sono di esempio: li decide chi fa il componente.

```jsx
// Card.jsx: la carta montata dal frontend con i dati del backend
const FALLBACK_ART = "/img/illustrazione-mancante.webp";

export default function Card({ card, size = "thumb" }) {
  const isFullArt = card.variantType === "FULL_ART";
  const art = (size === "large" ? card.illustrationUrl : card.illustrationThumbUrl) ?? FALLBACK_ART;
  const frame = `/img/cornici/${card.faction.code.toLowerCase()}-${card.rarity.code.toLowerCase()}-${size}-v1.webp`;
  // Testo per chi usa un lettore di schermo
  const label = `${card.name}, ${card.faction.name}, ${card.rarity.name}, attacco ${card.attack}, difesa ${card.defense}`;

  return (
    <figure className={`card card--${size}${isFullArt ? " card--full-art" : ""}`} aria-label={label}>
      {/* 1. Illustrazione, ritagliata in verticale senza zoom */}
      <img
        className="card__art"
        src={art}
        alt=""
        loading="lazy"
        style={{ objectFit: "cover", objectPosition: `50% ${card.illustrationFocusY * 100}%` }}
      />
      {/* 2. Cornice sopra; la full art non ce l'ha */}
      {!isFullArt && <img className="card__frame" src={frame} alt="" />}
      {/* 3. Testi nelle loro zone */}
      <span className="card__name">{card.name}</span>
      {card.lordTitle && <span className="card__lord">{card.lordTitle}</span>}
      {card.effect && (
        <p className="card__effect">
          <strong>{card.effect.name}</strong> {card.effect.text}
        </p>
      )}
      {card.description && <p className="card__description">{card.description}</p>}
      <span className="card__attack">{card.attack}</span>
      <span className="card__defense">{card.defense}</span>
    </figure>
  );
}
```

Uso:

```jsx
// Apertura di un pacchetto: carte grandi
opening.cards.map((item, index) => <Card key={index} card={item.card} size="large" />);

// Collezione: miniature, con le copie
collection.items.map((item) => (
  <div key={item.card.id}>
    <Card card={item.card} />
    <span>x{item.quantity}</span>
  </div>
));
```

Nelle aperture si usa l'indice come `key`: la stessa carta può uscire due volte nello stesso pacchetto.

Cornici, simboli, ritaglio, pesi delle immagini e cache sono spiegati nella [guida ai dati, capitolo 7](guida-frontend.md#7-come-si-monta-una-carta).

---

## Tutti i codici di errore

**Pronti** (arrivano già oggi):

| `code` | Stato | Quando |
|--------|-------|--------|
| `VALIDATION_ERROR` | 400 | Campi sbagliati: guardate `fieldErrors` |
| `MALFORMED_REQUEST` | 400 | JSON illeggibile o id scritto male nell'indirizzo |
| `UNAUTHORIZED` | 401 | Token mancante o scaduto (ci pensa `api.js`) |
| `FORBIDDEN` | 403 | Non permesso, es. un giocatore nel backoffice |
| `NOT_FOUND` | 404 | Indirizzo che non esiste |
| `METHOD_NOT_ALLOWED` | 405 | Metodo sbagliato, es. `GET` dove serve `POST` |
| `CONFLICT` | 409 | Due richieste insieme sugli stessi dati: si riprova |
| `INTERNAL_ERROR` | 500 | Errore del server: "Riprova" |
| `USERNAME_TAKEN`, `EMAIL_TAKEN` | 409 | Username o email già usati |
| `INVALID_CREDENTIALS` | 401 | Accesso sbagliato |
| `ACCOUNT_DISABLED` | 401 | Account cancellato |
| `INVALID_REFRESH_TOKEN` | 401 | Sessione finita: rifare l'accesso |
| `WRONG_PASSWORD` | 400 | Password attuale sbagliata |
| `INVALID_RESET_TOKEN` | 400 | Link di recupero scaduto o già usato |
| `INVALID_FACTION` | 400 | Fazione che non esiste |
| `WELCOME_ALREADY_CLAIMED` | 409 | Fazione già scelta |
| `PACK_NOT_FOUND` | 404 | Pacchetto che non esiste o non si compra |
| `PACK_NOT_ON_SALE` | 409 | Pacchetto non più in vendita |
| `INVALID_COVER` | 400 | Copertina sbagliata |
| `NOT_ENOUGH_CREDITS` | 409 | Crediti che non bastano |
| `PACK_OPENING_NOT_FOUND` | 404 | Apertura che non esiste o di un altro utente |

**Solo nel frontend:** `NETWORK_ERROR` (stato 0), creato da `api.js` quando il backend non risponde.

**Da fare** (arriveranno con le loro issue): `CARDS_LOCKED_IN_TRADE`, `NO_OPPONENT_FOUND`, `ENGINE_UNAVAILABLE`, e quelli di mazzi, amici e scambi.

---

## Controllo finale

Prima di aprire una PR con una pagina che usa le API:

- [ ] Le chiamate passano tutte da `api.js`: nessun `fetch()` nella pagina.
- [ ] Il token non è mai in `localStorage` o `sessionStorage`.
- [ ] La pagina mostra il caricamento e gli errori (`message`, e `fieldErrors` accanto ai campi).
- [ ] Le `POST`, `PUT` e `DELETE` partono solo da un clic, con il pulsante spento mentre la richiesta è in corso.
- [ ] Prezzi, percentuali, limiti dei mazzi e nomi vengono dalle API, non sono scritti a mano.
- [ ] Le date sono mostrate nell'ora italiana.
- [ ] Nessun dato personale negli indirizzi.
- [ ] Provata con il backend acceso e un utente di prova.
