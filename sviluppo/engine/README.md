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
- `it.somnia.dreamscape.engine`: tipi comuni, es. `Side` (`CHALLENGER`, `OPPONENT`).
- `setup`: inizio della partita. `MatchSetup.prepare(...)` crea dal seed un solo `Random`, mescola il mazzo di `CHALLENGER`, poi quello di `OPPONENT`, poi tira la moneta. Con lo stesso seed la partita è sempre la stessa.
