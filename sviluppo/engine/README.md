# Motore di gioco

Simula una partita di Dreamscape tra due mazzi e restituisce il risultato e il log round per round. È un microservizio separato dal backend ([decisione 029](../../docs/decisioni/029-motore-di-gioco.md)): il backend lo chiama via HTTP, il frontend mai.

## Requisiti
- Java 25
- Gradle: non serve installarlo, c'è il wrapper `./gradlew`
- Docker, solo per il container del motore

## Avvio
In sviluppo **non serve avviarlo a mano**: `./gradlew bootRun` del backend lo costruisce e lo avvia con Docker Compose, insieme a PostgreSQL e Mailpit ([README del backend](../backend/README.md)). L'immagine si ricostruisce a ogni avvio dal codice di questa cartella: la prima volta ci vuole circa un minuto, poi pochi secondi.

Per provarlo da solo, da questa cartella:
```bash
ENGINE_API_KEY=chiave-di-prova ./gradlew :engine-service:bootRun
```
Il servizio risponde su `http://localhost:8081`. Senza `ENGINE_API_KEY` non parte.

## Test
```bash
./gradlew test
```
I test di `engine-core` provano le regole; quelli di `engine-service` la chiamata, la chiave, gli errori e i nomi dei campi del log, che devono restare quelli di [schema.md](../../docs/database/schema.md#formato-dei-campi-json-di-matches) perché il replay del frontend li legge.

## Configurazione
| Variabile | Descrizione | Esempio |
|-----------|-------------|---------|
| `ENGINE_API_KEY` | Chiave condivisa con il backend, nell'intestazione `X-Engine-Key`. Obbligatoria: senza, il servizio non parte. In sviluppo Docker Compose usa una chiave di prova, la stessa del profilo `dev` del backend | (generata con `openssl rand -base64 32`) |
| `PORT` | Porta del servizio | `8081` |

## Struttura
| Parte | Cosa contiene |
|-------|---------------|
| `engine-core` | Le regole del gioco in Java puro: niente Spring, database o HTTP. Package `it.somnia.dreamscape.engine` |
| `engine-service` | Il servizio Spring Boot che espone il nucleo al backend con `POST /api/v1/simulations`. Package `it.somnia.dreamscape.engine.server`. Non contiene logica di gioco |
| `Dockerfile` | L'immagine di `engine-service`: compila con il JDK, esegue il jar con il solo JRE e senza l'utente root |

Package di `engine-core`:
- `it.somnia.dreamscape.engine`: tipi comuni e risultato. `Side` (`CHALLENGER`, `OPPONENT`), `EndReason`, `BattleResult` (vincitore, motivo della fine, round, punti vita finali, moneta e log) e `SimulationRequest`, il corpo della chiamata a engine-service.
- `input`: cosa riceve il motore. `DeckSnapshot` e `CardSnapshot` (le copie dei mazzi), `CardEffect` (codice e valore X), `EffectCode` (i 19 effetti che il motore conosce), `GameRules` (punti vita, limite di round, vantaggi di fazione `FactionAdvantage`). Il motore rifiuta solo i dati rotti: le regole del mazzo le controlla il backend.
- `battle`: la partita. `BattleEngine.simulate(...)` riceve i due mazzi, le regole, il seed e se serve il log, e restituisce il `BattleResult`. Ogni round ha tre fasi: iniziale (chi non ha una creatura in campo gioca la prima carta), combattimento (due colpi contemporanei con il bonus di fazione; le ferite restano nei round successivi dello stesso scontro) e finale (morte delle creature, danno al giocatore, fine della partita per punti vita, mazzo esaurito o limite di round). Dentro le fasi scattano i 19 effetti del catalogo (decisione 028; i casi lasciati aperti sono nella decisione 031): un effetto sconosciuto ferma la partita con un errore. `ENGINE_VERSION` va cambiata a ogni modifica che cambia l'esito delle partite.
- `setup`: inizio della partita. `MatchSetup.prepare(...)` crea dal seed un solo `Random`, mescola il mazzo di `CHALLENGER`, poi quello di `OPPONENT`, poi tira la moneta. Con lo stesso seed la partita è sempre la stessa.
- `log`: il log della partita, con gli stessi nomi del JSON di `matches.round_log` in `docs/database/schema.md`. `MatchLog` contiene i `RoundLog`; ogni round ha gli eventi in ordine (`GameEvent`: `CardPlayed`, `EffectApplied`, `EffectBlocked`, `Attack`, `CardDied`, `PlayerDamage`, `BonusExpired`, `GameOver`) e lo stato dei due lati a fine round (`AfterRound`, `SideState`).

Il backend **include questo progetto** (`includeBuild '../engine'` in `sviluppo/backend/settings.gradle`) e usa le classi di `engine-core` per la richiesta: la forma della chiamata è scritta una volta sola. Restano due progetti Gradle, ciascuno con il suo wrapper, e i comandi non cambiano.

## API
Una sola chiamata, solo per il backend: il frontend non parla mai con il motore. Nel backend la fa `HttpBattleSimulator`, dietro l'interfaccia `BattleSimulator`.

`POST /api/v1/simulations` con l'intestazione `X-Engine-Key`. Il corpo è `SimulationRequest` di `engine-core`:

```json
{
  "challengerDeck": { "cards": [
    { "cardId": "019a1f30-0000-7000-8000-000000000003", "name": "Il Faro nella Nebbia", "attack": 4, "defense": 7,
      "faction": "DREAMS", "quantity": 2, "effects": [ { "code": "PERMANENCE", "value": 1 } ] }
  ] },
  "opponentDeck": { "cards": [ … ] },
  "rules": { "startingLifePoints": 20, "roundLimit": 60, "factionAdvantages": [
    { "faction": "NIGHTMARES", "target": "DREAMS", "attackBonus": 1, "defenseBonus": 0 }
  ] },
  "seed": 8172635401928374,
  "withLog": true
}
```

- I mazzi sono le stesse copie che il backend salva in `matches.*_deck_snapshot`: i campi in più (`cardCode`, `rarity`, `variantType`…) vengono ignorati.
- Le regole il backend le legge da `game_modes` e `faction_advantages`; il seed lo sceglie e lo salva in `matches.random_seed`.
- `withLog: false` restituisce solo il risultato (simulazioni, #87).

La risposta `200`:

```json
{
  "engineVersion": "0.2.0",
  "winner": "OPPONENT",
  "endReason": "LIFE_POINTS",
  "totalRounds": 24,
  "challengerLifePoints": 0,
  "opponentLifePoints": 6,
  "coinTossWinner": "OPPONENT",
  "log": { "engineVersion": "0.2.0", "coinTossWinner": "OPPONENT", "rounds": [ … ] }
}
```

`winner` è `null` se la partita è pari; `log` è `null` con `withLog: false`. Il formato di `log` è in [schema.md](../../docs/database/schema.md#formato-dei-campi-json-di-matches): il backend lo salva così com'è in `matches.round_log`.

Errori, nello stesso formato del backend (`{ status, code, message }`):

| `code` | Stato | Quando |
|--------|-------|--------|
| `INVALID_ENGINE_KEY` | 401 | Chiave mancante o sbagliata, su qualsiasi indirizzo `/api/` |
| `INVALID_SIMULATION` | 400 | Dati rotti: JSON illeggibile, mazzo vuoto, seed mancante, carta con valori impossibili, effetto sconosciuto. Il motivo è nel messaggio |
| `ENGINE_ERROR` | 500 | Errore inatteso del motore |

Per il backend ogni errore del motore, e il motore spento o troppo lento, diventa `503 ENGINE_UNAVAILABLE`: la partita non si gioca e non si salva niente.
