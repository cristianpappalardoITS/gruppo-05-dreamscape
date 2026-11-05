# Backend di Dreamscape

Applicazione Spring Boot: portale e API REST del gioco, con il database PostgreSQL creato da Flyway.

## Requisiti
- Java 25
- Docker (PostgreSQL in sviluppo e test con Testcontainers)
- Gradle: non serve installarlo, c'è il wrapper `./gradlew`

## Avvio
```bash
cp .env.example .env
./gradlew bootRun
```

`bootRun` parte con il profilo `dev`: Docker Compose avvia PostgreSQL 18 e Mailpit, e Flyway crea tabelle, dati del regolamento e dati finti.

- API su `http://localhost:8080/api/v1`, documentazione Swagger su `http://localhost:8080/swagger-ui.html`, stato su `/actuator/health`.
- Le email del backend (recupero password) non partono davvero: si leggono in Mailpit, su `http://localhost:8025`.
- Nel profilo `dev` la chiave JWT è finta e scritta in `application-dev.properties`: alla consegna serve `JWT_SECRET`.

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

## Struttura
- `src/main/java/it/somnia/dreamscape/api/`: il codice, diviso per funzionalità ([regole-codice.md](../../docs/regole-codice.md)).
- `src/main/resources/db/migration/`: tabelle (`V1`–`V10`) e dati del regolamento (`V100`). Una migration già eseguita non si modifica.
- `src/main/resources/db/testdata/`: dati finti (`V900`–`V902`), caricati solo nel profilo `dev` e nei test.

Lo schema è spiegato in [docs/database/schema.md](../../docs/database/schema.md).

## Dati finti (profilo `dev` e test)
- **Carte:** 39 carte del Set di prova (`TEST-001`–`TEST-039`): per ogni fazione 7 Comuni, 3 Rare, 2 Epiche e 1 Leggendaria con la sua full art (`TEST-013-FA`, `TEST-026-FA`, `TEST-039-FA`). Ognuno dei 19 effetti è su almeno una carta.
- **Pacchetti:** Pacchetto di prova (500 DC), tre set iniziali di prova (uno per fazione), due pacchetti di benvenuto di prova.
- **Utenti di prova:** partono con 500 DC e la fazione ancora da scegliere. `anna_test` e `bruno_test` sono amici; `carla_test` ha chiesto l'amicizia a `bruno_test`.

| Username | Email | Password |
|----------|-------|----------|
| `anna_test` | `anna_test@example.test` | `prova-dreamscape` |
| `bruno_test` | `bruno_test@example.test` | `prova-dreamscape` |
| `carla_test` | `carla_test@example.test` | `prova-dreamscape` |

Sono password di prova, valide solo sul database locale.
