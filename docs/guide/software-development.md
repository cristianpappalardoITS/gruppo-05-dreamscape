# Guida per Software Development · si parte il 2 novembre

Per **Cristian, Marco e Nicolas**. Raccoglie le regole e le decisioni da seguire nel motore di gioco, nel backend e nel database, e quello che dobbiamo agli altri due team: le API che Web usa e i dati che Digital ci consegna.

> Le cose ancora da decidere sono in [cose da decidere del 2/11](../riunioni/2026-11-02-cose-da-decidere.md). Le guide degli altri team: [Web Development](web-development.md) e [Digital Strategy](digital-strategy.md). Quello che promettiamo a loro qui deve restare vero nel codice.

## In breve: le 10 regole da non sbagliare

1. **Il motore è Java puro:** niente Spring, JPA, database o HTTP in `engine/`. Riceve i dati e un `Random`, restituisce il risultato.
2. **Nessun numero di gioco nel codice:** punti vita, round, limiti, vantaggi, prezzi e pesi stanno in `GameRules` o nelle tabelle (`game_modes`, `rarities`, `faction_advantages`, `packs`).
3. **Controller → Service → Repository.** La logica sta nel service, le transazioni pure. Le entità non escono mai dalle API: si usano i DTO.
4. **Le API sono un contratto con Web:** nomi, campi ed errori come in [guida-frontend.md](../api/guida-frontend.md). Se cambiano, si aggiorna la guida nella stessa PR e si avvisa Gaia.
5. **Lo schema cambia solo con Flyway:** mai a mano, mai modificare una migration già eseguita. `ddl-auto=validate`.
6. **Id UUID v7 ovunque**, generati da Hibernate. Nelle migration con i dati ci si riferisce alle righe con `code`, mai con id scritti a mano.
7. **Crediti interi e operazioni atomiche:** acquisti, ricompense e scambi in una transazione, senza mai crediti negativi né carte duplicate.
8. **Il server decide tutto:** estrazione dei pacchetti, partite, validità dei mazzi, scambi. Mai fidarsi di quello che manda il browser.
9. **Niente segreti nel repository:** password, chiavi e credenziali arrivano da variabili d'ambiente. Nel repository solo `.env.example` con valori finti.
10. **Ogni regola del regolamento ha il suo test.** Prima di aprire una PR tutti i test passano.

---

## 1. Prima di iniziare

| Documento | Perché |
|-----------|--------|
| [workflow-github.md](../workflow-github.md) | Issue, branch, commit, pull request |
| [regole-codice.md](../regole-codice.md) | Package per funzionalità, motore, backend, database, sicurezza, test |
| [naming.md](../naming.md) e [commenti.md](../commenti.md) | Nomi in inglese, Javadoc e commenti in italiano |
| [schema.md](../database/schema.md) e [schema.sql](../database/schema.sql) | Le 31 tabelle, le scelte, le migration, i dati finti |
| [docs/decisioni/](../decisioni/README.md) | Cosa si è deciso e perché |
| [guida-frontend.md](../api/guida-frontend.md) | Le API che abbiamo promesso a Web, pagina per pagina |
| [Regolamento](../proposte/proposta-regolamento-dreamscape.pdf) | Round, effetti, fine della partita |
| [template.md](../template.md) | Classi del motore, service, test JUnit, README di modulo |
| [checklist-pr.md](../checklist-pr.md) | Da rileggere prima di ogni pull request |

Tre cose da fare prima del primo branch di codice (domande 1, 2 e 7 del [2/11](../riunioni/2026-11-02-cose-da-decidere.md)): creare `develop`, aggiungere `.editorconfig` nella radice, decidere come si avvia il frontend insieme al backend.

## 2. Come si lavora su GitHub

- Branch da `develop`: `feature/71-accesso-jwt`, `fix/130-saldo-negativo`, `test/88-integrazione-pacchetti`.
- Commit: `feat(user): aggiunge il refresh token in cookie HttpOnly (#71)`. Ambiti: `engine`, `user`, `card`, `pack`, `deck`, `trade`, `challenge`, `db`, `docs`.
- PR verso `develop` con `Closes #numero`, approvata da **un'altra persona di SWD**. Chi apre la PR non la approva.
- Al massimo 2 issue In Progress a testa. Le card della board le spostano Marco e Cristian.

## 3. Cosa è già deciso

### Tecnologie

| Livello | Scelta |
|---------|--------|
| Motore | Java, modulo `engine/` separato, test JUnit |
| Backend | Java + Spring Boot 4, Spring Data JPA (Hibernate 7), Spring Security con JWT |
| Database | PostgreSQL 18, UUID v7, migration Flyway, test con Testcontainers su PostgreSQL vero (non H2) |
| Avvio | Un solo `docker compose up` per backend, database e frontend (#68, #100) |
| Package base | `it.gruppo5.tcg`, diviso per funzionalità (`deck/`, `trade/`...) |

### Accesso

- Token di accesso JWT nella risposta di `/auth/login`, che il frontend tiene **solo in memoria**.
- Refresh token in un cookie **`HttpOnly`**, salvato nella tabella `refresh_tokens` e rinnovato da `/auth/refresh`.
- Password con BCrypt. Il primo admin nasce all'avvio da variabili d'ambiente.
- Ruoli: `PLAYER` e `ADMIN`. Si entra da `/accesso` per tutti.

### Numeri del gioco (confermati il 31/10)

Gli stessi numeri sono negli esempi di [schema.md](../database/schema.md#esempio-pacchetti) e di [guida-frontend.md](../api/guida-frontend.md). Nelle migration vanno questi:

| Cosa | Valore | Dove sta |
|------|--------|----------|
| Dream Pack | 5 carte, **500 DC**, tre copertine (una per Lord). Si apre subito, all'acquisto | `packs`, `pack_covers` |
| Altri pacchetti | Il cliente ne vuole di più tipi, a prezzi diversi: tipi, prezzi e probabilità li decidono Maie e Nicolas. Il codice non dà per scontato un solo pacchetto | `packs`, `pack_slots` |
| Slot del Dream Pack | 3 Comuni · 1 Rara · 1 slot speciale: Rara 70, Epica 25, Leggendaria 5 | `pack_slots`, `pack_slot_rarities` |
| Full art | 20% delle Leggendarie, cioè 1% del Dream Pack | `packs.full_art_chance` |
| Registrazione | 500 DC alla registrazione. Nella pagina Benvenuto il Dreamweaver sceglie una fazione e riceve il set iniziale di quella fazione (14 Comuni, 6 Rare) + 2 pacchetti di benvenuto (uno con Epica garantita) | `POST /me/welcome` ([guida](../api/guida-frontend.md#benvenuto-dopo-la-registrazione)) |
| Accesso giornaliero | 50 DC. Il giorno inizia a mezzanotte, ora italiana (`Europe/Rome`); nel database le date restano in UTC | `credit_transactions` (`DAILY_REWARD`) |
| Vittoria | 30 DC, massimo 3 al giorno, solo nelle partite classificate e non contro un amico | `credit_transactions` (`MATCH_REWARD`) |
| Rank | Elo con K = 32, si parte da 1000. Stesso avversario: rank e DC al massimo 3 volte al giorno | `users.rating`, `matches.*_rating_change` |
| Leghe | Dormiente, Sognatore, Lucido, Dreamweaver. Premio la prima volta che si entra; soglie e premi li propone Maie | `leagues`, `user_league_rewards` (`LEAGUE_REWARD`) |
| Missione giornaliera, obiettivo settimanale | 100 DC, 250 DC. Versione semplice, fissa, nello Sprint 6. La settimana inizia il lunedì a mezzanotte, ora italiana | `missions`, `user_missions` (`MISSION_REWARD`); issue da aprire (una SWD, una WMD) |
| Mazzo | 20 carte, max 2 copie (1 per le Leggendarie), Leggendarie 2, Epiche 4, Rare 8. Si salvano solo i mazzi validi | `GameRules`, `rarities` |
| Partita | 20 punti vita (confermati dal cliente, da verificare con #87), max 60 round, poi pareggio | `game_modes`, copiati in `matches` |
| Vantaggio di fazione | Dreams → Memories → Nightmares → Dreams, **+1 ATT** | `faction_advantages` |
| Effetti | I 23 della [lista di Nicolas](../../carte/effetti/Effetti%20Keyword%20Descrizioni.txt), al massimo uno per carta | `effects`, `card_effects` |

### Regole del gioco già decise

- Una sola modalità, la **Base**, con gli effetti.
- **Scambi solo tra amici**, quantità e rarità libere, niente crediti negli scambi.
- **Partite:** regole della Base per tutte. **Classificata** con il matchmaking (avversario con punteggio simile, anche non collegato, con il suo mazzo classificato `users.ranked_deck_id`): rank e DC. **Rapida** con il matchmaking (avversario a caso) o sfidando un amico: niente rank né DC. Il motore non cambia: cambia solo cosa fa il service dopo la partita ([schema.md](../database/schema.md#partite)).
- Il set iniziale è di **una fazione**, scelta dal Dreamweaver nella pagina Benvenuto, e si estrae senza superare le copie massime: con quelle 20 carte si compone sempre un mazzo valido. Il mazzo non si crea in automatico.
- Le copie offerte in uno scambio in attesa sono **bloccate per gli altri scambi** (si contano dagli scambi `PENDING`, con `SELECT ... FOR UPDATE` alla proposta), ma restano usabili nei mazzi. Alla proposta e all'accettazione si avvisa se un mazzo diventerà non valido. All'accettazione si ricontrolla tutto e, se mancano carte, lo scambio si annulla da solo con un messaggio.
- Token di accesso **15 minuti**, refresh token **7 giorni**.
- Lo storico delle partite usa le **copie dei mazzi** salvate in `matches` e il `random_seed`: con lo stesso seed il motore rigioca la stessa partita.

## 4. Il motore di gioco (`engine/`)

- Riceve i mazzi già caricati (es. `DeckSnapshot`), le regole (`GameRules`) e un `Random`. Restituisce il risultato e il log round per round. Non legge il database.
- Il log usa i nomi del regolamento: un **round** è un singolo scambio di colpi, uno **scontro** è la serie di round tra le stesse due creature. Il formato JSON è in [schema.md](../database/schema.md#formato-dei-campi-json-di-matches): Web lo usa per il replay (#97).
- L'ordine degli effetti lo decide il lancio della moneta, preso dallo stesso `Random`.
- Ogni regola e ogni effetto ha un test JUnit con seed fisso: `calculateDamage_attackerHasFactionAdvantage_addsBonus`.
- Le simulazioni (#87) usano lo stesso motore, senza copiarne la logica.

```java
// ✅ Regole e Random arrivano da fuori: stessa partita con lo stesso seed
BattleResult result = engine.simulate(first, second, rules, new Random(seed));

// ❌ Valori scritti nel codice e Random nascosto
if (lifePoints <= 0 || round > 60) { ... }
Collections.shuffle(cards);
```

## 5. Backend e database

### Struttura (proposta, da fissare nella #68)

```
backend/src/main/java/it/gruppo5/tcg/
├── user/        # registrazione, accesso, profilo, crediti
├── card/        # catalogo, fazioni, rarità, effetti, regole
├── pack/        # Dream Pack, aperture, set iniziale e benvenuto
├── deck/        # collezione e mazzi
├── friend/      # amicizie
├── trade/       # scambi
├── challenge/   # sfide e partite
├── admin/       # backoffice
└── common/      # errori, sicurezza, configurazione, FileStorage
```

### API

- Prefisso `/api/v1`, risorse in inglese al plurale, JSON in `camelCase`, id UUID come testo, date ISO 8601 in UTC.
- Liste lunghe paginate con `page` e `size`, risposta `{ items, page, size, totalItems, totalPages }`.
- Un solo `@RestControllerAdvice`: ogni errore esce come `{ status, code, message, fieldErrors }`, con i `code` della guida (es. `USERNAME_TAKEN`, `NOT_ENOUGH_CREDITS`).
- Immagini restituite come percorsi **relativi** (`/images/cards/base-004-v1.webp`), mai con il dominio.
- Degli altri giocatori si restituisce solo `id` e `username`: mai email o crediti.
- Un endpoint che serve a Web e non c'è nella guida si aggiunge **prima** nella guida, poi nel codice. Swagger (#99) diventa l'elenco definitivo.

### Database

| ✅ | ❌ |
|----|----|
| Migration `V1`–`V8` per le tabelle, come nella [divisione proposta](../database/schema.md#divisione-proposta) | Tabelle create da Hibernate o a mano |
| Catalogo vero in `V100` e successive, generato da `carte/compositore/carte.csv` | Carte inserite dal backoffice e poi anche da una migration (Flyway fallisce sul codice duplicato) |
| Dati finti in `db/testdata/`, da `V900`, codici `TEST-`, solo nel profilo `dev` e nei test | Dati finti in `db/migration/`. (In un punto di `schema.md` la cartella si chiama `db/dev-data`: vale `db/testdata`) |
| Correggere una carta con una migration nuova (`UPDATE`) | Modificare una migration già eseguita |
| Indice sulle chiavi esterne usate nelle ricerche | Chiavi esterne senza indice |
| Date in `timestamptz`, `Instant` in Java, tutto in UTC | `LocalDateTime` senza fuso |

### Transazioni che non devono sbagliare

| Operazione | Regola |
|------------|--------|
| Acquisto del Dream Pack (#78) | Controllo del saldo, addebito, estrazione e carte nella collezione in **una** transazione. L'apertura ha il suo id: ricaricare la pagina non ricompra |
| Ricompensa giornaliera (#115) | Un `UPDATE ... WHERE last_daily_reward_on < :oggi` atomico: due richieste insieme danno una sola ricompensa |
| Scambio (#93) | `SELECT ... FOR UPDATE` sulle righe di entrambi, ricontrollo di amicizia e carte, poi lo spostamento |
| Sfida (#94) | Ricontrollo dei due mazzi all'accettazione, poi partita e premio nella stessa transazione |

### Pensato per un possibile rilascio su AWS

La proposta è in `docs/proposte/rilascio-aws.md`, sul branch `docs/rilascio-aws`, non ancora unito. Non è nella consegna, ma alcune regole costano poco se si seguono da subito:

- Configurazione solo da variabili d'ambiente (`DB_URL`...), lette da un `.env` che resta fuori dal repository.
- Backend senza stato: niente `HttpSession`, niente file scritti su disco se non tramite l'interfaccia `FileStorage`.
- Log sulla console, controllo di salute su `/actuator/health`.
- I lavori programmati chiamano un service e si possono ripetere senza danni.

## 6. Cosa dobbiamo agli altri team

| Team | Cosa si aspetta da noi | Entro |
|------|------------------------|-------|
| Web | Backend avviabile con `docker compose up` e `/auth/*` funzionanti, come nella guida (#68, #71). Sito e API sullo stesso indirizzo: il container del frontend manda `/api` al backend. Web non usa dati finti, quindi aspetta questi due | Sprint 3 |
| Web | API del catalogo, `GET /rules`, Dream Pack, collezione e mazzi (#77, #78, #80) | Sprint 4 |
| Web | Guida ai dati aggiornata con i numeri nuovi | Dopo il 2/11 |
| Digital | Portare nel repository i file che Digital consegna sul Drive (Marco o Cristian, con una PR) e controllare `carte.csv` (#74): codici, fazioni, rarità ed effetti scritti come vuole lo script | Sprint 3 |
| Digital | Le copie WebP di illustrazioni e copertine, create dallo script dai PNG originali | Sprint 4–5 |

## 7. Le nostre issue dei primi sprint

| Issue | Chi | Sprint | Da leggere prima |
|-------|-----|--------|------------------|
| #68 Progetto Spring Boot con Docker | Cristian | 3 | [Configurazione Spring Boot](../database/schema.md#configurazione-spring-boot); verificare che Flyway supporti PostgreSQL 18 |
| #70 Modulo del motore | Cristian | 3 | Sezione 4; [template della classe del motore](../template.md#template-classe-del-motore-di-gioco) |
| #69 Migration e dati finti | Marco | 3 | [Migration Flyway](../database/schema.md#migration-flyway), [schema.sql](../database/schema.sql) |
| #71 Registrazione e accesso JWT | Marco | 3 | [Accesso con JWT](../api/guida-frontend.md#3-accesso-con-jwt); durata dei token (domanda 22) |
| #76 Decisioni del colloquio | Nicolas | 3 | Le risposte del cliente (domanda 11) vanno in `docs/decisioni/` |
| #74 Catalogo della fase 1, con Digital | Nicolas | 3 | [Guida di Digital](digital-strategy.md#4-il-catalogo-delle-carte-74); rarità e statistiche (domanda 14) |
| #77 API del catalogo | Cristian | 4 | Cache delle immagini nella [guida](../api/guida-frontend.md#cache-cosa-resta-nel-browser) |
| #80 Collezione e mazzi | Cristian | 4 | Mazzi non validi (domanda 5) |
| #78 Crediti e Dream Pack | Marco | 4 | Numeri della sezione 3; apertura immediata (domanda 16) |
| #79 Round del motore senza effetti | Marco | 4 | Regolamento, esempio del round 3 |
| #86 Effetti nel motore | Cristian | 5 | Lista di Nicolas, un test per effetto |
| #87 Simulazioni di punti vita e round | Marco, con Maie | 5 | Punti vita e round (domanda 11, risposta del cliente) |

## 8. A chi chiedere

| Domanda | Chi |
|---------|-----|
| Regolamento, effetti, valori di gioco, catalogo | Nicolas |
| Architettura, motore, progetto Spring Boot | Cristian |
| Database, accesso, pacchetti, scambi, Docker | Marco |
| Come una pagina usa un'API | Gaia e chi sviluppa la pagina |
| Numeri dell'economia, testi | Sara, Maie |
