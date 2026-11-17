# Backend di Dreamscape

Applicazione Spring Boot: portale e API REST del gioco, con il database PostgreSQL creato da Flyway.

## Requisiti
- Java 25
- Docker (PostgreSQL e motore di gioco in sviluppo, test con Testcontainers)
- Gradle: non serve installarlo, c'è il wrapper `./gradlew`

## Avvio
```bash
cp .env.example .env
./gradlew bootRun
```

`bootRun` parte con il profilo `dev`: Docker Compose avvia PostgreSQL 18, Mailpit e il motore di gioco, e Flyway crea tabelle, dati del regolamento e dati finti.

- API su `http://localhost:8080/api/v1`, documentazione Swagger su `http://localhost:8080/swagger-ui.html`, stato su `/actuator/health`.
- Le email del backend (recupero password) non partono davvero: si leggono in Mailpit, su `http://localhost:8025`.
- Nel profilo `dev` la chiave JWT è finta e scritta in `application-dev.properties`: alla consegna serve `JWT_SECRET`.
- Il motore di gioco ([decisione 029](../../docs/decisioni/029-motore-di-gioco.md)) gira nel container `engine`, costruito a ogni avvio da `sviluppo/engine` (la prima volta circa un minuto, poi pochi secondi) e raggiungibile solo da questo computer su `http://localhost:8081`. Backend e motore usano la stessa chiave di prova, quindi in sviluppo non serve impostare niente.

### Dopo una migration nuova
I dati finti (`V900` e seguenti) hanno numeri più alti delle migration vere. Se arriva una migration vera nuova (per esempio `V101`) e il database di sviluppo ha già i dati finti, Flyway non la applica e `bootRun` si ferma con un errore di validazione. Si ricrea il database di sviluppo, che riparte con tutti i dati finti:
```bash
docker compose down -v
./gradlew bootRun
```
I dati scritti a mano nel database di sviluppo si perdono. I test non ne hanno bisogno: Testcontainers crea ogni volta un database nuovo.

## Test
```bash
./gradlew test
```

## Configurazione
| Variabile | Descrizione | Esempio |
|-----------|-------------|---------|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Database avviato da Docker Compose in sviluppo, nel file `.env` (fuori dal repository) | `dreamscape` |
| `DB_URL` | URL del database alla consegna | `jdbc:postgresql://localhost:5432/dreamscape` |
| `DB_USERNAME`, `DB_PASSWORD` | Credenziali del database alla consegna | `dreamscape` |
| `JWT_SECRET` | Chiave del token di accesso, almeno 32 caratteri casuali. Obbligatoria fuori dal profilo `dev` | (generata con `openssl rand -base64 48`) |
| `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Primo admin, creato all'avvio se non esiste. Se ne manca uno, nessun admin | `admin` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Server SMTP per le email alla consegna. Senza `SPRING_MAIL_HOST` il link di recupero si scrive solo nel log | `smtp.example.com` |
| `MAIL_FROM` | Mittente delle email | `noreply@dreamscape.local` |
| `PASSWORD_RESET_URL` | Pagina "Nuova password" del frontend; il token si aggiunge in fondo | `https://…/nuova-password.html?token=` |
| `ENGINE_URL` | Indirizzo del motore di gioco (engine-service) | `http://localhost:8081` |
| `ENGINE_API_KEY` | Chiave condivisa con il motore, mandata in `X-Engine-Key`; deve essere uguale a quella del motore. Vuota = le partite rispondono `503 ENGINE_UNAVAILABLE`. Nel profilo `dev` c'è una chiave di prova | (generata con `openssl rand -base64 32`) |
| `GOOGLE_CLIENT_ID` | Client id OAuth di Google Cloud per l'accesso con Google (decisione 032). Vuota = accesso con Google spento (`503 GOOGLE_LOGIN_DISABLED`) | `123…apps.googleusercontent.com` |

## Struttura
- `src/main/java/it/somnia/dreamscape/api/`: il codice, diviso per funzionalità ([regole-codice.md](../../docs/regole-codice.md)).
- `src/main/resources/db/migration/`: tabelle (`V1`–`V11`), dati del regolamento (`V100`) e pacchetti (`V101`). Una migration già eseguita non si modifica.
- `src/main/resources/db/testdata/`: dati finti (`V900`–`V903`), caricati solo nel profilo `dev` e nei test.

Lo schema è spiegato in [docs/database/schema.md](../../docs/database/schema.md).

## Motore di gioco
- Il backend chiede le partite al motore con l'interfaccia `BattleSimulator` (`match/service`); l'implementazione `HttpBattleSimulator` chiama `POST /api/v1/simulations` di engine-service. Richiesta, risposta ed errori sono nel [README del motore](../engine/README.md#api).
- La richiesta usa le classi di `engine-core` (`SimulationRequest`, `DeckSnapshot`, `GameRules`): `settings.gradle` include il progetto `../engine` e `build.gradle` ha la dipendenza `it.somnia:engine-core`. Non serve pubblicare niente: Gradle compila il motore insieme al backend.
- Se il motore è spento, oltre il tempo massimo (5 secondi) o risponde con un errore, `BattleSimulator` lancia `EngineUnavailableException` (`503 ENGINE_UNAVAILABLE`): chi lo usa non salva niente e non dà crediti né punti. Il motivo vero è nel log del backend.
- Per l'immagine Docker del backend (deploy, #100) il contesto della build deve essere `sviluppo/`, non `sviluppo/backend/`: alla build servono anche i file del motore.

## Dati finti (profilo `dev` e test)
- **Carte:** 39 carte del Set di prova (`TEST-001`–`TEST-039`): per ogni fazione 7 Comuni, 3 Rare, 2 Epiche e 1 Leggendaria con la sua full art (`TEST-013-FA`, `TEST-026-FA`, `TEST-039-FA`). Ognuno dei 19 effetti è su almeno una carta.
- **Pacchetti:** valgono quelli veri di `V101` (decisioni 014 e 030: Nap Pack, Dream Pack, Lucid, Faceless e Still Time Pack, REM Pack, tre set iniziali e due Welcome Pack), che pescano anche dalle carte di prova. I pacchetti di prova di `V901` sono spenti da `V903`.
- **Utenti di prova:** partono con 500 DC e la fazione ancora da scegliere, quindi possono provare la pagina Benvenuto (`POST /me/welcome`) e poi comprare un Dream Pack. `anna_test` e `bruno_test` sono amici; `carla_test` ha chiesto l'amicizia a `bruno_test`.

| Username | Email | Password |
|----------|-------|----------|
| `anna_test` | `anna_test@example.test` | `prova-dreamscape` |
| `bruno_test` | `bruno_test@example.test` | `prova-dreamscape` |
| `carla_test` | `carla_test@example.test` | `prova-dreamscape` |

Sono password di prova, valide solo sul database locale.
