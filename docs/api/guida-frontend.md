# Guida per il frontend · Dreamscape

Per il team **WMD**. Qui c'è tutto quello che serve per collegare le pagine al backend, in un solo file:

1. **Avviare il progetto:** i programmi da installare, come scaricare il progetto e avviare backend e frontend.
2. **Come si parla con il backend:** le regole comuni delle API, l'accesso, il file `api.js` e come usarlo nelle pagine.
3. **Pagine e dati:** gli oggetti che arrivano, le chiamate pagina per pagina, come si monta la carta, l'elenco delle pagine.
4. **Riferimento:** l'elenco di tutti gli endpoint con il loro stato, tutti i codici di errore e il controllo prima della PR.

I dati vengono dallo [schema del database](../database/schema.md) e i nomi delle API seguono [naming.md](../naming.md). L'elenco definitivo, da provare dal browser, è **Swagger**: `http://localhost:8080/swagger-ui.html` con il backend acceso.

**Come leggere lo stato degli endpoint** ([capitolo 14](#14-elenco-completo-degli-endpoint)):

| Stato | Cosa vuol dire |
|-------|----------------|
| **Pronto** | È in `develop` e ha i test: si può usare subito, i dati sono quelli scritti qui. |
| **Da fare** | Non esiste ancora. Quello che c'è scritto è la proposta già approvata: si possono preparare le pagine, ma qualche nome può cambiare. Quando arriva, lo stato diventa "Pronto". |

Se qualcosa non torna, o vi serve un dato che non c'è, ditelo a Marco o Cristian.

## Indice

**Parte 1 · Avviare il progetto**

1. [Installare i programmi](#1-installare-i-programmi)
2. [Scaricare il progetto](#2-scaricare-il-progetto)
3. [Avviare il backend](#3-avviare-il-backend)
4. [Avviare il frontend collegato al backend](#4-avviare-il-frontend-collegato-al-backend)

**Parte 2 · Come si parla con il backend**

5. [Come funziona in breve](#5-come-funziona-in-breve)
6. [Regole comuni delle API](#6-regole-comuni-delle-api)
7. [Accesso con JWT](#7-accesso-con-jwt)
8. [Il file api.js](#8-il-file-apijs)
9. [Usare api.js nelle pagine](#9-usare-apijs-nelle-pagine)

**Parte 3 · Pagine e dati**

10. [Gli oggetti che ricevete](#10-gli-oggetti-che-ricevete)
11. [Pagine e chiamate](#11-pagine-e-chiamate)
12. [Come si monta una carta](#12-come-si-monta-una-carta)
13. [Elenco delle pagine per la sitemap](#13-elenco-delle-pagine-per-la-sitemap)

**Parte 4 · Riferimento**

14. [Elenco completo degli endpoint](#14-elenco-completo-degli-endpoint)
15. [Tutti i codici di errore](#15-tutti-i-codici-di-errore)
16. [Regole da ricordare](#16-regole-da-ricordare)
17. [Cose ancora da decidere](#17-cose-ancora-da-decidere)
18. [Controllo finale prima della PR](#18-controllo-finale-prima-della-pr)

---

## 1. Installare i programmi

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

## 2. Scaricare il progetto

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

## 3. Avviare il backend

1. Aprite **Docker Desktop** e aspettate **Engine running**.
2. Nel terminale, dalla cartella del progetto:

   ```bash
   cd sviluppo/backend
   cp .env.example .env
   ./gradlew bootRun
   ```

   `cp .env.example .env` si fa **solo la prima volta**: crea il file con i dati del database del vostro computer. Le volte dopo bastano `cd sviluppo/backend` e `./gradlew bootRun`.

3. La prima volta ci vogliono alcuni minuti: si scaricano Gradle, le librerie e le immagini di PostgreSQL e Mailpit, e si costruisce il container del motore di gioco (nel terminale scorrono molte righe che iniziano con `#`: è normale). Serve internet. Le volte dopo il motore si ricostruisce in pochi secondi.
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

Sono password di prova, valide solo sul database del vostro computer. Le carte di prova si chiamano "(prova)" e **non hanno ancora le illustrazioni**: arrivano con `illustrationUrl: null` (vedi [Gli oggetti che ricevete](#10-gli-oggetti-che-ricevete)).

---

## 4. Avviare il frontend collegato al backend

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
| `./gradlew bootRun` dà un errore che parla di **Java**, di **toolchain** o di **languageVersion 25** | Manca Java 25 o il terminale non lo vede. Controllate `java -version` (1.6); se serve reinstallate Java 25 (1.2) e riaprite il terminale |
| `./gradlew bootRun` dà un errore che parla di **Docker** | Docker Desktop è chiuso o non ancora pronto: apritelo, aspettate **Engine running** e riprovate |
| Sul Mac `docker --version` funziona ma il backend dice che **non trova `docker`** | In Docker Desktop: **Settings** (l'ingranaggio) → **Advanced** → scegliete **System** → **Apply & restart**. Poi riaprite il terminale |
| Dopo un `git pull`, `./gradlew bootRun` si ferma con **Validate failed** o **Migrations have failed validation** | È arrivata una migration nuova e il database di sviluppo è vecchio. Da `sviluppo/backend`: `docker compose down -v`, poi di nuovo `./gradlew bootRun`. Il database di sviluppo si ricrea con i dati di prova (quello che avevate scritto a mano si perde) |
| **Port 8080 was already in use** | Il backend è già acceso in un altro terminale: usate quello, oppure fermatelo con Ctrl+C |
| **8081** già in uso, oppure un errore che parla del servizio **engine** | La porta 8081 serve al motore di gioco: chiudete il programma che la usa. Se l'errore è nella costruzione del motore, da `sviluppo/backend` scrivete `docker compose build engine` e mandate a Marco o Cristian le ultime righe |
| `./gradlew: Permission denied` (Mac) | Scrivete `chmod +x gradlew` una volta, poi riprovate |
| `npm` o `node`: **command not found** | Chiudete e riaprite il terminale. Se non basta, reinstallate Node.js (1.4) |
| Vite dice che serve **Node.js 20.19+ o 22.12+** | Node.js è vecchio: installate la LTS (1.4) |
| Nel terminale di Vite compare **ECONNREFUSED** o **proxy error** | Il backend è spento: avviatelo (capitolo 3) |
| Dopo l'accesso, ricaricando la pagina si torna al login | Il cookie non si è salvato: usate Chrome o Firefox (4.2) |
| Su Windows Docker Desktop dice che manca **WSL** | PowerShell come amministratore → `wsl --install` → riavviate (1.3) |

Se dopo questi passi qualcosa ancora non va, mandate a Marco o Cristian la foto dell'errore con le ultime righe del terminale.

---

## 5. Come funziona in breve

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

## 6. Regole comuni delle API

| Cosa | Regola | Esempio |
|------|--------|---------|
| Indirizzo base | Tutte le API partono da `/api/v1`, sullo stesso indirizzo del sito: uguale in locale e alla consegna. Negli indirizzi non si scrive mai `http://localhost:8080` | `/api/v1/decks` |
| Id | Ogni id è un **UUID**: un testo di 36 caratteri. Si usa così come arriva, anche negli URL; non si calcola e non si confronta come un numero | `"019a1f2e-7c41-7d3a-9b2e-5f6a7b8c9d01"` |
| Formato | JSON in entrata e in uscita, nomi in `camelCase` | `{ "totalCards": 20, "isValid": true }` |
| Valori mancanti | I campi senza valore arrivano come `null`, non spariscono | `"imageUrl": null` |
| Risorse | In inglese, al plurale, il verbo è il metodo HTTP | `GET /decks`, `POST /decks`, `DELETE /decks/{deckId}` |
| Date | Testo ISO 8601 in UTC: va convertito nell'ora italiana per mostrarlo | `"2026-10-26T14:30:00Z"` |
| Immagini | URL relativi, in WebP: si mettono così come sono in `src` o si usano nel disegno della carta. Il browser le tiene in cache ([capitolo 12](#12-come-si-monta-una-carta)) | `"/images/cards/base-004-v1.webp"` |
| Liste lunghe | Con paginazione: `page` (da 0) e `size` | `GET /me/cards?page=0&size=24` |

Una lista paginata arriva così:

```json
{ "items": [ ], "page": 0, "size": 24, "totalItems": 57, "totalPages": 3 }
```

**Le cinque regole del codice**

1. Tutte le chiamate passano da **un solo file**, `api.js` ([capitolo 8](#8-il-file-apijs)). Mai `fetch()` nelle pagine o nei componenti.
2. Il **server decide**: contenuto dei pacchetti, esito delle partite, validità dei mazzi, scambi. Il frontend mostra quello che riceve.
3. **I controlli si fanno due volte:** nel frontend per aiutare l'utente (campo vuoto, password corta), nel backend per sicurezza.
4. **Mai dati personali negli indirizzi**: niente email o password nella barra degli indirizzi o nei parametri `?`.
5. **Niente dati finti** nel frontend: le pagine usano solo le API.

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

- `message` è già in italiano: si può mostrare così com'è.
- `code` serve al codice per capire cosa è successo (es. `NOT_ENOUGH_CREDITS`): mai confrontare il testo di `message`.
- `fieldErrors` è sempre una lista, vuota se l'errore non riguarda un campo. Ogni voce va mostrata accanto al suo campo.
- Solo il mazzo non valido (`INVALID_DECK`) ha anche `problems`, con tutti i problemi del mazzo ([Mazzi](#mazzi)).

| Stato HTTP | Quando | Cosa fa la pagina |
|------------|--------|-------------------|
| `200`, `201` | Tutto bene (`201` = creato) | Mostra il risultato |
| `202`, `204` | Tutto bene, nessun dato o richiesta presa in carico | Mostra una conferma |
| `400` | Dati sbagliati (campo vuoto, password troppo corta, mazzo non valido) | Mostra `fieldErrors` accanto ai campi, oppure `message` |
| `401` | Token mancante o scaduto | Ci pensa `api.js`: rinnova il token o porta all'accesso |
| `403` | Accesso non permesso (es. un giocatore nel backoffice) | Mostra "non hai i permessi" |
| `404` | Non esiste (carta, mazzo, scambio...) | Pagina o messaggio "non trovato" |
| `409` | Conflitto (username già usato, crediti insufficienti, carte non più possedute) | Mostra `message` |
| `503` | Servizio momentaneamente non disponibile (es. il motore di gioco) | Mostra `message` e invita a riprovare |

L'elenco di tutti i codici (`code`) è nel [capitolo 15](#15-tutti-i-codici-di-errore).

---

## 7. Accesso con JWT

Il backend usa i **token JWT**. Ci sono due token:

| Token | A cosa serve | Quanto dura | Dove sta |
|-------|--------------|-------------|----------|
| **Token di accesso** (`accessToken`) | Va in **ogni** richiesta che richiede l'accesso | **15 minuti** | In una variabile JavaScript, **non** in `localStorage` |
| **Refresh token** | Serve a ottenere un nuovo token di accesso | **7 giorni** | In un **cookie `HttpOnly`** che il browser gestisce da solo: il JavaScript non lo vede e non lo tocca |

Perché non `localStorage`: se una pagina avesse un bug di sicurezza (XSS), uno script estraneo potrebbe leggere il token. Il cookie `HttpOnly` non è leggibile dal JavaScript.

Il sito è fatto di più pagine HTML, quindi la variabile si perde a ogni cambio pagina. Non è un problema: all'apertura di ogni pagina `api.js` chiama `/auth/refresh` e ottiene un token nuovo dal cookie (vedi il [capitolo 8](#8-il-file-apijs)).

### Le chiamate dell'accesso

| Azione | Chiamata | Cosa mandate | Cosa ricevete |
|--------|----------|--------------|---------------|
| Registrazione | `POST /auth/register` | `username`, `email`, `password`, `privacyAccepted`, `termsAccepted` | `201` + i dati dell'utente. Poi si fa l'accesso |
| Accesso | `POST /auth/login` | `login` (username **o** email), `password` | `accessToken` + dati dell'utente; il refresh token arriva nel cookie |
| Accesso con Google | `POST /auth/google` | `idToken` (dal pulsante di Google); per registrarsi anche `username`, `privacyAccepted`, `termsAccepted` | `200` come l'accesso, `201` se l'account è nuovo, `202` se serve la registrazione (vedi sotto) |
| Nuovo token di accesso | `POST /auth/refresh` | Niente: il cookie parte da solo | Un nuovo `accessToken` |
| Uscita | `POST /auth/logout` | Niente | `204`; il backend revoca il refresh token e cancella il cookie |
| Chi sono | `GET /me` | — | I dati dell'utente collegato |
| Modifica profilo | `PUT /me` | `email` | L'utente aggiornato |
| Cambio password | `PUT /me/password` | `currentPassword`, `newPassword` | `204`; per sicurezza gli altri dispositivi vengono scollegati |
| Password dimenticata (facoltativa) | `POST /auth/password-reset-requests` | `email` | Sempre `202`, anche se l'email non esiste (per non rivelare chi è registrato) |
| Nuova password (facoltativa) | `POST /auth/password-resets` | `token` (dal link dell'email), `newPassword` | `204` |

#### Accesso con Google

Il pulsante "Continua con Google" usa la libreria ufficiale di Google (Google Identity Services), che dà al JavaScript un **id token**. Il frontend lo manda al backend, che lo verifica e risponde come `POST /auth/login`: stesso `accessToken`, stesso cookie. Il client id di Google lo dà SWD (decisione 032).

1. `POST /auth/google` con `{ "idToken": "…" }`.
2. Le risposte possibili:

| Stato | Quando | Cosa fa il frontend |
|-------|--------|---------------------|
| `200` | L'account Google è già collegato, oppure c'è già un account con la stessa email (verificata da Google), che viene collegato | Come dopo `POST /auth/login` |
| `202` | Nessun account Dreamscape: serve la registrazione. Arriva `{ "signupRequired": true, "email": "mario.rossi@gmail.com", "suggestedUsername": "mario_rossi" }` | Mostra un piccolo form con l'username proposto (modificabile) e le caselle di Privacy e Termini, poi rimanda `POST /auth/google` con `idToken`, `username`, `privacyAccepted` e `termsAccepted` |
| `201` | L'account è stato creato (500 DC di registrazione): la persona è già dentro | Come dopo l'accesso, poi pagina **Benvenuto** |

L'id token di Google scade dopo circa un'ora: tra il `202` e il secondo invio si può riusare lo stesso. Se è scaduto si ripremi il pulsante.

Errori: `401 INVALID_GOOGLE_TOKEN` (token non valido o scaduto), `403 GOOGLE_EMAIL_NOT_VERIFIED`, `409 GOOGLE_ACCOUNT_CONFLICT` (l'account con quell'email è collegato a un altro account Google), `409 USERNAME_TAKEN`, `400 VALIDATION_ERROR` (username non valido o consensi mancanti), `401 ACCOUNT_DISABLED`, `503 GOOGLE_LOGIN_DISABLED` (manca il client id: si nasconde il pulsante).

Chi si è registrato con Google **non ha una password**: `POST /auth/login`, `PUT /me/password` e `DELETE /me` non funzionano (`PUT` e `DELETE` rispondono `400 PASSWORD_NOT_SET`). La password si sceglie con "Password dimenticata", e da lì si entra in tutti e due i modi.

### Le chiamate dell'accesso nel dettaglio

Per ognuna: cosa **mandate**, cosa **ricevete** e gli errori possibili.

#### Registrazione: `POST /auth/register`

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

#### Accesso: `POST /auth/login`

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

#### Token nuovo: `POST /auth/refresh`

Lo chiama solo `api.js`. Non si manda niente: parte il cookie.

Ricevete `200` e un cookie nuovo:

```json
{ "accessToken": "eyJhbGciOi...", "expiresIn": 900 }
```

Errore: `401 INVALID_REFRESH_TOKEN` (cookie mancante, scaduto o revocato): l'utente deve rifare l'accesso.

#### Uscita: `POST /auth/logout`

Con `logout()` di `api.js`. Non si manda niente. Ricevete `204`: il backend revoca il refresh token e cancella il cookie. Poi si va alla Home.

#### Password dimenticata: `POST /auth/password-reset-requests`

Mandate:

```json
{ "email": "lucida@example.com" }
```

Ricevete **sempre** `202`, anche se l'email non è registrata: così nessuno può scoprire chi è iscritto. La pagina mostra sempre lo stesso messaggio, es. "Se l'email è registrata, ti abbiamo mandato un link".

L'email contiene un link a `nuova-password.html?token=...` (indirizzo da confermare con chi fa la pagina). Il link vale **1 ora** e una volta sola. In sviluppo l'email si legge in Mailpit, `http://localhost:8025`.

#### Nuova password: `POST /auth/password-resets`

La pagina legge il token dall'indirizzo e lo manda con la password nuova:

```js
const token = new URLSearchParams(window.location.search).get("token");
await api("/auth/password-resets", { method: "POST", body: { token, newPassword } });
```

Ricevete `204`. Tutti i dispositivi vengono scollegati: si va all'accesso.

Errore: `400 INVALID_RESET_TOKEN` (link scaduto o già usato): si propone di chiederne uno nuovo.

#### Profilo: `GET /me`

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

#### Cambio email: `PUT /me`

Mandate:

```json
{ "email": "nuova@example.com" }
```

Ricevete `200` con il profilo aggiornato (stessa forma di `GET /me`). Lo username non si cambia.

Errori: `409 EMAIL_TAKEN`, `400 VALIDATION_ERROR`.

#### Cambio password: `PUT /me/password`

Mandate:

```json
{ "currentPassword": "una-password-lunga", "newPassword": "una-password-nuova" }
```

Ricevete `204`. Gli **altri** dispositivi vengono scollegati; questo resta collegato.

Errore: `400 WRONG_PASSWORD` (password attuale sbagliata).

#### Cancellazione dell'account: `DELETE /me`

Mandate:

```json
{ "password": "una-password-lunga" }
```

Ricevete `204` e il cookie viene cancellato: si va alla Home. L'account diventa anonimo e non si può recuperare: **chiedete conferma prima**.

Errore: `400 WRONG_PASSWORD`.

Da sapere:

- `/auth/refresh` risponde `{ "accessToken": "…", "expiresIn": 900 }` e manda un refresh token **nuovo** nel cookie: quello vecchio non vale più. Il cookie si chiama `dreamscape_refresh` e vale solo per `/api/v1`.
- Il primo accesso del giorno (ora italiana), con il login o con il refresh all'apertura di una pagina, dà i **50 DC** della ricompensa giornaliera: si vedono in `GET /me/credit-transactions`.

Codici di errore dell'accesso:

| `code` | Stato | Quando |
|--------|-------|--------|
| `USERNAME_TAKEN`, `EMAIL_TAKEN` | 409 | Registrazione o cambio email con dati già usati (anche in `fieldErrors`) |
| `INVALID_CREDENTIALS` | 401 | Login sbagliato: stesso messaggio se manca l'utente o la password è errata |
| `INVALID_REFRESH_TOKEN` | 401 | Cookie mancante, scaduto o revocato: si va alla pagina di accesso |
| `ACCOUNT_DISABLED` | 401 | L'account è stato cancellato |
| `WRONG_PASSWORD` | 400 | Password attuale sbagliata nel cambio password o nella cancellazione dell'account |
| `INVALID_RESET_TOKEN` | 400 | Link di recupero scaduto o già usato: si chiede un link nuovo |
| `PASSWORD_NOT_SET` | 400 | Account registrato con Google senza password: cambio password e cancellazione chiedono di sceglierla prima con "Password dimenticata" |
| `INVALID_GOOGLE_TOKEN`, `GOOGLE_EMAIL_NOT_VERIFIED`, `GOOGLE_ACCOUNT_CONFLICT`, `GOOGLE_LOGIN_DISABLED` | 401, 403, 409, 503 | Accesso con Google: vedi [Accesso con Google](#accesso-con-google) |

---

## 8. Il file api.js

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

---

## 9. Usare api.js nelle pagine

### 9.1 All'apertura del sito: l'utente è ancora collegato?

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

Il `refresh()` restituisce solo il token, non il saldo: dopo un ricaricamento i crediti per l'intestazione si leggono con [`GET /me/summary`](#dashboard).

### 9.2 Leggere dati: GET in un useEffect

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

### 9.3 Mandare dati: un form con gli errori accanto ai campi

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

### 9.4 Azioni che cambiano dati: comprare un pacchetto

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

### 9.5 Liste a pagine: i crediti

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

### 9.6 Date e numeri

```js
// "2026-11-11T10:15:00Z" -> "11/11/2026, 11:15"
const formatDate = (iso) =>
  new Date(iso).toLocaleString("it-IT", { timeZone: "Europe/Rome", dateStyle: "short", timeStyle: "short" });

// 1000 -> "1.000"
const formatCredits = (credits) => credits.toLocaleString("it-IT");
```

---

---

## 10. Gli oggetti che ricevete

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
- `illustrationUrl` è la copia **grande** (larghezza 1024 px), `illustrationThumbUrl` la **miniatura** (larghezza 400 px): quale usare è spiegato nel [capitolo 12](#12-come-si-monta-una-carta).
- `illustrationFocusY` dice quale fascia dell'immagine resta visibile nel ritaglio: 0 = in alto, 1 = in basso (quasi sempre 0,40).
- **Full art:** è una carta a parte, con il suo `id` e il suo codice (`BASE-004-FA`): `variantType` è `"FULL_ART"` e `baseCardId` è l'id della carta normale. Nome, testi, ATT, DIF, fazione, rarità ed effetto sono quelli della carta normale (il backend li manda già compilati); cambiano solo `id`, `code` e l'illustrazione, verticale 2:3. Oggi solo le Leggendarie hanno la full art. Domani le skin arriveranno nello stesso modo, con un altro `variantType`.
- `lordTitle` c'è solo per le tre carte Lord.
- Codici delle rarità: `COMMON`, `RARE`, `EPIC`, `LEGENDARY`. Codici delle fazioni: `DREAMS`, `NIGHTMARES`, `MEMORIES`.

### La carta campo per campo

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

Se `illustrationUrl` è `null` (le carte di prova `TEST-` non hanno illustrazioni), il componente mostra un'immagine di riserva.

### Carta della collezione

```json
{ "card": { "id": "019a1f30-0000-7000-8000-000000000004", "name": "Il Sogno Lucido", "...": "..." }, "quantity": 2, "availableForTrade": 1 }
```

`availableForTrade` sono le copie che si possono offrire in uno scambio: `quantity` meno quelle già offerte negli scambi in attesa (vedi [Scambi](#scambi)). Nei mazzi si usano tutte le `quantity` copie.

Ogni versione è una voce a parte: se l'utente ha anche la full art del Sogno Lucido, nella collezione c'è un'altra voce con `card.variantType: "FULL_ART"`, il suo `card.id` e le sue copie.

### Utente (in liste, amici, sfide)

```json
{ "id": "019a1f2e-8d52-7e4b-8c3f-6a7b8c9d0e12", "username": "incubo_99" }
```

Degli altri giocatori si vede solo lo username: mai email o crediti.

---

## 11. Pagine e chiamate

Per ogni pagina: le chiamate da fare e cosa mostrare. 🔓 = pagina pubblica, 🔑 = serve l'accesso, 🛠️ = solo admin.

### Pagine pubbliche 🔓

| Pagina | Chiamate | Cosa mostra |
|--------|----------|-------------|
| Home | `GET /cards?featured=true` | Alcune carte del catalogo come anteprima |
| Il gioco / Lore | `GET /factions` | Le tre fazioni con forze, debolezze e il loro Lord |
| Regolamento | `GET /rules` | I numeri del regolamento, sempre aggiornati |
| Contatti | `POST /contact-messages` | Form: `name`, `email`, `message` → `201` e messaggio di conferma |
| Chi siamo, FAQ, Glossario, Privacy, Cookie, Termini, 404 | Nessuna | Pagine statiche |

Il catalogo `GET /cards` è una lista paginata (`{ items, page, size, totalItems, totalPages }`) di oggetti [Carta](#carta), in ordine di codice. Ci sono solo le carte normali attive: niente full art e niente carte disattivate. Parametri, tutti facoltativi:

| Parametro | Cosa fa | Esempio |
|-----------|---------|---------|
| `faction` | Solo le carte di una fazione | `DREAMS` |
| `rarity` | Solo le carte di una rarità | `RARE` |
| `search` | Parte del nome, senza badare a maiuscole e minuscole (al massimo 80 caratteri) | `sogno` |
| `featured` | `true` = solo le carte in evidenza della Home: le Leggendarie, Lord compresi | `true` |
| `page`, `size` | Pagina (da 0) e carte per pagina (da 1 a 100, di solito 24) | `page=0&size=24` |

Un codice di fazione o di rarità che non esiste restituisce una pagina vuota. `GET /cards/{cardId}` restituisce una sola carta, anche una full art o una carta disattivata (chi la possiede ne vede ancora il dettaglio); se non esiste risponde `404` con `CARD_NOT_FOUND`.

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
    { "openingId": "019a2b40-0000-7000-8000-000000000101", "packName": "Welcome Pack", "cards": [ { "card": { "...": "..." } } ] },
    { "openingId": "019a2b40-0000-7000-8000-000000000102", "packName": "Welcome Pack Epico", "cards": [ { "card": { "...": "..." } } ] }
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

- È anche il modo per avere i crediti dopo un ricaricamento della pagina: `GET /me` non li ha.
- `latestCards` sono le carte dell'ultima apertura (comprata o regalata), nell'ordine di uscita, con la stessa forma di `GET /pack-openings/{openingId}`. Lista vuota se il giocatore non ha mai aperto pacchetti.
- `league` è la lega del punteggio attuale; nella lega più alta `nextLeagueName` e `nextLeagueMinRating` sono `null`.
- `pendingFriendRequests`, `pendingTrades` e `pendingChallenges` servono per il contatore nel menu: non ci sono altre notifiche.
- `pendingFriendRequests` sono le richieste di amicizia ricevute in attesa, `pendingTrades` gli scambi ricevuti in attesa di una risposta, `pendingChallenges` le sfide ricevute in attesa più quelle lanciate con un mazzo da sistemare. Per ora `missions` è una lista vuota: si riempie quando sono pronte le missioni (#125). La forma della risposta non cambia, quindi la pagina si può già fare tutta.
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

| `type` | Da mostrare come |
|--------|------------------|
| `SIGNUP_BONUS` | Regalo di benvenuto |
| `DAILY_REWARD` | Ricompensa giornaliera |
| `PACK_PURCHASE` | Acquisto pacchetto |
| `MATCH_REWARD` | Vittoria in partita |
| `MISSION_REWARD` | Missione completata |
| `LEAGUE_REWARD` | Premio di lega |
| `ADMIN_ADJUSTMENT` | Correzione dell'amministratore |

`amount` è negativo per le spese. `page` parte da 0, `size` va da 1 a 100 (di base 20); i movimenti arrivano dal più recente. Il riquadro "Acquista crediti" con denaro vero va mostrato solo come "in arrivo": in fase 1 non c'è.

#### Negozio e apertura dei pacchetti

`GET /packs` → i pacchetti in vendita:

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
      { "cardCount": 1, "rarities": [ { "code": "RARE", "chance": 70.0 }, { "code": "EPIC", "chance": 25.0 }, { "code": "LEGENDARY", "chance": 5.0 } ] }
    ]
  }
]
```

- Arrivano solo i pacchetti **in vendita in questo momento**, dal più economico. Set iniziale e pacchetti di benvenuto non ci sono: si ricevono nella pagina Benvenuto.
- `imageUrl` è l'immagine del pacchetto (Dream Pack il retro delle carte, i pacchetti di fazione il loro Lord). Finché le immagini non ci sono arriva `null`: si mostra un'immagine generica del pacchetto.
- Oggi nessun pacchetto ha copertine da scegliere ([decisione 030](../decisioni/030-nuovi-pacchetti.md)): `covers` arriva vuoto. Se in futuro un pacchetto ne avrà più di una, il giocatore ne sceglie una e il contenuto resta lo stesso.
- `slots` serve a mostrare "3 Comuni, 1 Rara, 1 carta speciale: Rara 70%, Epica 25%, Leggendaria 5% (di cui full art 1%)". Le rarità di uno slot arrivano dalla più comune alla più rara; `chance` è già in percentuale.
- `fullArtChance` è la percentuale delle **Leggendarie** che escono in full art, non del pacchetto: con 5% di Leggendaria e 20 di `fullArtChance` la full art esce nell'1% dei pacchetti (5 × 20 / 100).
- Nel negozio ci sono **sei pacchetti** ([decisione 030](../decisioni/030-nuovi-pacchetti.md)): Nap Pack (300 DC), Dream Pack (500), Faceless, Lucid e Still Time Pack (600, uno per fazione), REM Pack (1.000). Nomi, prezzi e percentuali si leggono sempre da `GET /packs`, mai scritti a mano nel frontend.
- `factionCode` dice se il pacchetto ha carte di una sola fazione (`null` = tutte); `availableUntil` (UTC) se è in vendita solo fino a una data (`null` = nessun limite). Nei pacchetti di fazione `fullArtChance` è 10.

Acquisto: `POST /packs/{packId}/open`, senza corpo (un pacchetto con più copertine vorrebbe `{ "coverId": "..." }`, con l'id di una di `covers`). Risponde `201`:

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

La collezione è una lista paginata di voci [Carta della collezione](#carta-della-collezione), in ordine di codice: la full art viene subito dopo la sua carta normale (`BASE-004`, `BASE-004-FA`). Ci sono anche le carte disattivate: chi le possiede le tiene. I parametri sono tutti facoltativi e funzionano come in `GET /cards`; fazione, rarità e nome di una full art sono quelli della sua carta normale. `fullArt=true` mostra solo le full art, `fullArt=false` solo le carte normali, senza il parametro tutte.

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
  "isRanked": false,
  "problems": [],
  "cards": [ { "card": { "...": "..." }, "quantity": 2 } ]
}
```

`GET /decks` restituisce i mazzi in ordine di nome, ognuno in questa forma; le carte di un mazzo sono in ordine di codice. `POST /decks` risponde `201`, `PUT /decks/{deckId}` `200` e `DELETE /decks/{deckId}` `204`. Le carte di `PUT` sostituiscono quelle di prima. Se nella richiesta la stessa carta compare due volte, le copie si sommano.

Errori dei mazzi:

| Codice | Stato | Quando |
|--------|-------|--------|
| `INVALID_DECK` | `400` | Il mazzo non rispetta le regole. `message` è il primo problema, `problems` li elenca tutti |
| `VALIDATION_ERROR` | `400` | Nome vuoto o più lungo di 40 caratteri, carte mancanti, copie minori di 1 (con `fieldErrors`) |
| `DECK_NOT_FOUND` | `404` | Il mazzo non esiste o è di un altro giocatore |
| `DECK_NAME_TAKEN` | `409` | Hai già un altro mazzo con questo nome (con `fieldErrors` su `name`) |

Un mazzo non valido al salvataggio:

```json
{
  "status": 400,
  "code": "INVALID_DECK",
  "message": "Il mazzo ha 18 carte: ne servono 20.",
  "fieldErrors": [],
  "problems": [ "Il mazzo ha 18 carte: ne servono 20.", "Le Rare sono 9: il massimo è 8." ]
}
```

`problems` c'è solo in questo errore. Le frasi sono le stesse di `problems` nella lista dei mazzi, per esempio:

- `"Il mazzo ha 18 carte: ne servono 20."`
- `"Le copie di Il Sogno Lucido sono 2: il massimo è 1 (normale e full art contano insieme)."`
- `"Le Rare sono 9: il massimo è 8."`
- `"Non possiedi la carta Lucciola."` / `"Possiedi 1 copia di Lucciola: nel mazzo ce ne sono 2."`
- `"La carta Il Faro nella Nebbia non è più disponibile."`
- `"Una delle carte scelte non esiste."`

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

Cosa si riceve dalle altre chiamate:

| Chiamata | Mandate | Ricevete |
|----------|---------|----------|
| `GET /users?search=luc` | | Lista di `{ "id", "username" }`, al massimo 20, in ordine di username. Da 2 caratteri in su; non ci sono chi cerca, gli admin e gli account cancellati |
| `POST /friend-requests` | `{ "username": "lucida" }` | `201` con la richiesta. Se lucida vi aveva già chiesto l'amicizia, siete subito amici: `200` con l'amico |
| `GET /friend-requests?direction=received` (o `sent`) | | Lista di richieste in attesa, dalla più recente: `{ "id", "user": { "id", "username" }, "createdAt" }`. `user` è l'altro giocatore |
| `POST /friend-requests/{requestId}/accept` | | `200` con l'amico (solo chi ha ricevuto la richiesta) |
| `DELETE /friend-requests/{requestId}` | | `204`: rifiuta se l'avete ricevuta, annulla se l'avete mandata |
| `DELETE /friends/{userId}` | | `204`; annulla anche scambi e sfide in attesa tra voi due |

- Lo username si cerca senza distinguere maiuscole e minuscole.
- Errori di `POST /friend-requests`, con l'errore anche sul campo `username` (`fieldErrors`): `USER_NOT_FOUND` (404), `CANNOT_FRIEND_YOURSELF` (400), `ALREADY_FRIENDS` (409), `FRIEND_REQUEST_ALREADY_SENT` (409).
- Una richiesta che non esiste o non vi riguarda: `404 FRIEND_REQUEST_NOT_FOUND`. Togliere chi non è un amico: `404 FRIEND_NOT_FOUND`.
- Se un giocatore cancella l'account, sparisce dalla lista amici e dalle richieste, e gli scambi in attesa con lui si annullano.

#### Scambi

Si scambia **solo con gli amici**. Quantità e rarità sono libere (es. 1 Epica per 5 Comuni): decide l'amico accettando o rifiutando.

| Azione | Chiamata |
|--------|----------|
| Ricevuti / Inviati / Storico | `GET /trades?box=received`, `?box=sent`, `?box=history` |
| Dettaglio | `GET /trades/{tradeId}` |
| Proponi | `POST /trades` |
| Accetta / Rifiuta (destinatario) | `POST /trades/{tradeId}/accept`, `POST /trades/{tradeId}/reject` |
| Annulla (chi propone) | `POST /trades/{tradeId}/cancel` |
| Avvisi prima di proporre / di accettare | `POST /trades/preview`, `GET /trades/{tradeId}/deck-warnings` |
| Collezione di un amico | `GET /users/{userId}/cards` (stessi filtri e pagine di `GET /me/cards`) |

Uno scambio, come arriva da tutte le chiamate:

```json
{
  "id": "019a2d10-1b2c-7d3e-8f40-a1b2c3d4e5f6",
  "status": "PENDING",
  "proposer":  { "id": "019a1f2e-7c41-7d3a-9b2e-5f6a7b8c9d01", "username": "lucida" },
  "recipient": { "id": "019a1f2e-8d52-7e4b-8c3f-6a7b8c9d0e12", "username": "incubo_99" },
  "message": "Ti va?",
  "offered":   [ { "card": { "...": "..." }, "quantity": 1 } ],
  "requested": [ { "card": { "...": "..." }, "quantity": 5 } ],
  "createdAt": "2026-11-17T18:11:00Z",
  "resolvedAt": null
}
```

- `card` è una [Carta](#carta) completa. `message` è `null` se non c'è; `resolvedAt` è `null` finché lo scambio è in attesa.
- `GET /trades?box=` è una lista a pagine (`page`, `size`, di base 20), dal più recente: `received` = in attesa di una vostra risposta, `sent` = proposti da voi e in attesa, `history` = chiusi, da tutti e due i lati.
- `POST /trades` risponde `201` con lo scambio; `accept`, `reject` e `cancel` rispondono `200` con lo scambio aggiornato.

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
- **Copie bloccate:** le copie che offri in uno scambio in attesa non le puoi offrire in un altro scambio finché il primo non si chiude (accettato, rifiutato o annullato). Restano usabili nei mazzi. Nella collezione per gli scambi si mostrano le copie disponibili: `availableForTrade` in ogni voce di `GET /me/cards`, come `quantity` ma senza quelle già offerte. Le copie che chiedi all'amico non si bloccano.
- **Collezione dell'amico:** `GET /users/{userId}/cards` funziona solo con gli amici (`403 NOT_FRIENDS` con gli altri). Anche lì `availableForTrade` dice quante copie l'amico può darvi: si chiedono al massimo quelle.
- **Errori della proposta:**

  | `code` | Stato | Quando |
  |--------|-------|--------|
  | `VALIDATION_ERROR` | 400 | Nessuna carta da un lato, quantità sotto 1, messaggio oltre 255 caratteri |
  | `INVALID_TRADE` | 400 | Scambio con se stessi, o la stessa carta sia offerta sia richiesta |
  | `USER_NOT_FOUND` | 404 | L'amico non esiste o ha cancellato l'account |
  | `NOT_FRIENDS` | 403 | Non siete amici |
  | `CARD_NOT_FOUND` | 404 | Una carta non esiste |
  | `CARDS_NOT_OWNED` | 409 | Offri più copie di quelle che hai |
  | `CARDS_LOCKED_IN_TRADE` | 409 | Le copie le hai, ma alcune sono già offerte in altri scambi in attesa |
  | `REQUESTED_CARDS_NOT_AVAILABLE` | 409 | L'amico non ha abbastanza copie disponibili di una carta che chiedi |

  Il `message` dice quale carta e quante copie, es. "Possiedi 2 copie di Il Faro nella Nebbia: ne offri 3.": si può mostrare così com'è. La stessa carta ripetuta nella richiesta si somma.
- **Avvisi prima di proporre o accettare:** `POST /trades/preview` (stesso corpo e stessi errori della proposta, ma non salva niente) per chi propone; `GET /trades/{tradeId}/deck-warnings` per chi guarda uno scambio in attesa (per chi lo riceve: le carte che darebbe accettando).

  ```json
  { "deckWarnings": [
      { "deckId": "019a2a10-1b2c-7d3e-8f40-a1b2c3d4e5f6", "deckName": "Incubi veloci", "isRanked": true, "cardName": "Il Faro nella Nebbia", "outcome": "INVALID" },
      { "deckId": "019a2a10-1b2c-7d3e-8f40-a1b2c3d4e5f7", "deckName": "Lord e amici", "isRanked": false, "cardName": "L'Uomo Senza Volto", "outcome": "NORMAL_VERSION" }
    ],
    "lastCopyWarnings": [
      { "cardId": "019a1f30-0000-7000-8000-000000000003", "cardName": "Il Faro nella Nebbia" }
  ] }
  ```

  `INVALID`: si mostra "Il mazzo Incubi veloci diventerà non valido (è il tuo mazzo classificato)". Un mazzo già non valido non si segnala. `NORMAL_VERSION`: cedi una full art che usi nel mazzo, ma hai una copia libera della versione normale, quindi si mostra "Il mazzo Lord e amici userà la versione normale di L'Uomo Senza Volto". Se la versione normale non ce l'hai, l'avviso è `INVALID`.

  `lastCopyWarnings`: carte che escono ed erano le ultime copie che avete; dopo lo scambio non ne resta nessuna (la full art si conta da sola, e il nome finisce con "(full art)"). Si mostra "Dopo lo scambio non avrai più Il Faro nella Nebbia".

  Con almeno un avviso si chiede conferma; lo scambio si può fare lo stesso. Liste vuote = niente da dire. Un mazzo non valido va poi sistemato nell'editor.
- **Accettazione:** solo chi riceve. Le carte si spostano subito, e i mazzi passano alla versione normale come annunciato da `NORMAL_VERSION`.
  - Se non avete più abbastanza copie disponibili delle carte richieste (es. le avete offerte in un altro scambio), arriva `409 REQUESTED_CARDS_NOT_AVAILABLE`, nessuna carta si muove e **lo scambio resta in attesa**: si può rifiutare, oppure annullare l'altro scambio e riprovare.
  - Se non siete più amici, lo scambio si annulla da solo: `409 TRADE_CANCELLED`.
- Uno scambio già chiuso: `409 TRADE_NOT_PENDING`. Uno scambio che non esiste o non vi riguarda: `404 TRADE_NOT_FOUND`. Rifiutare il proprio scambio o annullare quello di un altro: `403 FORBIDDEN`.

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
- `PUT /me/ranked-deck` risponde `204`; `POST /challenges` risponde `201` con la sfida; rifiuto e annullamento rispondono `204`.
- Tra due amici c'è al massimo una sfida in attesa: la seconda risponde `409`. Le sfide non scadono: chi sfida può annullarla.
- Dopo la partita si va su `/partite/{matchId}`, che legge `GET /matches/{matchId}`: ricaricare la pagina non fa giocare un'altra partita. La partita la vedono solo i due giocatori: per gli altri `404` con `code: "MATCH_NOT_FOUND"`.

**Sfide tra amici (pronte, #94).** `GET /challenges?box=received` sono le sfide da accettare o rifiutare, `?box=sent` quelle lanciate: solo quelle in attesa, dalla più recente. Ogni sfida, come la risposta di `POST /challenges`:

```json
{
  "id": "019a2e10-1b2c-7d3e-8f40-a1b2c3d4e5f6",
  "challenger": { "id": "019a1f2e-7c41-7d3a-9b2e-5f6a7b8c9d01", "username": "lucida" },
  "opponent": { "id": "019a1f2e-8d52-7e4b-8c3f-6a7b8c9d0e12", "username": "incubo_99" },
  "challengerDeckId": "019a2a10-1b2c-7d3e-8f40-a1b2c3d4e5f6",
  "challengerDeckName": "Incubi veloci",
  "challengerDeckValid": true,
  "challengerDeckProblems": [],
  "createdAt": "2026-11-18T10:15:00Z"
}
```

- `challengerDeckValid` si calcola a ogni lettura: il mazzo di chi sfida poteva essere valido quando ha lanciato la sfida e non esserlo più (carte scambiate, carta disattivata, mazzo cancellato). Se è `false`:
  - tra le inviate si mostra "Il tuo mazzo Incubi veloci non è più valido: sistemalo", con `challengerDeckProblems`;
  - tra le ricevute si mostra "Il mazzo di lucida non è più valido: aspetta che lo sistemi o rifiuta la sfida";
  - accettando arriva `409` con `code: "CHALLENGER_DECK_INVALID"` e `problems`: niente partita, la sfida resta in attesa.
- Se il mazzo è stato cancellato, `challengerDeckId` e `challengerDeckName` sono `null` e il problema è "Il mazzo della sfida è stato cancellato.".
- Il mazzo di chi accetta deve essere valido: altrimenti `400` con `code: "INVALID_DECK"` e `problems`, come per `POST /challenges`.
- Accetta e rifiuta solo chi è sfidato, annulla solo chi sfida: le altre combinazioni rispondono `403` con `code: "FORBIDDEN"`.
- Se all'accettazione non siete più amici, la sfida si annulla da sola (`409` con `code: "CHALLENGE_CANCELLED"`). Togliere un amico o cancellare l'account annulla subito le sfide in attesa tra i due.
- `pendingChallenges` di `GET /me/summary` conta le sfide ricevute in attesa e quelle lanciate con un mazzo da sistemare: è il numero da mostrare nel menu.

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
- `ratingChange` è `null` nelle partite rapide, e nelle classificate oltre la terza del giorno contro lo stesso avversario. `creditsEarned` sono i DC vinti da chi guarda. `leagueReached` dice se **chi guarda** è entrato per la prima volta in una lega con questa partita (`{ "name": "Sognatore", "rewardCredits": 100 }`, con `rewardCredits` i DC del premio già aggiunti al saldo): è il momento per un'animazione. È `null` se non è entrato in nessuna lega nuova; non arriva mai per Dormiente, la lega di partenza.
- `endReason`: `LIFE_POINTS` (punti vita a 0), `DECK_EXHAUSTED` (mazzo finito), `ROUND_LIMIT` (60 round, pareggio).
- `outcome`: `NONE_DIED`, `CHALLENGER_CARD_DIED`, `OPPONENT_CARD_DIED`, `BOTH_DIED`.
- **Replay:** ogni round ha `events`, gli eventi **in ordine**: carta giocata, effetti, i due colpi, morti, danno al giocatore, fine della partita. Il replay li mostra uno alla volta. `afterRound` è lo stato dei due lati a fine round (carta in campo, attacco, difesa, punti vita, carte nel mazzo e negli scarti): serve per saltare a un round. L'elenco dei tipi di evento e dei loro campi è in [schema.md](../database/schema.md#formato-dei-campi-json-di-matches).
- **Frasi della cronaca:** il log ha solo dati, niente testi. La frase la compone la pagina: nomi delle carte e degli effetti da `decks`, valori dall'evento. Esempio: dall'evento `EFFECT` con `code: "WEAKENING"`, `value: 2`, `before: 4`, `after: 2` → "Indebolimento 2: l'attacco del Faro nella Nebbia scende da 4 a 2".
- Un **round** è un singolo scambio di colpi; uno **scontro** (`clash`) è la serie di round tra le stesse due creature. Nella cronaca si usa "round", non "turno".
- `decks` contiene la copia dei due mazzi com'erano al momento della partita: serve per mostrare le carte anche se poi i mazzi sono cambiati.
- **Storico:** `/storico`, con un filtro per tipo di partita.

#### Classifica

`GET /leagues` (utente) → le leghe dalla più bassa, con il punteggio da cui si entra e il premio:

```json
[
  { "code": "DORMANT", "name": "Dormiente", "minRating": 0, "rewardCredits": 0 },
  { "code": "DREAMER", "name": "Sognatore", "minRating": 1100, "rewardCredits": 100 },
  { "code": "LUCID", "name": "Lucido", "minRating": 1300, "rewardCredits": 200 },
  { "code": "DREAMWEAVER", "name": "Dreamweaver", "minRating": 1500, "rewardCredits": 400 }
]
```

- La lega del giocatore e il suo punteggio sono in `GET /me/summary` (`rating` e `league`, con quanto manca alla lega successiva).
- Si parte da **1000 punti**, in Dormiente. Il punteggio cambia **solo con le classificate**: Elo con K = 32, quindi al massimo 32 punti a partita. Con punteggi uguali chi vince prende 16 punti e chi perde ne perde 16; battere chi è più avanti dà di più. Il pareggio fa guadagnare qualcosa a chi è più indietro.
- Contro lo stesso avversario contano per il rank **al massimo 3 classificate al giorno** (da mezzanotte, ora italiana); le altre si giocano ma hanno `ratingChange: null`.
- Il **premio** di una lega arriva solo la **prima volta** che ci si entra: chi scende e risale non lo riprende. I premi li decide Maie: finché non ci sono valgono 0 (gli importi dell'esempio sono inventati).
- Soglie e premi si cambiano dal backoffice: non scriveteli nelle pagine, leggeteli sempre da qui.
- Una classifica con gli altri giocatori (solo username e punteggio) si aggiunge se c'è tempo.

### Backoffice 🛠️ (solo `role: ADMIN`)

| Pagina | Chiamate | Note |
|--------|----------|------|
| Dashboard admin | `GET /admin/stats` | Numeri di utenti, carte, pacchetti, partite |
| Carte | `GET/POST /admin/cards`, `PUT /admin/cards/{cardId}` | Campi: codice, nome, descrizione, ATT, DIF, fazione, rarità, espansione, effetto e valore (solo per gli effetti con la X, `hasValue`), illustrazione 3:2 e ritaglio, attiva. La full art si crea come versione della carta (codice `-FA`, solo illustrazione 2:3 e ritaglio; oggi solo per le Leggendarie). ATT e DIF si possono cambiare anche se la carta è già posseduta e valgono anche per la full art. Disattivare una carta rende non validi i mazzi che la usano: conviene chiedere conferma dicendo quanti mazzi tocca |
| Caricamento illustrazioni | `POST /admin/images` (form `multipart/form-data` con il file, il codice della carta e il tipo: illustrazione, full art o copertina) → `{ "url": "/images/cards/base-013-v1.webp", "thumbUrl": "/images/cards/base-013-v1-thumb.webp" }` | Si carica il PNG originale: il backend lo conserva intatto e crea da solo le copie WebP. Una nuova immagine per la stessa carta diventa `-v2`. Le copertine hanno solo `url` |
| Pacchetti | `GET/POST /admin/packs`, `PUT /admin/packs/{packId}` | Tipo (negozio, set iniziale, benvenuto), prezzo, fazione (facoltativa), periodo di vendita (facoltativo), slot con rarità e probabilità (per ogni slot sommano a 100), copertine, probabilità full art, attivo |
| Configurazione | `GET/PUT /admin/factions`, `/admin/rarities`, `/admin/game-modes`, `/admin/missions`, `/admin/leagues` | Vantaggi e Lord delle fazioni, limiti delle rarità, punti vita e limite di round, premi delle missioni, soglie e premi delle leghe. Le carte per mazzo (20) sono in sola lettura |
| Messaggi | `GET /admin/contact-messages`, `PUT /admin/contact-messages/{messageId}` | Messaggi del form Contatti: letto, archiviato |

Le carte e i pacchetti non si cancellano: si disattivano (`isActive: false`). Un giocatore che chiama `/admin/...` riceve `403`.

**Pronti:** carte, caricamento delle immagini e pacchetti (#95), soglie e premi delle leghe (#106). **Da fare, dopo il SAL:** dashboard, il resto della configurazione e messaggi.

#### Carte: `GET/POST /admin/cards`, `GET/PUT /admin/cards/{cardId}`

`GET /admin/cards` è una lista a pagine come `GET /cards`, ma con **tutte** le carte: anche le full art e le disattivate. Parametri facoltativi: `faction`, `rarity`, `search` (nome o codice), `active` (`true`/`false`), `fullArt` (`true`/`false`), `page`, `size`.

Una carta del backoffice (dati salvati, non quelli presi dalla carta base):

```json
{
  "id": "019a1f30-0000-7000-8000-000000000013", "code": "BASE-013", "baseCardId": null, "variantType": null,
  "name": "Il Sogno Lucido", "description": "Sai di sognare, e il sogno ti obbedisce.", "attack": 10, "defense": 14,
  "factionCode": "DREAMS", "rarityCode": "LEGENDARY", "cardSetCode": "BASE", "effectCode": "REST", "effectValue": 3,
  "illustrationUrl": "/images/cards/base-013-v1.webp", "illustrationThumbUrl": "/images/cards/base-013-v1-thumb.webp",
  "illustrationFocusY": 0.40, "isActive": true, "deckCount": 12
}
```

- `deckCount`: in quanti mazzi è usata la carta, contando anche la sua full art. Prima di disattivarla conviene chiedere conferma: "12 mazzi diventeranno non validi".
- `POST` (risponde `201`) e `PUT` (risponde `200`) mandano gli stessi campi, senza `id` e `deckCount`. Con `PUT` si mandano tutti i dati della carta: quelli mancanti si svuotano (es. senza `effectCode` l'effetto si toglie). Fanno eccezione l'illustrazione e il ritaglio: se `illustrationUrl`, `illustrationThumbUrl` o `illustrationFocusY` mancano, restano come sono; per togliere l'illustrazione si mandano le due URL vuote (`""`).
- **Lord e full art:** la carta che fa da Lord di una fazione non cambia fazione e resta Leggendaria; una carta con la full art resta Leggendaria. Altrimenti `400 VALIDATION_ERROR` sul campo `factionCode` o `rarityCode`.
- **Carta normale:** obbligatori `code` (lettere maiuscole, numeri e trattini), `name`, `attack`, `defense` (da 1 a 99), `factionCode`, `rarityCode`, `cardSetCode`. `effectCode` è facoltativo; `effectValue` c'è solo per gli effetti con la X (da 1 a 20). `illustrationFocusY` va da 0 a 1 (di base 0,40).
- **Full art:** solo `code` (il codice della carta base + `-FA`, es. `BASE-013-FA`), `baseCardId`, l'illustrazione e `isActive`. Oggi solo per le Leggendarie, una per carta. Nome, testi, statistiche ed effetto restano vuoti: li prende dalla carta base.
- Il codice e la carta base non si cambiano.
- L'illustrazione: prima si carica con `POST /admin/images`, poi si mettono gli indirizzi ricevuti in `illustrationUrl` e `illustrationThumbUrl` (sempre tutti e due).

Errori: `409 CARD_CODE_TAKEN`, `409 CARD_NAME_TAKEN`, `409 FULL_ART_EXISTS`, `404 CARD_NOT_FOUND`, `400 VALIDATION_ERROR` (con `fieldErrors`: campi mancanti, fazione o effetto che non esistono, valore X sbagliato, full art di una carta non Leggendaria).

#### Immagini: `POST /admin/images`

Form `multipart/form-data` con tre campi: `file` (PNG o JPEG, al massimo 15 MB, 8000 pixel per lato e 40 milioni di pixel in tutto), `code` e `type`:

| `type` | `code` | Cosa crea | Risposta `201` |
|--------|--------|-----------|----------------|
| `ILLUSTRATION` | Codice della carta, es. `BASE-013` | Copia grande (1024 px) e miniatura (400 px) | `{ "url": "/images/cards/base-013-v1.webp", "thumbUrl": "/images/cards/base-013-v1-thumb.webp" }` |
| `FULL_ART` | `BASE-013` o `BASE-013-FA` | Come sopra, con `-fullart` nel nome | `{ "url": "/images/cards/base-013-fullart-v1.webp", "thumbUrl": "…" }` |
| `COVER` | Nome del pacchetto, es. `dream-pack` | Una sola copia (1024 px) | `{ "url": "/images/packs/dream-pack-v1.webp" }` |

Il backend conserva intatto l'originale e crea da solo le copie WebP. Un nuovo caricamento per lo stesso codice diventa `-v2`: il file vecchio non cambia mai, così la cache del browser resta giusta. Il caricamento **non** modifica la carta: gli indirizzi si salvano poi con `PUT /admin/cards/{cardId}` (o nel pacchetto).

```js
const form = new FormData();
form.append("file", fileInput.files[0]);
form.append("code", "BASE-013");
form.append("type", "ILLUSTRATION");
// Con FormData non si mette Content-Type: lo aggiunge il browser
```

Errori: `400 INVALID_IMAGE` (non è un PNG o un JPEG, o ha troppi pixel), `400 VALIDATION_ERROR` (codice non valido), `413 FILE_TOO_LARGE`, `400 MALFORMED_REQUEST` (manca il file o il tipo).

#### Pacchetti: `GET/POST /admin/packs`, `GET/PUT /admin/packs/{packId}`

`GET /admin/packs` restituisce **tutti** i pacchetti, anche spenti, set iniziali e benvenuto. Un pacchetto:

```json
{
  "id": "019a1f31-0000-7000-8000-000000000001", "name": "Dream Pack", "description": "…", "packType": "SHOP",
  "priceCredits": 500, "cardSetCode": null, "factionCode": null, "fullArtChance": 20.00,
  "availableFrom": null, "availableUntil": null, "imageUrl": null, "isActive": true,
  "slots": [
    { "cardCount": 3, "rarities": [ { "rarityCode": "COMMON", "weight": 100 } ] },
    { "cardCount": 1, "rarities": [ { "rarityCode": "RARE", "weight": 70 }, { "rarityCode": "EPIC", "weight": 25 }, { "rarityCode": "LEGENDARY", "weight": 5 } ] }
  ],
  "covers": [ { "id": "…", "name": "Dreams Lord", "factionCode": "DREAMS", "imageUrl": "/images/packs/dreams-lord-v1.webp", "isActive": true } ]
}
```

- `POST` (risponde `201`) e `PUT` (risponde `200`) mandano gli stessi campi, senza `id`. Nelle copertine, `id` serve solo per modificarne una che c'è già.
- `packType`: `SHOP` (si compra, prezzo maggiore di 0), `STARTER` o `WELCOME` (gratis, prezzo 0).
- **Slot:** almeno uno; i pesi (`weight`) di ogni slot sono percentuali e **sommano a 100**. Con `PUT` gli slot sostituiscono quelli di prima.
- **Copertine:** quelle con `id` si modificano, quelle senza `id` si aggiungono, quelle che mancano dalla lista si spengono (non si cancellano: le aperture già fatte le ricordano). Senza il campo `covers` le copertine restano come sono; con `"covers": []` si spengono tutte.
- `availableFrom` e `availableUntil` (UTC) sono facoltativi: la fine viene dopo l'inizio. `fullArtChance` va da 0 a 100.

Errori: `409 PACK_NAME_TAKEN`, `404 PACK_NOT_FOUND`, `400 VALIDATION_ERROR` (es. "Le probabilità dello slot 2 sommano a 95: devono fare 100.").

#### Leghe: `GET/PUT /admin/leagues`

`GET /admin/leagues` restituisce le leghe come `GET /leagues`. `PUT /admin/leagues` cambia soglie e premi di **tutte** le leghe insieme e risponde con le leghe aggiornate:

```json
{ "leagues": [
  { "code": "DORMANT", "minRating": 0, "rewardCredits": 0 },
  { "code": "DREAMER", "minRating": 1100, "rewardCredits": 100 },
  { "code": "LUCID", "minRating": 1300, "rewardCredits": 200 },
  { "code": "DREAMWEAVER", "minRating": 1500, "rewardCredits": 400 }
] }
```

- Ogni lega una volta sola, in qualunque ordine. Codici, nomi e ordine delle leghe non cambiano.
- La lega più bassa (Dormiente) parte sempre da **0** e non ha premio: è quella in cui si parte. Ogni lega parte da un punteggio più alto della precedente.
- Le leghe già raggiunte restano tali. Se si abbassa una soglia, chi è già sopra entra nella lega, con il premio, alla sua prossima classificata.

Errori: `400 VALIDATION_ERROR` con il campo sbagliato in `fieldErrors` (es. `leagues[2].minRating`: "Ogni lega deve partire da un punteggio più alto della lega prima.").

---

## 12. Come si monta una carta

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

---

## 13. Elenco delle pagine per la sitemap

Le pagine che il database permette, già con le decisioni di questi giorni.

| Area | Pagine |
|------|--------|
| 🔓 Pubblica | Home · Il gioco / Lore · Regolamento · Chi siamo · Contatti · FAQ · Glossario · Privacy Policy · Cookie · Termini di servizio · 404 |
| 🔓 Accesso | Registrazione · Accesso · Password dimenticata (facoltativa) · Nuova password (facoltativa) |
| 🔑 Dreamweaver | Benvenuto (con la scelta della fazione) · Dashboard (con missioni e lega) · Profilo (con cambio email e password e cancellazione dell'account) · Crediti · Negozio · Apertura pacchetto · Collezione · Dettaglio carta · Mazzi · Editor mazzo · **Amici** · Scambi · Nuovo scambio · Dettaglio scambio · **Gioca** (classificata e rapida) · Sfide tra amici · Nuova sfida · Accetta sfida · Risultato partita · Storico · **Classifica** |
| 🛠️ Backoffice | Dashboard admin · Carte · Pacchetti · Configurazione · Messaggi |

Rispetto alla prima sitemap: nuove **Benvenuto**, **Amici**, **Nuova password**, **Messaggi**, **Gioca** e **Classifica**; tolto l'accesso admin separato (si entra dalla stessa pagina); le sfide si mandano solo agli amici; nessuna scelta della modalità (le regole sono sempre quelle della Base, cambia solo il tipo di partita). Il SiteMap lo aggiorna Alessandro.

---

## 14. Elenco completo degli endpoint

Accesso: **pubblico** = senza accesso; **utente** = serve il token; **admin** = solo `role: ADMIN`. Gli indirizzi sono dopo `/api/v1`. Dove sono spiegati: accesso e profilo nel [capitolo 7](#7-accesso-con-jwt), tutto il resto nella pagina che li usa ([capitolo 11](#11-pagine-e-chiamate)).

### Pronti (in develop)

| Metodo e indirizzo | Accesso | A cosa serve |
|--------------------|---------|--------------|
| `POST /auth/register` | pubblico | Registrazione |
| `POST /auth/login` | pubblico | Accesso |
| `POST /auth/google` | pubblico | Accesso e registrazione con Google (#159; serve `GOOGLE_CLIENT_ID` sul backend) |
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
| `GET /me/summary` | utente | Dashboard: crediti, punteggio, lega, ultime carte, richieste di amicizia, scambi e sfide in attesa (le missioni arrivano con #125) |
| `GET /cards` | pubblico | Catalogo, con filtri e pagine (anche `?featured=true` per la Home) |
| `GET /cards/{cardId}` | pubblico | Una carta |
| `GET /factions` | pubblico | Le tre fazioni con il loro Lord |
| `GET /rules` | pubblico | I numeri del regolamento |
| `GET /images/...` | pubblico | Illustrazioni e immagini dei pacchetti, con la cache |
| `GET /me/cards` | utente | La mia collezione, con filtri e copie disponibili per gli scambi |
| `GET /decks` | utente | I miei mazzi |
| `GET /decks/{deckId}` | utente | Un mazzo |
| `POST /decks` | utente | Nuovo mazzo |
| `PUT /decks/{deckId}` | utente | Modifica di un mazzo |
| `DELETE /decks/{deckId}` | utente | Elimina un mazzo |
| `GET /friends` | utente | I miei amici |
| `DELETE /friends/{userId}` | utente | Togli un amico |
| `GET /users?search=` | utente | Cerca un Dreamweaver per username |
| `GET /users/{userId}/cards` | utente | Collezione di un amico (per gli scambi) |
| `GET /friend-requests?direction=` | utente | Richieste di amicizia ricevute o inviate |
| `POST /friend-requests` | utente | Manda una richiesta |
| `POST /friend-requests/{requestId}/accept` | utente | Accetta una richiesta di amicizia |
| `DELETE /friend-requests/{requestId}` | utente | Rifiuta o annulla una richiesta di amicizia |
| `GET /trades?box=` | utente | Scambi ricevuti, inviati, storico |
| `GET /trades/{tradeId}` | utente | Uno scambio |
| `POST /trades` | utente | Proponi uno scambio |
| `POST /trades/preview` | utente | Avvisi (mazzi e ultima copia) prima di proporre |
| `GET /trades/{tradeId}/deck-warnings` | utente | Avvisi (mazzi e ultima copia) prima di accettare |
| `POST /trades/{tradeId}/accept` | utente | Accetta uno scambio e sposta le carte |
| `POST /trades/{tradeId}/reject` | utente | Rifiuta uno scambio |
| `POST /trades/{tradeId}/cancel` | utente | Annulla (chi propone) |
| `GET /challenges?box=` | utente | Sfide in attesa ricevute o inviate, con lo stato del mazzo di chi sfida |
| `POST /challenges` | utente | Sfida un amico |
| `POST /challenges/{challengeId}/accept` | utente | Accetta e gioca: la partita si gioca subito |
| `POST /challenges/{challengeId}/reject` | utente | Rifiuta (chi è sfidato) |
| `POST /challenges/{challengeId}/cancel` | utente | Annulla (chi sfida) |
| `GET /matches/{matchId}` | utente | Risultato e replay di una partita, solo per i due giocatori |
| `GET /leagues` | utente | Le leghe con soglie e premi |
| `GET /admin/cards`, `GET /admin/cards/{cardId}` | admin | Le carte, anche full art e disattivate |
| `POST /admin/cards`, `PUT /admin/cards/{cardId}` | admin | Nuova carta o full art; modifica e disattivazione |
| `POST /admin/images` | admin | Caricamento di illustrazioni e copertine, con le copie WebP |
| `GET /admin/packs`, `GET /admin/packs/{packId}` | admin | Tutti i pacchetti, anche spenti |
| `POST /admin/packs`, `PUT /admin/packs/{packId}` | admin | Nuovo pacchetto; modifica di slot, copertine, prezzo e vendita |
| `GET /admin/leagues`, `PUT /admin/leagues` | admin | Soglie e premi delle leghe |

### Da fare

| Metodo e indirizzo | Accesso | A cosa serve | Issue | Chi | Sprint |
|--------------------|---------|--------------|-------|-----|--------|
| `PUT /me/ranked-deck` | utente | Scegli il mazzo classificato | #127 | Cristian | 6 |
| `POST /matches/ranked` | utente | Gioca una classificata | #127 | Cristian | 6 |
| `POST /matches/casual` | utente | Gioca una rapida | #127 | Cristian | 6 |
| `GET /me/matches` | utente | Storico delle partite | #127 | Cristian | 6 |
| `POST /contact-messages` | pubblico | Form Contatti | #162 | Nicolas | 5 |
| `GET /admin/stats`, configurazione, messaggi | admin | Il resto del backoffice (elenco in [Backoffice](#backoffice--solo-role-admin)) | #95 | Cristian | dopo il SAL |

Sprint 5: 16-22/11, prima del SAL del 23/11. Sprint 6: 23-29/11.

---

## 15. Tutti i codici di errore

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
| `CARD_NOT_FOUND` | 404 | Carta che non esiste |
| `INVALID_DECK` | 400 | Mazzo che non rispetta le regole: la lista è in `problems` |
| `DECK_NOT_FOUND` | 404 | Mazzo che non esiste o di un altro utente |
| `DECK_NAME_TAKEN` | 409 | Hai già un mazzo con questo nome |
| `PASSWORD_NOT_SET` | 400 | Account senza password (registrato con Google) |
| `INVALID_GOOGLE_TOKEN` | 401 | Accesso con Google non riuscito |
| `GOOGLE_EMAIL_NOT_VERIFIED` | 403 | Email dell'account Google non verificata |
| `GOOGLE_ACCOUNT_CONFLICT` | 409 | Account già collegato a un altro account Google |
| `GOOGLE_LOGIN_DISABLED` | 503 | Accesso con Google non attivo: si nasconde il pulsante |
| `USER_NOT_FOUND` | 404 | Giocatore che non esiste o ha cancellato l'account |
| `CANNOT_FRIEND_YOURSELF` | 400 | Richiesta di amicizia a se stessi |
| `ALREADY_FRIENDS` | 409 | Siete già amici |
| `FRIEND_REQUEST_ALREADY_SENT` | 409 | Richiesta già mandata e in attesa |
| `FRIEND_REQUEST_NOT_FOUND` | 404 | Richiesta che non esiste o non vi riguarda |
| `FRIEND_NOT_FOUND` | 404 | Togliere chi non è un amico |
| `NOT_FRIENDS` | 403 | Scambio o collezione di chi non è un amico |
| `TRADE_NOT_FOUND` | 404 | Scambio che non esiste o non vi riguarda |
| `TRADE_NOT_PENDING` | 409 | Scambio già accettato, rifiutato o annullato |
| `INVALID_TRADE` | 400 | Scambio con se stessi, o stessa carta offerta e richiesta |
| `CARDS_NOT_OWNED` | 409 | Offri più copie di quelle che hai |
| `CARDS_LOCKED_IN_TRADE` | 409 | Copie già offerte in altri scambi in attesa |
| `REQUESTED_CARDS_NOT_AVAILABLE` | 409 | Le copie richieste non sono disponibili: alla proposta, o all'accettazione (lo scambio resta in attesa) |
| `TRADE_CANCELLED` | 409 | All'accettazione non siete più amici: lo scambio si è annullato |
| `INVALID_CHALLENGE` | 400 | Sfida a se stessi |
| `CHALLENGE_ALREADY_PENDING` | 409 | C'è già una sfida in attesa tra voi due, in una delle due direzioni |
| `CHALLENGE_NOT_FOUND` | 404 | Sfida che non esiste o non vi riguarda |
| `CHALLENGE_NOT_PENDING` | 409 | Sfida già giocata, rifiutata o annullata |
| `CHALLENGE_CANCELLED` | 409 | All'accettazione non siete più amici: la sfida si è annullata |
| `CHALLENGER_DECK_INVALID` | 409 | All'accettazione il mazzo di chi ha sfidato non è più valido (`problems` dice perché): la sfida resta in attesa |
| `MATCH_NOT_FOUND` | 404 | Partita che non esiste o che non avete giocato |
| `ENGINE_UNAVAILABLE` | 503 | Il motore di gioco non risponde: la partita non si gioca, la sfida resta in attesa |
| `CARD_CODE_TAKEN`, `CARD_NAME_TAKEN` | 409 | Backoffice: codice o nome della carta già usati |
| `FULL_ART_EXISTS` | 409 | Backoffice: la carta ha già la sua full art |
| `PACK_NAME_TAKEN` | 409 | Backoffice: nome del pacchetto già usato |
| `INVALID_IMAGE` | 400 | Backoffice: il file non è un PNG o un JPEG, o ha troppi pixel |
| `FILE_TOO_LARGE` | 413 | Backoffice: file oltre i 15 MB |

**Solo nel frontend:** `NETWORK_ERROR` (stato 0), creato da `api.js` quando il backend non risponde.

**Da fare** (arriverà con il matchmaking, #127): `NO_OPPONENT_FOUND`.

---

## 16. Regole da ricordare

- **Il server decide**: estrazione dei pacchetti, esito delle partite, validità dei mazzi, scambi. Il frontend non calcola niente di tutto questo.
- **I controlli si fanno due volte**: nel frontend per aiutare l'utente, nel backend per sicurezza.
- **Mai token in `localStorage`**: solo in memoria, come in `api.js`.
- **Miniature nelle griglie, copia grande solo per la carta da sola**: le immagini restano leggere anche da telefono.
- **Mai dati personali negli URL** (es. email nella barra degli indirizzi).
- **Pagine dell'area utente e del backoffice in `noindex`**; `robots.txt` e `sitemap.xml` solo con le pagine pubbliche.
- **Responsive**: ATT, DIF, fazione e rarità leggibili anche su mobile.
- **Acquisto crediti con denaro reale**: solo "in arrivo", non si sviluppa in fase 1.

---

## 17. Cose ancora da decidere

| Decisione | Proposta | Chi |
|-----------|----------|-----|
| Strumenti del frontend e come si caricano i componenti React nelle pagine | Vite, con più pagine HTML, in JavaScript | Gaia (#72) |
| DC a chi viene scelto dal matchmaking e vince | No: i DC della vittoria vanno solo a chi ha avviato la partita | Maie + SWD |
| Partite che contano per le missioni | Missione giornaliera: ogni partita; obiettivo settimanale: solo vittorie classificate | Maie + Nicolas |
| Soglie e premi delle leghe | Dormiente 0, Sognatore 1100, Lucido 1300, Dreamweaver 1500 | Maie |
| Immagini di Nap Pack e REM Pack | Da proporre ([decisione 030](../decisioni/030-nuovi-pacchetti.md)) | Digital |
| Collezione con le carte mancanti in ombra (album) | Sì: invoglia a completare la collezione | WMD + DGS |

---

## 18. Controllo finale prima della PR

Prima di aprire una PR con una pagina che usa le API:

- [ ] Le chiamate passano tutte da `api.js`: nessun `fetch()` nella pagina.
- [ ] Il token non è mai in `localStorage` o `sessionStorage`.
- [ ] La pagina mostra il caricamento e gli errori (`message`, e `fieldErrors` accanto ai campi).
- [ ] Le `POST`, `PUT` e `DELETE` partono solo da un clic, con il pulsante spento mentre la richiesta è in corso.
- [ ] Prezzi, percentuali, limiti dei mazzi e nomi vengono dalle API, non sono scritti a mano.
- [ ] Le date sono mostrate nell'ora italiana.
- [ ] Nessun dato personale negli indirizzi.
- [ ] Provata con il backend acceso e un utente di prova.
