# Motore di gioco

Simula una partita di Dreamscape tra due mazzi e restituisce il risultato e il log round per round. È un microservizio separato dal backend ([decisione 029](../../docs/decisioni/029-motore-di-gioco.md)): il backend lo chiama via HTTP, il frontend mai.

Oggi c'è il nucleo, `engine-core`, versione `0.2.0` (`BattleEngine.ENGINE_VERSION`): la partita completa con i 19 effetti delle carte. Il servizio HTTP che lo collega al backend arriva con la #161.

## Requisiti
- Java 25
- Gradle: non serve installarlo, c'è il wrapper `./gradlew`
- Docker **non** serve: il nucleo non usa database

## Test
```bash
./gradlew test
```
I test giocano partite vere con seed fissi, quindi il risultato è sempre lo stesso. Un test solo, o una classe: `./gradlew test --tests "*BattleEngineTest"`. Il resoconto si apre da `engine-core/build/reports/tests/test/index.html`.

## Come si usa
```java
BattleResult result = new BattleEngine().simulate(challengerDeck, opponentDeck, rules, seed, true);
```
- `challengerDeck` e `opponentDeck` sono `DeckSnapshot`: la copia dei due mazzi presa al momento della partita.
- `rules` è `GameRules`: punti vita, limite di round e vantaggi tra fazioni, letti dal backend nel database (modalità `BASE`).
- `seed` decide mescolamento e moneta: con lo stesso seed la partita è sempre la stessa, così si può rigiocare.
- L'ultimo valore dice se costruire il log round per round.

`BattleResult` dice chi ha vinto, perché è finita, quanti round, i punti vita finali, chi ha vinto la moneta e il log.

## Struttura
| Parte | Cosa contiene |
|-------|---------------|
| `engine-core` | Le regole del gioco in Java puro: niente Spring, database o HTTP. Package `it.somnia.dreamscape.engine` |
| `engine-service` | *(#161)* Il servizio Spring Boot che espone il nucleo al backend con `POST /api/v1/simulations` |

Package di `engine-core`:
- `it.somnia.dreamscape.engine`: tipi comuni e risultato. `Side` (`CHALLENGER`, `OPPONENT`), `EndReason` e `BattleResult`, con vincitore, motivo della fine, round, punti vita finali, moneta e log.
- `input`: cosa riceve il motore. `DeckSnapshot` e `CardSnapshot` (le copie dei mazzi), `CardEffect` (codice e valore X), `EffectCode` (i 19 effetti che il motore conosce), `GameRules` (punti vita, limite di round, vantaggi di fazione `FactionAdvantage`). Il motore rifiuta solo i dati rotti: le regole del mazzo le controlla il backend.
- `battle`: la partita. `simulate(...)` di `BattleEngine` riceve i due mazzi, le regole, il seed e se serve il log, e restituisce il `BattleResult`. Ogni round ha tre fasi: iniziale (chi non ha una creatura in campo gioca la prima carta), combattimento (due colpi contemporanei con il bonus di fazione; le ferite restano nei round successivi dello stesso scontro) e finale (morte delle creature, danno al giocatore, fine della partita per punti vita, mazzo esaurito o limite di round). Dentro le fasi scattano i 19 effetti del catalogo (decisione 028; i casi lasciati aperti sono nella decisione 031): un effetto sconosciuto ferma la partita con un errore. `ENGINE_VERSION` va cambiata a ogni modifica che cambia l'esito delle partite.
- `setup`: inizio della partita. `MatchSetup.prepare(...)` crea dal seed un solo `Random`, mescola il mazzo di `CHALLENGER`, poi quello di `OPPONENT`, poi tira la moneta. Con lo stesso seed la partita è sempre la stessa.
- `log`: il log della partita, con gli stessi nomi del JSON di `matches.round_log` in `docs/database/schema.md`. `MatchLog` contiene i `RoundLog`; ogni round ha gli eventi in ordine (`GameEvent`: `CardPlayed`, `EffectApplied`, `EffectBlocked`, `Attack`, `CardDied`, `PlayerDamage`, `BonusExpired`, `GameOver`) e lo stato dei due lati a fine round (`AfterRound`, `SideState`).
