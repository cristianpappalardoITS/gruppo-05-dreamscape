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
- Le immagini caricate dal backoffice finiscono in `data/images` (copie WebP, servite su `/images/...`) e `data/originals` (originali), fuori da git.

### Con un admin
Il backoffice (`/api/v1/admin/...`) vuole un utente con `role: ADMIN`. Il primo admin si crea all'avvio con tre variabili d'ambiente (non stanno nel file `.env`, che serve solo a Docker Compose); se esiste già, non cambia:
```bash
ADMIN_USERNAME=admin ADMIN_EMAIL=admin@example.test ADMIN_PASSWORD=una-password-lunga ./gradlew bootRun
```
In PowerShell:
```powershell
$env:ADMIN_USERNAME="admin"; $env:ADMIN_EMAIL="admin@example.test"; $env:ADMIN_PASSWORD="una-password-lunga"; ./gradlew bootRun
```

### Dopo una migration nuova
I dati finti (`V900` e seguenti) hanno numeri più alti delle migration vere. Se arriva una migration vera nuova (per esempio `V101`) e il database di sviluppo ha già i dati finti, Flyway non la applica e `bootRun` si ferma con un errore di validazione. Si ricrea il database di sviluppo, che riparte con tutti i dati finti:
```bash
docker compose down -v
./gradlew bootRun
```
I dati scritti a mano nel database di sviluppo si perdono. I test non ne hanno bisogno: Testcontainers crea ogni volta un database nuovo.

## Documentazione delle API (Swagger)
Con il backend avviato, `http://localhost:8080/swagger-ui.html` elenca tutti gli endpoint, divisi in gruppi (Accesso, Profilo, Benvenuto, Catalogo, Negozio, Collezione, Mazzi e i tre del Backoffice). Ogni endpoint ha una descrizione, i campi con un esempio e le risposte possibili con i codici di errore. La descrizione in formato OpenAPI è su `/v3/api-docs`.

Per provare un endpoint con il lucchetto:
1. Apri **Accesso** → `POST /api/v1/auth/login` → **Try it out**: l'esempio è già `anna_test` con `prova-dreamscape`. Premi **Execute**.
2. Copia `accessToken` dalla risposta, premi **Authorize** in alto a destra e incollalo (senza la parola Bearer).
3. Ora rispondono anche gli endpoint con il lucchetto. Il token dura 15 minuti: poi si rifà l'accesso. Swagger lo ricorda anche se ricarichi la pagina.

Per il backoffice si fa l'accesso con l'admin (vedi sopra). Le pagine del frontend, con gli esempi in JavaScript, sono spiegate nella [guida per il frontend](../../docs/api/guida-frontend.md).

Gli endpoint nuovi vanno documentati nel controller, come gli altri: `@Tag` sulla classe (uno dei gruppi esistenti o uno nuovo, in italiano), `@Operation` con `summary` su ogni metodo e un `@ApiResponse` per ogni risposta, con i codici di errore. Gli endpoint pubblici hanno anche `@SecurityRequirements` (senza lucchetto). Il `401`, il `403` del backoffice e il formato degli errori li aggiunge da solo `OpenApiConfig`. Se manca il titolo o il gruppo, `OpenApiDocsTest` fallisce.

## Test
```bash
./gradlew test
```
Serve Docker acceso: Testcontainers avvia un PostgreSQL nuovo per i test, con le migration e i dati finti. Un test solo, o una classe: `./gradlew test --tests "*DeckApiTest"`. Il resoconto si apre da `build/reports/tests/test/index.html`.

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
| `IMAGES_DIR` | Cartella delle immagini servite su `/images/...` (copie WebP). In locale `data/images`, fuori da git | `/data/images` |
| `ORIGINALS_DIR` | Cartella dei PNG e JPEG originali caricati dal backoffice: non si serve al pubblico. In locale `data/originals` | `/data/originals` |
| `GOOGLE_CLIENT_ID` | Client id OAuth di Google Cloud per l'accesso con Google (decisione 032). Vuota = accesso con Google spento (`503 GOOGLE_LOGIN_DISABLED`) | `123…apps.googleusercontent.com` |

## Struttura
- `src/main/java/it/somnia/dreamscape/api/`: il codice, diviso per funzionalità ([regole-codice.md](../../docs/regole-codice.md)). Oggi hanno le API `user` (accesso, profilo, crediti), `card` (catalogo), `deck` (collezione e mazzi), `pack` (benvenuto, negozio, aperture) e `admin` (backoffice); le altre cartelle (amici, scambi, partite, sfide, leghe, missioni, contatti) sono ancora quasi vuote. In `common/` ci sono configurazione (anche `OpenApiConfig` per Swagger), errori, sicurezza e salvataggio dei file.
- `src/main/resources/db/migration/`: tabelle (`V1`–`V11`), dati del regolamento (`V100`) e pacchetti (`V101`). Una migration già eseguita non si modifica.
- `src/main/resources/db/testdata/`: dati finti (`V900`–`V903`), caricati solo nel profilo `dev` e nei test.

Lo schema è spiegato in [docs/database/schema.md](../../docs/database/schema.md).

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
