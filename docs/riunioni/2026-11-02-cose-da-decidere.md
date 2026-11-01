# Cose da decidere per iniziare · 2 novembre 2026

Riunione del gruppo dopo il colloquio con il cliente. Da questa settimana (Sprint 3) si scrive il codice: queste sono le domande a cui dobbiamo rispondere perché Software, Web e Digital lavorino sulle stesse decisioni.

Ogni domanda ha le opzioni, la **proposta** di partenza (non è ancora una decisione) e chi decide. Le risposte si segnano nella colonna "Decisione" e poi Marco o Cristian le registrano in `docs/decisioni/`.

Guide collegate: [Software Development](../guide/software-development.md) · [Web Development](../guide/web-development.md) · [Digital Strategy](../guide/digital-strategy.md)

## Riepilogo

| N. | Domanda | Proposta | Chi | Entro | Decisione |
|----|---------|----------|-----|-------|-----------|
| **A** | **Prima di scrivere codice** | | | | |
| 1 | Creiamo il branch `develop`? | Sì, oggi, protetto | Cristian | 2/11 | |
| 2 | Aggiungiamo `.editorconfig` nella radice? | Sì, quello di `template.md` | Cristian | 2/11 | |
| 3 | Come entrano Sara e Maie nel repository? | Username a Cristian, invito oggi | Sara, Maie, Cristian | 3/11 | |
| 4 | Allineiamo il SiteMap di Alessandro alle decisioni? | Sì, prima delle pagine dello Sprint 4 | Alessandro, Gaia | 6/11 | |
| 5 | Mazzi non validi: si salvano come bozza? | No, solo mazzi validi | SWD, WMD | 6/11 | |
| 6 | Carte offerte in uno scambio: bloccate? | No, ricontrollate all'accettazione | SWD | 6/11 | |
| 7 | Come si avvia il frontend in locale? | Stesso indirizzo del backend, con Docker Compose | Gaia, Marco | 4/11 | |
| 8 | Struttura di `frontend/` e nomi delle classi CSS | Cartelle per tipo, nomi BEM | Gaia | 4/11 | |
| 9 | Con quali dati lavora Web finché il backend non c'è? | Dati finti uguali alla guida | Gaia, Marco | 4/11 | |
| 10 | Dove e come consegna Digital? | Pull request dal sito, cartelle della guida | Sara, Marco | 4/11 | |
| **B** | **Dal colloquio con il cliente** | | | | |
| 11 | Le quattro risposte del cliente | Da segnare nella #76 | Nicolas | 3/11 | |
| **C** | **Gioco ed economia** | | | | |
| 12 | Missione giornaliera e obiettivo settimanale | Versione semplice, nello Sprint 6 | Tutti | 6/11 | |
| 13 | Bonus di 500 DC, set iniziale e benvenuto si sommano? | Sì, tutti e tre | Maie, Nicolas | 6/11 | |
| 14 | Le carte più rare sono più forti? | Sì, ATT + DIF cresce con la rarità | Nicolas, Maie | 8/11 | |
| 15 | Fazioni del set iniziale e pacchetti di benvenuto | Tutte e tre; benvenuto come il Dream Pack | Maie, Nicolas | 8/11 | |
| 16 | Il pacchetto si apre subito? | Sì, all'acquisto | DGS, WMD | 8/11 | |
| 17 | Quando inizia il "giorno" delle ricompense? | Mezzanotte italiana | SWD, DGS | 8/11 | |
| 18 | Nomi italiani delle fazioni | No, restano in inglese | DGS | 8/11 | |
| 19 | Seconda buyer persona | Sì, solo per il marketing | Sara | 15/11 | |
| **D** | **Tecniche** | | | | |
| 20 | La carta si disegna su canvas o con livelli HTML? | Canvas | Gaia, Yuri | 8/11 | |
| 21 | Cornici e coordinate: le prepara Python o JavaScript? | Python, una volta sola | SWD, WMD | 8/11 | |
| 22 | Durata dei token | 15 minuti e 7 giorni | SWD | 4/11 | |

---

## A · Prima di scrivere codice

### 1. Il branch `develop`

**Perché:** [workflow-github.md](../workflow-github.md) dice che ogni branch parte da `develop` e che `main` si aggiorna solo da lì. Oggi `develop` non esiste: il primo branch di codice partirebbe da `main`.

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. Creare `develop` da `main` oggi e proteggerlo** (proposta) | Si segue il flusso scritto. `main` resta la versione consegnabile per il SAL e la consegna |
| B. Lavorare su `main` e togliere `develop` dal flusso | Un passaggio in meno, ma `main` non è più sempre consegnabile. Va cambiato `workflow-github.md` |

**Decide:** Cristian (è l'admin del repository).

### 2. Il file `.editorconfig`

**Perché:** le regole dicono 4 spazi per Java e 2 per HTML, CSS e JS, ma il file che le applica da solo non è nel repository: senza, ognuno formatta a modo suo e le PR si riempiono di differenze inutili.

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. Aggiungerlo oggi, copiato da [template.md](../template.md#editorconfig)** (proposta) | Le regole valgono da subito in IntelliJ, Eclipse e VS Code |
| B. Aggiungerlo con il progetto backend (#68) | Ma Web parte in parallelo e ne ha bisogno subito |

### 3. Account GitHub di Digital

**Perché:** nel repository c'è solo Valerio (`decosm`). Senza account non si possono assegnare le issue di Sara (#75, #98) e di Maie (#103, #109), né fare le loro PR.

**Proposta:** Sara e Maie mandano il loro username a Cristian oggi; Cristian manda l'invito. Intanto le loro consegne passano da Marco o Cristian. La #75 oggi non ha nessun assegnatario.

### 4. Il SiteMap di Alessandro (#49) e le decisioni

**Perché:** Web scriverà le pagine dal SiteMap. Dove il SiteMap dice altro rispetto alle decisioni, le pagine e il backend non combaceranno.

| Il SiteMap dice | Decisione (o proposta) | Stato |
|-----------------|------------------------|-------|
| Database MySQL | PostgreSQL 18 | Deciso il 26/10 |
| Bonus di benvenuto di 1.000 crediti | 500 DC alla registrazione | Confermato con il documento di Sara |
| Tre pacchetti: Base 200, Fazione 350, Elite 500 | Un solo Dream Pack da 500 DC, con tre copertine | Confermato |
| Vittoria +150 crediti | 30 DC, massimo 3 vittorie al giorno | Confermato |
| Scambio con qualsiasi utente | Solo tra amici | Deciso il 26/10 |
| Carte "Mitiche" | Comune, Rara, Epica, Leggendaria | Deciso |
| Vantaggio "+1 Attacco/Difesa" | +1 ATT | Deciso |
| API come `/api/decks/save`, `/api/trades/create`, `PUT /api/trades/{id}/action` | `/api/v1/...` di [guida-frontend.md](../api/guida-frontend.md), senza verbi nell'URL | Regola di [naming.md](../naming.md) |
| Token JWT "salvato nel client" | Token di accesso in memoria, refresh token in un cookie `HttpOnly` | Deciso il 26/10 |
| Dopo la registrazione si torna al form di accesso | Accesso automatico e pagina Benvenuto | Proposta della guida |
| Scambio "rifiutato o scaduto" | Nessuna scadenza in fase 1: chi propone può annullare | Proposta di `schema.md` |
| Mazzo "principale" | Non previsto: si sceglie il mazzo a ogni sfida | Da confermare |
| Mazzi salvati come bozza | Vedi domanda 5 | Da decidere |
| Carte bloccate durante lo scambio | Vedi domanda 6 | Da decidere |

**Proposta:** Alessandro aggiorna il SiteMap entro il 6/11, prima delle pagine dello Sprint 4. Gaia controlla.

### 5. Mazzi non validi

**Perché:** il SiteMap salva un mazzo non valido come bozza; lo schema del database prevede solo mazzi validi. Cambia la tabella dei mazzi (#80) e l'editor (#89).

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. Si salvano solo i mazzi validi** (proposta di `schema.md`) | Più semplice. Un mazzo che diventa non valido dopo uno scambio viene segnalato alla sfida |
| B. Si salvano anche le bozze | Comodo per chi costruisce il mazzo in più volte. Serve la colonna `decks.is_valid` e il badge Valido / Non valido |

### 6. Carte offerte in uno scambio in attesa

**Perché:** il SiteMap le blocca finché lo scambio non si chiude; lo schema propone di non bloccarle e di ricontrollare tutto quando lo scambio viene accettato.

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. Non bloccate, ricontrollate all'accettazione** (proposta di `schema.md`) | Meno stati da gestire. Se le carte mancano, lo scambio si annulla da solo con un messaggio |
| B. Bloccate finché lo scambio non si chiude (come nel SiteMap) | Chi riceve l'offerta è sicuro che le carte ci saranno. Va deciso cosa succede ai mazzi che le contengono |

### 7. Avvio del frontend in locale

**Perché:** il refresh token viaggia in un cookie `HttpOnly`, che funziona senza problemi solo se sito e API hanno lo stesso indirizzo. La guida oggi scrive `http://localhost:8080/api/v1` in `api.js`.

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. Stesso indirizzo: il container del frontend serve le pagine e manda `/api` al backend** (proposta, segue la #100) | `api.js` usa `/api/v1`, uguale in locale e alla consegna. Si avvia tutto con `docker compose up` |
| B. Due indirizzi (es. Live Server sulla porta 5500 e backend sulla 8080) | Avvio più veloce per Web, ma serve configurare CORS e il cookie, e alla consegna si cambia |

### 8. Struttura di `frontend/` e nomi delle classi CSS

**Perché:** le sezioni "Frontend" di [naming.md](../naming.md) e [regole-codice.md](../regole-codice.md) sono vuote e vanno riempite prima che quattro persone scrivano pagine.

**Proposta** (la decide Gaia nella #72):

```
frontend/
├── index.html, accesso.html, collezione.html...   # pagine, nomi in italiano come gli URL
├── css/        # base.css (variabili del design system), layout.css, una per area
├── js/         # api.js, card.js (componente della carta), un file per pagina
├── assets/     # cornici, simboli, font, icone
└── mock/       # dati finti (domanda 9)
```

Nomi delle classi alla **BEM**, come già negli esempi di [commenti.md](../commenti.md): `.card`, `.card__name`, `.card--rare`.

### 9. Dati finti per Web

**Perché:** backend (#68) e accesso (#71) nascono nello stesso sprint delle prime pagine. Web non deve fermarsi ad aspettarli.

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. File JSON in `frontend/mock/`, uguali agli esempi della guida, e un interruttore in `api.js`** (proposta) | Si lavora da subito. Quando il backend è pronto si spegne l'interruttore |
| B. Aspettare il backend | Nessun lavoro doppio, ma Web resta fermo una settimana |

### 10. Consegne di Digital

**Perché:** catalogo, illustrazioni e testi diventano dati del database e pagine del sito: devono arrivare sempre nello stesso posto e nello stesso formato.

**Proposta:** pull request fatta dal sito di GitHub (spiegata nella [guida di Digital](../guide/digital-strategy.md#1-account-github-e-consegne)), nelle cartelle `carte/compositore/carte.csv`, `carte/illustrazioni/`, `carte/copertine/`, `docs/testi-sito/`, `docs/identita/`. Chi non ha ancora l'account passa da Marco o Cristian.

---

## B · Dal colloquio con il cliente

### 11. Le quattro risposte del cliente

Le domande della slide 24. Il note-taker le segna durante il colloquio, poi Nicolas le registra nella #76 (COM-06).

| Domanda | La nostra proposta | Risposta del cliente |
|---------|--------------------|----------------------|
| Quale proposta di retro delle carte preferite? | Una delle tre di Digital | |
| Partite più tattiche (20 punti vita) o più rapide (10)? | 20, da confermare con le simulazioni | |
| Quante carte deve avere il catalogo alla consegna? | Circa 30 | |
| Le ricompense in Dream Credits vi sembrano giuste? | Quelle della slide 12 | |

Quello che il cliente chiede in più si aggiunge qui sotto, con chi se ne occupa.

---

## C · Gioco ed economia

### 12. Missione giornaliera e obiettivo settimanale

**Perché:** al cliente abbiamo mostrato 100 DC per la missione giornaliera e 250 per l'obiettivo settimanale, ma nessuna issue li prevede.

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. Versione semplice, nello Sprint 6** (proposta) | Una missione giornaliera fissa (es. "gioca una sfida") e un obiettivo settimanale fisso (es. "vinci 5 sfide"), senza un elenco di missioni configurabili. Una issue per SWD e una per WMD |
| B. Missioni configurabili dal backoffice | Più ricco, ma è lavoro in più per backend, pagine e backoffice |
| C. Rimandarle al Backlog | Per ora solo registrazione, accesso giornaliero e vittorie. Va detto al cliente |

### 13. Bonus di registrazione, set iniziale e benvenuto

**Perché:** alla registrazione oggi sono previsti 500 DC, un set iniziale di 20 carte e 2 pacchetti di benvenuto. Va deciso se il nuovo Dreamweaver li riceve tutti.

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. Tutti e tre** (proposta) | Con i 500 DC il nuovo Dreamweaver compra subito il primo Dream Pack |
| B. Set iniziale e benvenuto, senza i 500 DC | Va cambiata la tabella delle ricompense mostrata al cliente |
| C. Set iniziale e 500 DC, senza benvenuto | Meno carte all'inizio, ma il primo mazzo si fa comunque |

### 14. Rarità e statistiche

**Perché:** serve una regola per il catalogo di circa 30 carte (#74), prima delle illustrazioni (#84).

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. ATT + DIF cresce con la rarità, come nel set di prova** (proposta): Comuni 6–10, Rare 9–10, Epiche 10–11, Leggendarie 12–13 | La rarità conta anche in partita. Si verifica con le simulazioni (#87) |
| B. Statistiche simili per tutte le rarità; la differenza la fanno gli effetti | Partite più equilibrate tra giocatori nuovi e vecchi |

### 15. Set iniziale e pacchetti di benvenuto

| Domanda | Proposta |
|---------|----------|
| Il set iniziale ha carte di tutte e tre le fazioni, o di una fazione scelta dal giocatore? | Tutte e tre |
| Cosa c'è nei 2 pacchetti di benvenuto? | Uno come il Dream Pack, l'altro con l'Epica garantita nello slot speciale |

### 16. Apertura dei pacchetti

**Perché:** cambia il negozio (#82). Se il pacchetto si può tenere chiuso, serve un inventario dei pacchetti.

**Proposta:** il Dream Pack si apre subito, al momento dell'acquisto.

### 17. Inizio del "giorno"

**Perché:** accesso giornaliero, missione giornaliera e le 3 vittorie al giorno devono ripartire tutti alla stessa ora.

**Proposta:** a mezzanotte, ora italiana. Nel database le date restano in UTC.

### 18. Nomi italiani delle fazioni

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. Restano in inglese: Dreams, Nightmares, Memories** (proposta) | Sono già nella presentazione, nei mockup e nelle carte |
| B. In italiano nei testi (Sogni, Incubi, Ricordi) | Vanno cambiati testi, carte e mockup. I codici `DREAMS`, `NIGHTMARES`, `MEMORIES` restano comunque |

### 19. Seconda buyer persona

**Proposta:** sì, solo per il piano di marketing (#98). Non cambia niente nel codice. Decide Sara.

---

## D · Tecniche

### 20. Disegno della carta (#81)

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. Canvas** (proposta) | Stesso risultato delle carte di prova, con le stesse coordinate |
| B. Livelli HTML e CSS | Testi selezionabili e più semplici da rendere accessibili |

### 21. Cornici trasparenti e coordinate

**Perché:** il componente della carta ha bisogno di 12 cornici con la finestra trasparente e delle coordinate di nome, testi, ATT e DIF.

| Opzione | Cosa comporta |
|---------|---------------|
| ✅ **A. Le prepara il compositore Python una volta sola** (proposta) | Le misure ci sono già nel compositore. Il frontend riceve i file pronti e un `zone.json` |
| B. Il frontend le calcola in JavaScript | Nessuno script in più, ma si rifanno le misure da capo |

### 22. Durata dei token

**Proposta:** token di accesso 15 minuti, refresh token 7 giorni. Decide SWD, serve a #71 e #73.

---

## Dopo la riunione

| Cosa | Chi |
|------|-----|
| Registrare le decisioni in `docs/decisioni/`, una per file | Marco, Cristian |
| Aggiornare i punti aperti di [schema.md](../database/schema.md) e i numeri negli esempi di [guida-frontend.md](../api/guida-frontend.md) | Marco |
| Aggiornare le issue toccate dalle decisioni: #78, #80, #82, #89, #93, #98, #114, #115 | Marco, Cristian |
| Aggiornare il SiteMap | Alessandro |
| Riempire le sezioni "Frontend" di `naming.md` e `regole-codice.md` | Gaia |
