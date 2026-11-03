# Guida per Web Development · si parte il 2 novembre

Per **Gaia, Alessandro, Daniele e Yuri**. Raccoglie in un solo posto le regole e le decisioni da seguire quando si scrive il codice del frontend, così le pagine di tutti si incastrano e combaciano con il backend.

> Se qualcosa qui non torna con quello che vi serve, ditelo a Marco o Cristian **prima** di scrivere il codice, non dopo. Le cose ancora da decidere sono in [cose da decidere del 2/11](../riunioni/2026-11-02-cose-da-decidere.md). Quello che Software vi deve è nella [guida di Software Development](software-development.md#6-cosa-dobbiamo-agli-altri-team).

## In breve: le 10 regole da non sbagliare

1. **Una issue, un branch, una pull request.** Niente lavoro direttamente su `main` o `develop`.
2. **Il server decide tutto:** contenuto dei pacchetti, esito delle partite, validità dei mazzi, scambi. Il frontend mostra e manda, non calcola.
3. **Tutte le chiamate passano da `api.js`.** Nessuna pagina e nessun componente usa `fetch()` direttamente e l'indirizzo delle API (`/api/v1`) è scritto solo lì.
4. **Le API sono quelle della [guida ai dati](../api/guida-frontend.md):** `/api/v1/...`, in inglese, al plurale, senza verbi nell'URL. Non quelle del SiteMap.
5. **Gli id sono UUID**, cioè testi: mai `parseInt(id)`, mai confrontarli come numeri.
6. **Il token non va mai in `localStorage`** né in `sessionStorage`: solo in memoria, come in `api.js`.
7. **La carta è un solo componente React** (`Card`), montato con dati e illustrazione: mai immagini di carte già pronte.
8. **Prima lo smartphone:** ATT, DIF, fazione e rarità leggibili anche su un telefono.
9. **Nomi in inglese, commenti e testi in italiano.** Gli URL delle pagine sono in italiano (`/collezione`), quelli delle API in inglese (`/api/v1/me/cards`).
10. **Si usano i numeri e le parole di questa guida** (Dream Pack, 500 DC, Leggendaria...). Se un documento più vecchio dice altro, vale questa guida.

---

## 1. Prima di iniziare

- Account GitHub nel repository: Gaia `gaia-giachero`, Alessandro `alessandroalzati-boop`, Daniele `danieleocchii`, Yuri `yuribrescia-cyber`.
- Da leggere, in quest'ordine:

| Documento | Perché |
|-----------|--------|
| [workflow-github.md](../workflow-github.md) | Issue, branch, commit, pull request |
| [regole-codice.md](../regole-codice.md) | React, formattazione (2 spazi per HTML, CSS, JS), sicurezza, file da non caricare |
| [guida-frontend.md](../api/guida-frontend.md) | Le API pagina per pagina, `api.js`, come si monta la carta |
| [naming.md](../naming.md) e [commenti.md](../commenti.md) | Nomi e commenti |
| [checklist-pr.md](../checklist-pr.md) | Da rileggere prima di ogni pull request |

## 2. Come si lavora su GitHub

### Branch

- Ogni branch parte da **`develop`** aggiornato e si chiama `tipo/numero-issue-descrizione`.
- Se `develop` non c'è ancora, chiedete a Marco o Cristian prima di creare il branch.

```bash
git switch develop
git pull
git switch -c feature/72-progetto-frontend
```

✅ `feature/73-registrazione-accesso` · `feature/113-layout-comune` · `fix/120-menu-mobile`
❌ `gaia` · `frontend` · `prova` · `Feature/Layout`

### Commit

```
tipo(ambito): descrizione al presente, in italiano (#numero-issue)
```

Per il frontend l'ambito è `frontend`, oppure l'area (`user`, `pack`, `deck`, `trade`, `challenge`, `card`).

✅ `feat(frontend): aggiunge il layout comune con menu e piè di pagina (#113)`
✅ `fix(pack): non ricompra il pacchetto se si ricarica l'apertura (#82)`
❌ `modifiche` · ❌ `fix` · ❌ `aggiornato tutto`

Un commit = una modifica. Se nella descrizione serve una "e", probabilmente sono due commit.

### Pull request e board

- La PR va verso `develop`, con il [template](../template.md#template-pull-request) e `Closes #numero`.
- Serve l'approvazione di **un'altra persona**: chi apre la PR non se la approva da solo.
- Al massimo **2 issue In Progress a testa**.
- **Le card sulla board le spostano solo Marco e Cristian.** Quando finite una task, o aprite la PR, avvisateli.

## 3. Cosa è già deciso

Queste decisioni valgono anche dove il SiteMap o un documento più vecchio dicono altro.

| Tema | Decisione | ❌ Da non usare |
|------|-----------|-----------------|
| Database | PostgreSQL 18 con id UUID | MySQL, id numerici |
| Frontend | Pagine **HTML, CSS e JavaScript**; i **componenti** che si ripetono (la carta, #81) in **React** (2/11). Strumenti proposti: Vite, li conferma Gaia nella #72 | Tutto il sito in React (applicazione a pagina unica), Vue, Angular, jQuery |
| Avvio in locale | Sito e API sullo **stesso indirizzo**, con `docker compose up`: il container del frontend serve le pagine e manda `/api` al backend | Live Server su un'altra porta, indirizzo `http://localhost:8080` scritto in `api.js` |
| Accesso | Token JWT: quello di accesso (15 minuti) in memoria, il refresh token (7 giorni) in un cookie `HttpOnly` che gestisce il backend | Token "salvato nel client" in `localStorage` |
| Indirizzi delle API | `/api/v1/...` come in [guida-frontend.md](../api/guida-frontend.md) | `/api/decks/save`, `/api/trades/create`, `/api/challenges/create` |
| Partite | Regole della Base, con gli effetti, per tutte le partite. **Classificata**: avversario con rank simile scelto dal matchmaking, dà rank e DC. **Rapida**: avversario a caso dal matchmaking, oppure sfida a un amico; niente rank né DC. L'avversario del matchmaking non deve essere collegato: gioca con il suo **mazzo classificato** | Scelta della modalità, attesa dell'avversario collegato |
| Rank e leghe | In fase 1. Elo, si parte da 1000. Leghe: Dormiente, Sognatore, Lucido, Dreamweaver; premio in DC la prima volta che si entra in una lega | Rank "dopo" |
| Fazioni | Dreams, Nightmares, Memories, in inglese anche nei testi. Dreams batte Memories, Memories batte Nightmares, Nightmares batte Dreams | Altri cicli, Sogni / Incubi / Ricordi |
| Vantaggio di fazione | **+1 ATT** a chi è in vantaggio | "+1 Attacco/Difesa", malus |
| Rarità | Comune, Rara, Epica, Leggendaria. Le Leggendarie hanno una copia rara **full art** | "Mitica", "TIER" mostrato all'utente |
| Lord | Il Sogno Lucido (Dreams), L'Uomo Senza Volto (Nightmares), La Custode del Tempo Fermo (Memories): sono anche le copertine | |
| Pacchetto | Oggi c'è il **Dream Pack**: 5 carte, **500 DC**, con tre copertine (una per Lord) e lo stesso contenuto. Si apre subito, all'acquisto. Il cliente vuole **più tipi di pacchetti a prezzi diversi**: tipi, prezzi e probabilità li decidono Maie e Nicolas. Il negozio va pensato per più pacchetti | Pack Base 200, Pack Fazione 350, Pack Elite 500 (nomi e prezzi vecchi), inventario dei pacchetti chiusi |
| Contenuto del Dream Pack | 3 Comuni, 1 Rara, 1 slot speciale: Rara 70%, Epica 25%, Leggendaria 4%, Leggendaria full art 1% | |
| Moneta | **Dream Credits (DC)**, numeri interi | "CR", decimali, prezzi in euro |
| Ricompense | Registrazione 500 · accesso giornaliero 50 · missione giornaliera 100 · vittoria classificata 30 (max 3 al giorno, non contro un amico) · obiettivo settimanale 250 · premio di lega | Bonus di 1.000, vittoria +150, DC nelle partite rapide |
| Denaro reale | Non c'è in fase 1: il riquadro "Acquista crediti" si mostra solo come "in arrivo" | Pagamenti, prezzi in euro |
| Dopo la registrazione | 500 DC subito. Poi la pagina **Benvenuto**: il Dreamweaver **sceglie una fazione** e riceve il set iniziale di 20 carte di quella fazione (14 Comuni, 6 Rare) e 2 pacchetti di benvenuto, uno con un'Epica garantita | Ritorno al form di accesso, set con tutte e tre le fazioni |
| Scambi | **Solo tra amici**, quantità e rarità libere, solo se l'altro accetta. Niente crediti negli scambi | Scambi con chiunque |
| Sfide | Si mandano **solo agli amici**, che accettano: sono partite rapide. Al massimo una sfida in attesa tra due amici, senza scadenza | Sfide a chiunque |
| Mazzo | 20 carte, max 2 copie (1 per le Leggendarie), Leggendarie max 2, Epiche max 4, Rare max 8. Si salvano **solo i mazzi validi**. Ogni giocatore sceglie un mazzo classificato | Bozze, badge Valido / Non valido |
| Carte in uno scambio | Le copie offerte in uno scambio in attesa sono **bloccate per gli altri scambi**, ma restano usabili nei mazzi. Prima di proporre o accettare si avvisa se un mazzo diventerà non valido (anche il mazzo classificato). All'accettazione il server ricontrolla tutto | Bozze di mazzi, carte nascoste dalla collezione |
| Missioni | Una missione giornaliera e un obiettivo settimanale fissi, nello **Sprint 6** (una issue per SWD e una per WMD). La settimana inizia il lunedì a mezzanotte, ora italiana | Pagina con un elenco di missioni |
| Giorno delle ricompense | Inizia a **mezzanotte, ora italiana**. Le date arrivano in UTC e si mostrano nell'ora locale | |
| Partita | 20 punti vita (confermati dal cliente, da verificare con le simulazioni), max 60 round, poi pareggio. Si dice **round**, non turni | "Turni" |

Skin e animazioni per il retro e per le carte (altra richiesta del cliente) sono nel **Backlog**: non si costruiscono in fase 1. Ancora da decidere, quindi da non dare per scontato: tipi e prezzi dei nuovi pacchetti, soglie e premi delle leghe, quali partite contano per le missioni. Fino alla decisione, chiedete prima di costruire queste parti. Le decisioni, con il perché, sono in [docs/decisioni/](../decisioni/README.md).

## 4. Il design system

È il design system di Gaia, lo stesso della presentazione e dei mockup.

| Colore | Codice | Uso |
|--------|--------|-----|
| Midnight | `#111522` | Sfondi |
| Shadow | `#292733` | Pannelli |
| Antique Gold | `#C9955D` | Accenti · Memories |
| Ivory Mist | `#E8D9C4` | Testi |
| Dream Blue | `#8FC8F4` | Accenti · Dreams |
| Cosmic Blue | `#344C83` | Gradienti |
| Mystic Violet | `#76538D` | Magia · Nightmares |
| Dusky Mauve | `#704C5B` | Ombre |

| Carattere | Uso |
|-----------|-----|
| Cinzel | Titoli |
| Cormorant Garamond | Lore e sottotitoli |
| Inter | Interfaccia, pulsanti, numeri |

- I colori stanno **una volta sola**, in variabili CSS (es. `--color-midnight`), e le pagine usano le variabili. Mai codici esadecimali sparsi nei file.
- I mockup di riferimento (negozio, collezione, apertura del pacchetto, scambio) sono nella cartella "Dreamscape - Mockup completo", che Marco condivide con il gruppo.

## 5. Regole di codice per il frontend

### Formattazione

- UTF-8, fine riga LF, **2 spazi**, mai tab. Lo applica da solo il file `.editorconfig` di [template.md](../template.md#editorconfig), quando è nella radice del repository.
- Righe di massimo 120 caratteri.
- Nomi di variabili e funzioni in inglese, `camelCase`; costanti in `UPPER_SNAKE_CASE`.

```js
// ✅
const MAX_CARDS_PER_DECK = 20;
async function loadCollection(filters) { ... }

// ❌
const maxcarte = 20;
async function caricaCollezione(f) { ... }
```

### Chiamate al backend

- Un solo file `api.js`, come quello della [guida](../api/guida-frontend.md#4-il-file-apijs). Pagine e componenti importano `api()` e non toccano mai token né `fetch()`.
- Gli errori arrivano sempre con `{ status, code, message, fieldErrors }`: mostrate `fieldErrors` accanto ai campi e `message` negli altri casi.
- Con `401` ci pensa `api.js` (rinnovo del token o pagina di accesso). Con `403` si mostra "non hai i permessi".

```js
// ✅ La pagina usa api.js
import { api } from "./api.js";
const decks = await api("/decks");

// ❌ fetch nella pagina, indirizzo scritto a mano, token in localStorage
const res = await fetch("http://localhost:8080/api/decks", {
  headers: { Authorization: localStorage.getItem("token") }
});
```

### Cosa decide il server

| ✅ Il frontend | ❌ Il frontend non |
|----------------|-------------------|
| Controlla i campi del form per aiutare l'utente (email, lunghezza della password, 20 carte nel mazzo) | Decide se il mazzo è valido: lo ricontrolla sempre il server |
| Manda l'id del pacchetto e della copertina scelta | Estrae le carte del pacchetto |
| Mostra la cronaca della partita arrivata dal server | Simula la partita o calcola il vincitore |
| Mostra i crediti di `/me/summary` | Calcola il saldo da solo |

### Le carte

- Si montano con **un solo componente React**, `Card` (#81), usato in tutte le pagine: collezione, mazzi, aperture, scambi, risultato della partita.
- **Cornici con la finestra trasparente e coordinate le preparate voi in JavaScript** (decisione 21), partendo dalle 12 cornici di [carte/cornici/](../../carte/cornici/). Software non le prepara.
- **Miniatura** (`illustrationThumbUrl`) in griglie e liste, **copia grande** (`illustrationUrl`) solo per la carta da sola.
- Ogni carta ha un testo alternativo con nome, fazione, rarità, ATT e DIF.
- Il riferimento visivo sono le 15 carte di [carte/carte-finali/](../../carte/carte-finali/).

### Pagine, privacy e SEO

- Pagine dell'area utente e del backoffice in `noindex`. `robots.txt` e `sitemap.xml` solo con le pagine pubbliche.
- **Mai dati personali negli URL** (email, password, crediti).
- Degli altri giocatori si mostra solo lo username.
- Ricaricare la pagina dell'apertura **non** deve comprare un altro pacchetto: l'URL usa l'id dell'apertura (`/negozio/apertura/{openingId}`).
- Registrazione con due caselle separate: Privacy e Termini di servizio.

### Commenti

```html
<!-- Sezione: griglia della collezione (#83) -->
```

```jsx
/**
 * Griglia delle carte della collezione.
 * @param {Array} items - le carte arrivate da GET /me/cards
 */
export function CollectionGrid({ items }) {
  return (
    <section className="collection">
      {/* Una carta per ogni elemento, in miniatura (#83) */}
      {items.map((item) => <Card key={item.card.id} card={item.card} size="thumb" />)}
    </section>
  );
}
```

Niente codice commentato, niente `TODO` senza nome: `// TODO(daniele): aggiungere il filtro full art (#83)`.

### File da non caricare

`node_modules/`, `dist/`, `.vscode/`, `.idea/`, `.DS_Store`, file con password o token. Le immagini delle carte non si copiano nel frontend: arrivano dal backend.

## 6. Finché il backend non è pronto

Il backend nasce nello stesso Sprint 3 (#68 progetto, #71 registrazione e accesso). **Niente dati finti** (decisione 9 del 2/11): le pagine che usano le API si collegano al backend vero, quindi quelle dello Sprint 3 dipendono da #68 e #71. Nel frattempo:

- Si prepara quello che non ha bisogno di dati: progetto del frontend (#72), layout comune (#113), design system in CSS, form di registrazione e accesso (#73) senza invio, wireframe (#112).
- Se un dato che vi serve non c'è nella guida, non inventate il nome del campo: chiedete a Marco o Cristian, che lo aggiungono.
- Strumenti, struttura delle cartelle di `frontend/` e convenzione dei nomi CSS li decide Gaia nella #72 (decisione 8). Poi vanno scritti nelle sezioni "Frontend" di [naming.md](../naming.md) e [regole-codice.md](../regole-codice.md), dove oggi c'è solo la proposta.

## 7. Le vostre issue dei primi due sprint

| Issue | Chi | Sprint | Da leggere prima |
|-------|-----|--------|------------------|
| #72 Progetto frontend | Gaia | 3 | Questa guida, [`api.js`](../api/guida-frontend.md#4-il-file-apijs) |
| #113 Layout comune | Gaia, Yuri | 3 | Design system (sezione 4), menu diverso per visitatore, Dreamweaver e admin |
| #73 Registrazione e accesso | Alessandro | 3 | [Accesso con JWT](../api/guida-frontend.md#3-accesso-con-jwt): consensi, errori, pagina Benvenuto con la scelta della fazione |
| #112 Wireframe Amici, Benvenuto, Profilo, backoffice | Daniele | 3 | [Pagine e chiamate](../api/guida-frontend.md#6-pagine-e-chiamate) |
| #81 Componente della carta | Yuri, Gaia | 4 | [Come si monta una carta](../api/guida-frontend.md#7-come-si-monta-una-carta) |
| #82 Negozio e apertura | Daniele | 4 | Dream Pack (sezione 3), apertura con l'id nell'URL |
| #83 Collezione e dettaglio | Alessandro | 4 | Filtri, paginazione, passaggio alla full art |

## 8. A chi chiedere

| Domanda | Chi |
|---------|-----|
| API, dati, campi che mancano, accesso | Marco, Cristian |
| Grafica, design system, struttura del frontend | Gaia |
| Testi delle pagine, lore, nomi delle carte | Sara, Valerio (Digital) |
| Numeri del gioco (crediti, pacchetti, regole) | Si chiede a Marco o Cristian, che controllano le decisioni prese |
