# Motore di gioco

Simula una partita di Dreamscape tra due mazzi e restituisce il risultato e il log round per round. È un microservizio separato dal backend ([decisione 029](../../docs/decisioni/029-motore-di-gioco.md)): il backend lo chiama via HTTP, il frontend mai.

## Requisiti
- Java 25
- Gradle: non serve installarlo, c'è il wrapper `./gradlew`

## Test
```bash
./gradlew test
```

## Struttura
| Parte | Cosa contiene |
|-------|---------------|
| `engine-core` | Le regole del gioco in Java puro: niente Spring, database o HTTP. Package `it.somnia.dreamscape.engine` |
| `engine-service` | *(passo successivo)* Il servizio Spring Boot che espone il nucleo al backend con `POST /api/v1/simulations` |

Package di `engine-core`:
- `it.somnia.dreamscape.engine`: tipi comuni e risultato. `Side` (`CHALLENGER`, `OPPONENT`), `EndReason` e `BattleResult`, con vincitore, motivo della fine, round, punti vita finali, moneta e log.
- `input`: cosa riceve il motore. `DeckSnapshot` e `CardSnapshot` (le copie dei mazzi), `CardEffect` (codice e valore X), `GameRules` (punti vita, limite di round, vantaggi di fazione `FactionAdvantage`). Il motore rifiuta solo i dati rotti: le regole del mazzo le controlla il backend.
- `battle`: la partita. `BattleEngine.simulate(...)` riceve i due mazzi, le regole, il seed e se serve il log, e restituisce il `BattleResult`. Ogni round ha tre fasi: iniziale (chi non ha una creatura in campo gioca la prima carta), combattimento (due colpi contemporanei con il bonus di fazione; le ferite restano nei round successivi dello stesso scontro) e finale (morte delle creature, danno al giocatore, fine della partita per punti vita, mazzo esaurito o limite di round). Gli effetti delle carte per ora sono ignorati: arrivano con la #86. `ENGINE_VERSION` va cambiata a ogni modifica che cambia l'esito delle partite.
- `setup`: inizio della partita. `MatchSetup.prepare(...)` crea dal seed un solo `Random`, mescola il mazzo di `CHALLENGER`, poi quello di `OPPONENT`, poi tira la moneta. Con lo stesso seed la partita è sempre la stessa.
- `log`: il log della partita, con gli stessi nomi del JSON di `matches.round_log` in `docs/database/schema.md`. `MatchLog` contiene i `RoundLog`; ogni round ha gli eventi in ordine (`GameEvent`: `CardPlayed`, `EffectApplied`, `EffectBlocked`, `Attack`, `CardDied`, `PlayerDamage`, `BonusExpired`, `GameOver`) e lo stato dei due lati a fine round (`AfterRound`, `SideState`).
