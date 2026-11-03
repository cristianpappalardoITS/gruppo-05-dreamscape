# Cose da decidere per iniziare · 2 novembre 2026

> **Aggiornato il 3/11 con le decisioni della riunione.** 21 decisioni prese su 22: resta aperta la 3. Quelle che cambiano le guide sono elencate in [Cosa cambia](#cosa-cambia-dopo-le-decisioni).

Riunione del gruppo dopo il colloquio con il cliente. Da questa settimana (Sprint 3) si scrive il codice: queste sono le domande a cui dobbiamo rispondere perché Software, Web e Digital lavorino sulle stesse decisioni.

Ogni domanda ha le opzioni, la **proposta** di partenza (non è ancora una decisione) e chi decide. Le risposte si segnano nella colonna "Decisione" e poi Marco o Cristian le registrano in `docs/decisioni/`.

Guide collegate: [Software Development](../guide/software-development.md) · [Web Development](../guide/web-development.md) · [Digital Strategy](../guide/digital-strategy.md)

## Cosa cambia dopo le decisioni

Alcune decisioni vanno contro quello che oggi è scritto nelle guide e nelle regole. Finché le guide non sono aggiornate, vale quello che è scritto qui.

| Decisione | Cosa va cambiato | Chi |
|-----------|------------------|-----|
| 20 · Componenti React | Cambia la decisione del 26/10 "HTML, CSS e JavaScript, senza framework". Da aggiornare: README, [regole-codice.md](../regole-codice.md), [guida-frontend.md](../api/guida-frontend.md), [guida di Web](../guide/web-development.md), issue #72 e #81 | Gaia, Marco |
| 21 · Cornici e coordinate in JavaScript | La [guida di Software](../guide/software-development.md) promette a Web le cornici dal compositore Python: si toglie | Marco |
| 9 · Si aspetta il backend | La [guida di Web](../guide/web-development.md) dice di lavorare con dati finti: si toglie. Le pagine dello Sprint 3 dipendono da #68 e #71 | Gaia, Cristian, Marco |
| 10 · Consegne sul Drive | La [guida di Digital](../guide/digital-strategy.md) chiede pull request nel repository: va scritto chi porta i file dal Drive al repository (carte.csv, illustrazioni) | Sara, Marco |
| 11 · 99 o 102 carte | Il catalogo passa da circa 30 a circa 100 carte: più lavoro per #74 (catalogo) e #84 (illustrazioni) | Maie, Nicolas, Valerio |
| 15 · Set iniziale di una fazione | Ogni fazione deve avere almeno 7 Comuni e 3 Rare diverse, per comporre 14 Comuni e 6 Rare con al massimo 2 copie. Serve la scelta della fazione alla registrazione o nella pagina Benvenuto | Nicolas, Marco, Alessandro |
| Richiesta: più tipi di pacchetti | Le guide e la slide 12 parlano di un solo Dream Pack | Maie, Nicolas |

## Riepilogo

| N. | Domanda | Proposta | Chi | Entro | Decisione |
|----|---------|----------|-----|-------|-----------|
| **A** | **Prima di scrivere codice** | | | | |
| 1 | Creiamo il branch `develop`? | Sì, oggi, protetto | Cristian | 2/11 | ✅ Sì, develop da main, protetto |
| 2 | Aggiungiamo `.editorconfig` nella radice? | Sì, quello di `template.md` | Cristian | 2/11 | ✅ Con il progetto backend (#68) |
| 3 | Come entrano Sara e Maie nel repository? | Username a Cristian, invito oggi | Sara, Maie, Cristian | 3/11 | ⏳ Non ancora deciso |
| 4 | Allineiamo il SiteMap di Alessandro alle decisioni? | Sì, prima delle pagine dello Sprint 4 | Alessandro, Gaia | 6/11 | ✅ Alessandro entro il 6/11, Gaia controlla |
| 5 | Mazzi non validi: si salvano come bozza? | No, solo mazzi validi | SWD, WMD | 6/11 | ✅ Solo mazzi validi |
| 6 | Carte offerte in uno scambio: bloccate? | No, ricontrollate all'accettazione | SWD | 6/11 | ✅ Non bloccate, ricontrollate all'accettazione |
| 7 | Come si avvia il frontend in locale? | Stesso indirizzo del backend, con Docker Compose | Gaia, Marco | 4/11 | ✅ Stesso indirizzo, Docker Compose |
| 8 | Struttura di `frontend/` e nomi delle classi CSS | Cartelle per tipo, nomi BEM | Gaia | 4/11 | ✅ Decide Gaia nella #72 |
| 9 | Con quali dati lavora Web finché il backend non c'è? | Dati finti uguali alla guida | Gaia, Marco | 4/11 | ✅ Si aspetta il backend |
| 10 | Dove e come consegna Digital? | Pull request dal sito, cartelle della guida | Sara, Marco | 4/11 | ✅ Sul Drive |
| **B** | **Dal colloquio con il cliente** | | | | |
| 11 | Le quattro risposte del cliente | Da segnare nella #76 | Nicolas | 3/11 | ✅ Monolite sul lago · 20 punti vita · 99 o 102 carte · ricompense ok |
| **C** | **Gioco ed economia** | | | | |
| 12 | Missione giornaliera e obiettivo settimanale | Versione semplice, nello Sprint 6 | Tutti | 6/11 | ✅ Versione semplice, Sprint 6 |
| 13 | Bonus di 500 DC, set iniziale e benvenuto si sommano? | Sì, tutti e tre | Maie, Nicolas | 6/11 | ✅ Tutti e tre |
| 14 | Le carte più rare sono più forti? | Sì, ATT + DIF cresce con la rarità | Nicolas, Maie | 8/11 | ✅ Sì, e contano anche gli effetti |
| 15 | Fazioni del set iniziale e pacchetti di benvenuto | Tutte e tre; benvenuto come il Dream Pack | Maie, Nicolas | 8/11 | ✅ Una fazione scelta dal giocatore |
| 16 | Il pacchetto si apre subito? | Sì, all'acquisto | DGS, WMD | 8/11 | ✅ Sì, all'acquisto |
| 17 | Quando inizia il "giorno" delle ricompense? | Mezzanotte italiana | SWD, DGS | 8/11 | ✅ Mezzanotte italiana |
| 18 | Nomi italiani delle fazioni | No, restano in inglese | DGS | 8/11 | ✅ Restano in inglese |
| 19 | Seconda buyer persona | Sì, solo per il marketing | Sara | 15/11 | ✅ No, basta Giulia |
| **D** | **Tecniche** | | | | |
| 20 | La carta si disegna su canvas o con livelli HTML? | Canvas | Gaia, Yuri | 8/11 | ✅ Componenti React |
| 21 | Cornici e coordinate: le prepara Python o JavaScript? | Python, una volta sola | SWD, WMD | 8/11 | ✅ Il frontend in JavaScript |
| 22 | Durata dei token | 15 minuti e 7 giorni | SWD | 4/11 | ✅ 15 minuti e 7 giorni |

---

## A · Prima di scrivere codice

### 1. Il branch `develop`

> **Decisione del 2/11:** si crea `develop` da `main` e si protegge (opzione A).

**Perché:** [workflow-github.md](../workflow-github.md) dice che ogni branch parte da `develop` e che `main` si aggiorna solo da lì. Oggi `develop` non esiste: il primo branch di codice partirebbe da `main`.

| Opzione | Cosa comporta |
|---------|---------------|
| **A. Creare `develop` da `main` oggi e proteggerlo** (proposta) | Si segue il flusso scritto. `main` resta la versione consegnabile per il SAL e la consegna |
| B. Lavorare su `main` e togliere `develop` dal flusso | Un passaggio in meno, ma `main` non è più sempre consegnabile. Va cambiato `workflow-github.md` |

**Decide:** Cristian (è l'admin del repository).

### 2. Il file `.editorconfig`

> **Decisione del 2/11:** il file `.editorconfig` arriva con il progetto backend, nella #68 (opzione B).

**Perché:** le regole dicono 4 spazi per Java e 2 per HTML, CSS e JS, ma il file che le applica da solo non è nel repository: senza, ognuno formatta a modo suo e le PR si riempiono di differenze inutili.

| Opzione | Cosa comporta |
|---------|---------------|
| **A. Aggiungerlo oggi, copiato da [template.md](../template.md#editorconfig)** (proposta) | Le regole valgono da subito in IntelliJ, Eclipse e VS Code |
| B. Aggiungerlo con il progetto backend (#68) | Ma Web parte in parallelo e ne ha bisogno subito |

### 3. Account GitHub di Digital

> **Decisione del 2/11:** ⏳ non ancora deciso.

**Perché:** nel repository c'è solo Valerio (`decosm`). Senza account non si possono assegnare le issue di Sara (#75, #98) e di Maie (#103, #109), né fare le loro PR.

**Proposta:** Sara e Maie mandano il loro username a Cristian oggi; Cristian manda l'invito. Intanto le loro consegne passano da Marco o Cristian. La #75 oggi non ha nessun assegnatario.

### 4. Il SiteMap di Alessandro (#49) e le decisioni

> **Decisione del 2/11:** Alessandro aggiorna il SiteMap entro il 6/11 e Gaia controlla.

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

> **Decisione del 2/11:** si salvano solo i mazzi validi (opzione A).

**Perché:** il SiteMap salva un mazzo non valido come bozza; lo schema del database prevede solo mazzi validi. Cambia la tabella dei mazzi (#80) e l'editor (#89).

| Opzione | Cosa comporta |
|---------|---------------|
| **A. Si salvano solo i mazzi validi** (proposta di `schema.md`) | Più semplice. Un mazzo che diventa non valido dopo uno scambio viene segnalato alla sfida |
| B. Si salvano anche le bozze | Comodo per chi costruisce il mazzo in più volte. Serve la colonna `decks.is_valid` e il badge Valido / Non valido |

### 6. Carte offerte in uno scambio in attesa

> **Decisione del 2/11:** le carte non si bloccano: si ricontrolla tutto quando lo scambio viene accettato (opzione A).

**Perché:** il SiteMap le blocca finché lo scambio non si chiude; lo schema propone di non bloccarle e di ricontrollare tutto quando lo scambio viene accettato.

| Opzione | Cosa comporta |
|---------|---------------|
| **A. Non bloccate, ricontrollate all'accettazione** (proposta di `schema.md`) | Meno stati da gestire. Se le carte mancano, lo scambio si annulla da solo con un messaggio |
| B. Bloccate finché lo scambio non si chiude (come nel SiteMap) | Chi riceve l'offerta è sicuro che le carte ci saranno. Va deciso cosa succede ai mazzi che le contengono |

### 7. Avvio del frontend in locale

> **Decisione del 2/11:** sito e API sullo stesso indirizzo con Docker Compose; `api.js` usa `/api/v1` (opzione A).

**Perché:** il refresh token viaggia in un cookie `HttpOnly`, che funziona senza problemi solo se sito e API hanno lo stesso indirizzo. La guida oggi scrive `http://localhost:8080/api/v1` in `api.js`.

| Opzione | Cosa comporta |
|---------|---------------|
| **A. Stesso indirizzo: il container del frontend serve le pagine e manda `/api` al backend** (proposta, segue la #100) | `api.js` usa `/api/v1`, uguale in locale e alla consegna. Si avvia tutto con `docker compose up` |
| B. Due indirizzi (es. Live Server sulla porta 5500 e backend sulla 8080) | Avvio più veloce per Web, ma serve configurare CORS e il cookie, e alla consegna si cambia |

### 8. Struttura di `frontend/` e nomi delle classi CSS

> **Decisione del 2/11:** decide Gaia nella #72.

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

> **Decisione del 2/11:** Web aspetta il backend, senza dati finti (opzione B).

**Perché:** backend (#68) e accesso (#71) nascono nello stesso sprint delle prime pagine. Web non deve fermarsi ad aspettarli.

| Opzione | Cosa comporta |
|---------|---------------|
| **A. File JSON in `frontend/mock/`, uguali agli esempi della guida, e un interruttore in `api.js`** (proposta) | Si lavora da subito. Quando il backend è pronto si spegne l'interruttore |
| B. Aspettare il backend | Nessun lavoro doppio, ma Web resta fermo una settimana |

### 10. Consegne di Digital

> **Decisione del 2/11:** Digital consegna i file sul Drive.

**Perché:** catalogo, illustrazioni e testi diventano dati del database e pagine del sito: devono arrivare sempre nello stesso posto e nello stesso formato.

**Proposta:** pull request fatta dal sito di GitHub (spiegata nella [guida di Digital](../guide/digital-strategy.md#1-account-github-e-consegne)), nelle cartelle `carte/compositore/carte.csv`, `carte/illustrazioni/`, `carte/copertine/`, `docs/testi-sito/`, `docs/identita/`. Chi non ha ancora l'account passa da Marco o Cristian.

---

## B · Dal colloquio con il cliente

### 11. Le quattro risposte del cliente

> **Decisione del 2/11:** risposte del cliente nella tabella qui sotto.

Le domande della slide 24. Il note-taker le segna durante il colloquio, poi Nicolas le registra nella #76 (COM-06).

| Domanda | La nostra proposta | Risposta del cliente |
|---------|--------------------|----------------------|
| Quale proposta di retro delle carte preferite? | Una delle tre di Digital | Proposta 3, il monolite sul lago |
| Partite più tattiche (20 punti vita) o più rapide (10)? | 20, da confermare con le simulazioni | 20 punti vita, da confermare con le simulazioni |
| Quante carte deve avere il catalogo alla consegna? | Circa 30 | 99 o 102 |
| Le ricompense in Dream Credits vi sembrano giuste? | Quelle della slide 12 | Sì, quelle della slide 12 |

Richieste in più del cliente:

| Richiesta | Cosa comporta | Chi |
|-----------|---------------|-----|
| Diverse tipologie di pacchetti, a prezzi diversi | Cambia il "solo Dream Pack da 500 DC" confermato il 31/10: vanno decisi tipi, prezzi e probabilità. Il database li prevede già (`packs`, `pack_slots`) | Maie, Nicolas, poi SWD (#78) e WMD (#82) |
| Skin e animazioni per il retro delle carte e per le carte | Lavoro nuovo per Digital (grafica) e Web (componente della carta, #81). Va deciso se è in fase 1 o nel Backlog | Sara, Gaia |

---

## C · Gioco ed economia

### 12. Missione giornaliera e obiettivo settimanale

> **Decisione del 2/11:** versione semplice nello Sprint 6, con una issue per SWD e una per WMD (opzione A).

**Perché:** al cliente abbiamo mostrato 100 DC per la missione giornaliera e 250 per l'obiettivo settimanale, ma nessuna issue li prevede.

| Opzione | Cosa comporta |
|---------|---------------|
| **A. Versione semplice, nello Sprint 6** (proposta) | Una missione giornaliera fissa (es. "gioca una sfida") e un obiettivo settimanale fisso (es. "vinci 5 sfide"), senza un elenco di missioni configurabili. Una issue per SWD e una per WMD |
| B. Missioni configurabili dal backoffice | Più ricco, ma è lavoro in più per backend, pagine e backoffice |
| C. Rimandarle al Backlog | Per ora solo registrazione, accesso giornaliero e vittorie. Va detto al cliente |

### 13. Bonus di registrazione, set iniziale e benvenuto

> **Decisione del 2/11:** il nuovo Dreamweaver riceve tutti e tre: 500 DC, set iniziale e 2 pacchetti di benvenuto (opzione A).

**Perché:** alla registrazione oggi sono previsti 500 DC, un set iniziale di 20 carte e 2 pacchetti di benvenuto. Va deciso se il nuovo Dreamweaver li riceve tutti.

| Opzione | Cosa comporta |
|---------|---------------|
| **A. Tutti e tre** (proposta) | Con i 500 DC il nuovo Dreamweaver compra subito il primo Dream Pack |
| B. Set iniziale e benvenuto, senza i 500 DC | Va cambiata la tabella delle ricompense mostrata al cliente |
| C. Set iniziale e 500 DC, senza benvenuto | Meno carte all'inizio, ma il primo mazzo si fa comunque |

### 14. Rarità e statistiche

> **Decisione del 2/11:** ATT + DIF cresce con la rarità, come nel set di prova, e contano anche gli effetti.

**Perché:** serve una regola per il catalogo di circa 30 carte (#74), prima delle illustrazioni (#84).

| Opzione | Cosa comporta |
|---------|---------------|
| **A. ATT + DIF cresce con la rarità, come nel set di prova** (proposta): Comuni 6–10, Rare 9–10, Epiche 10–11, Leggendarie 12–13 | La rarità conta anche in partita. Si verifica con le simulazioni (#87) |
| B. Statistiche simili per tutte le rarità; la differenza la fanno gli effetti | Partite più equilibrate tra giocatori nuovi e vecchi |

### 15. Set iniziale e pacchetti di benvenuto

> **Decisione del 2/11:** il set iniziale è di una fazione scelta dal giocatore (opzione B).

| Domanda | Proposta |
|---------|----------|
| Il set iniziale ha carte di tutte e tre le fazioni, o di una fazione scelta dal giocatore? | Tutte e tre |
| Cosa c'è nei 2 pacchetti di benvenuto? | Uno come il Dream Pack, l'altro con l'Epica garantita nello slot speciale |

### 16. Apertura dei pacchetti

> **Decisione del 2/11:** il Dream Pack si apre subito, al momento dell'acquisto.

**Perché:** cambia il negozio (#82). Se il pacchetto si può tenere chiuso, serve un inventario dei pacchetti.

**Proposta:** il Dream Pack si apre subito, al momento dell'acquisto.

### 17. Inizio del "giorno"

> **Decisione del 2/11:** il giorno inizia a mezzanotte, ora italiana; nel database le date restano in UTC.

**Perché:** accesso giornaliero, missione giornaliera e le 3 vittorie al giorno devono ripartire tutti alla stessa ora.

**Proposta:** a mezzanotte, ora italiana. Nel database le date restano in UTC.

### 18. Nomi italiani delle fazioni

> **Decisione del 2/11:** restano in inglese: Dreams, Nightmares, Memories (opzione A).

| Opzione | Cosa comporta |
|---------|---------------|
| **A. Restano in inglese: Dreams, Nightmares, Memories** (proposta) | Sono già nella presentazione, nei mockup e nelle carte |
| B. In italiano nei testi (Sogni, Incubi, Ricordi) | Vanno cambiati testi, carte e mockup. I codici `DREAMS`, `NIGHTMARES`, `MEMORIES` restano comunque |

### 19. Seconda buyer persona

> **Decisione del 2/11:** no, basta Giulia.

**Proposta:** sì, solo per il piano di marketing (#98). Non cambia niente nel codice. Decide Sara.

---

## D · Tecniche

### 20. Disegno della carta (#81)

> **Decisione del 2/11:** componenti React.

| Opzione | Cosa comporta |
|---------|---------------|
| **A. Canvas** (proposta) | Stesso risultato delle carte di prova, con le stesse coordinate |
| B. Livelli HTML e CSS | Testi selezionabili e più semplici da rendere accessibili |

### 21. Cornici trasparenti e coordinate

> **Decisione del 2/11:** le prepara il frontend in JavaScript (opzione B).

**Perché:** il componente della carta ha bisogno di 12 cornici con la finestra trasparente e delle coordinate di nome, testi, ATT e DIF.

| Opzione | Cosa comporta |
|---------|---------------|
| **A. Le prepara il compositore Python una volta sola** (proposta) | Le misure ci sono già nel compositore. Il frontend riceve i file pronti e un `zone.json` |
| B. Il frontend le calcola in JavaScript | Nessuno script in più, ma si rifanno le misure da capo |

### 22. Durata dei token

> **Decisione del 2/11:** token di accesso 15 minuti, refresh token 7 giorni.

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
| Aggiornare le guide come in [Cosa cambia](#cosa-cambia-dopo-le-decisioni) | Marco, Gaia, Sara |
| Decidere come entrano Sara e Maie nel repository (domanda 3) | Sara, Maie, Cristian |
| Aprire le issue per missioni (Sprint 6), pacchetti di più tipi e skin | Marco, Cristian |
