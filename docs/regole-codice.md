# Regole generali per il codice

Regole valide per tutto il codice del progetto. Per i nomi vedi [naming.md](naming.md), per i commenti [commenti.md](commenti.md).

## Organizzazione dei package del backend

Il package base del backend è `it.somnia.dreamscape.api`; quello del motore di gioco è `it.somnia.dreamscape.engine`.

I package del backend sono divisi **per funzionalità** (`deck/`, `trade/`...), non per tipo. Dentro ogni funzionalità ci sono le stesse sottocartelle: `controller/`, `service/`, `repository/`, `model/` (entità JPA) e `dto/` con `request/` e `response/`. Le cartelle vuote hanno un file `.gitkeep`, da togliere quando arriva la prima classe.

```
deck/
├── controller/
│   └── DeckController.java
├── service/
│   └── DeckService.java
├── repository/
│   └── DeckRepository.java
├── model/
│   ├── Deck.java            # entità JPA
│   └── DeckCard.java        # entità JPA (tabella di relazione)
└── dto/
    ├── request/
    │   └── DeckRequest.java
    └── response/
        └── DeckResponse.java
```

`admin/` non ha `model/` né `repository/`: usa quelli delle altre funzionalità. `common/` ha `config/`, `security/`, `exception/` e `storage/`.

## Lingua

| Cosa | Lingua |
|------|--------|
| Nomi di classi, metodi, variabili, tabelle, endpoint | **Inglese** |
| Commenti, Javadoc, documentazione, issue, PR | **Italiano** |
| Tipo del commit (`feat`, `fix`...) | Inglese |
| Descrizione del commit | Italiano |

```java
// ✅ Nome in inglese, commento in italiano
// Il danno non può essere negativo anche con modificatori futuri
int damage = Math.max(0, attack - shield);

// ❌ Nomi misti
int dannoTotale = calcolaDamage(cartaAttacker);
```

## Formattazione

- Codifica **UTF-8**, fine riga **LF**.
- Indentazione: **4 spazi** per Java e SQL, **2 spazi** per HTML, CSS, JS, JSON, YAML. Mai tab.
- Lunghezza massima della riga: **120 caratteri**.
- Parentesi graffe sempre, anche per `if` di una riga.
- Una classe pubblica per file.
- Niente import con `*`; niente import inutilizzati.

```java
// ✅
if (deck.isEmpty()) {
    return BattleResult.draw();
}

// ❌
if (deck.isEmpty()) return BattleResult.draw();
```

Il file `.editorconfig` (vedi [template.md](template.md#editorconfig)) applica queste regole in automatico in IntelliJ, Eclipse e VS Code.

## Motore di gioco (`sviluppo/engine/`)

- È un **microservizio** con due parti ([decisione 029](decisioni/029-motore-di-gioco.md)): `engine-core` con le regole ed `engine-service` che le espone via HTTP al solo backend.
- `engine-core` è **Java puro**: nessuna dipendenza da Spring, JPA, PostgreSQL o HTTP. `engine-service` non contiene logica di gioco.
- Riceve dati in ingresso (mazzi, carte, configurazione) e restituisce un risultato (vincitore, round e log della partita). Non legge né scrive sul database.
- **Deterministico e testabile:** il mescolamento usa un `Random` passato dall'esterno, così nei test si usa un seed fisso e il risultato è sempre lo stesso.
- I valori di gioco (punti vita, carte per mazzo, copie massime, bonus di fazione) stanno in **una sola classe di configurazione**, non sparsi nel codice. Sono ancora in discussione e cambieranno.
- Ogni regola del regolamento ha almeno un test JUnit.

```java
// ✅ Il Random arriva da fuori: nei test si passa new Random(42)
public BattleResult simulate(DeckSnapshot first, DeckSnapshot second, Random random) { ... }

// ❌ Random nascosto dentro: impossibile riprodurre una partita nei test
public BattleResult simulate(DeckSnapshot first, DeckSnapshot second) {
    Collections.shuffle(cards); // usa un Random interno
}
```

```java
// ✅ Valori in un unico punto
public record GameRules(
        int playerLifePoints,     // ipotesi attuale: 20
        int deckSize,             // ipotesi attuale: 20
        int maxCopiesPerCard,     // ipotesi attuale: 2
        int factionBonusAttack    // ipotesi attuale: +1
) { }

// ❌ Numeri magici sparsi
if (deck.size() != 20) { ... }
life -= damage + 1;
```

## Backend (`sviluppo/backend/`)

- Struttura a livelli: **Controller → Service → Repository**.
  - Il **controller** riceve la richiesta, valida l'input, chiama il service, restituisce la risposta. Niente logica di business.
  - Il **service** contiene la logica e le transazioni (`@Transactional`).
  - Il **repository** accede al database e basta.
- Le **entità JPA non escono mai dalle API**: si usano DTO (`DeckRequest`, `DeckResponse`).
- Dependency injection **tramite costruttore**, mai `@Autowired` sui campi.
- **Lombok** si usa solo con queste annotazioni:
  - `@RequiredArgsConstructor` per l'iniezione tramite costruttore (campi `private final`), al posto del costruttore scritto a mano;
  - `@Getter`, `@Setter` e `@NoArgsConstructor(access = AccessLevel.PROTECTED)` sulle entità JPA;
  - `@Slf4j` per i log.

  Mai `@Data`, `@EqualsAndHashCode` o `@ToString` sulle entità JPA: caricano le relazioni lazy e confrontano male le righe ancora senza id. I DTO sono `record` Java, senza Lombok.
- Validazione dell'input con `@Valid` e annotazioni (`@NotBlank`, `@Size`...).
- Gli errori si lanciano come eccezioni specifiche (`DeckNotFoundException`) e si gestiscono in un unico `@RestControllerAdvice`.
- Il **mescolamento dei mazzi e la simulazione avvengono solo lato server**. Il client non decide mai l'esito di una partita né il contenuto di un pacchetto.
- I crediti sono numeri **interi** (`int`/`long`), mai `double` o `float`.

```java
// ✅ Iniezione via costruttore, logica nel service
@RestController
@RequestMapping("/api/v1/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @PostMapping
    public ResponseEntity<DeckResponse> createDeck(@Valid @RequestBody DeckRequest request) {
        DeckResponse created = deckService.createDeck(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

// ❌ Campo @Autowired, logica e accesso al DB nel controller, entità restituita
@RestController
public class DeckController {
    @Autowired
    private DeckRepository repo;

    @PostMapping("/createDeck")
    public Deck create(@RequestBody Deck deck) {
        if (deck.getCards().size() != 20) { ... }
        return repo.save(deck);
    }
}
```

## Database

- **PostgreSQL 18** e **Flyway**. Le modifiche allo schema si fanno solo con migration versionate in `sviluppo/backend/src/main/resources/db/migration/`, mai a mano sul proprio PC. Una migration già eseguita non si modifica: se serve una correzione, si scrive una migration nuova.
- Ogni tabella ha una chiave primaria `id` di tipo `UUID` v7. In Java il campo è `java.util.UUID`, generato da Hibernate con `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)`. Il `DEFAULT uuidv7()` del database serve alle migration con i dati.
- Le relazioni hanno sempre la foreign key dichiarata. PostgreSQL non indicizza da solo le colonne delle foreign key: se si cercano righe per quella colonna, serve un indice.
- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate controlla le tabelle ma non le crea né le modifica.
- I dati finti per sviluppo e test stanno in `sviluppo/backend/src/main/resources/db/testdata/`, attivi solo nel profilo `dev` e nei test. Il catalogo vero sta nelle migration `V100` e successive.
- Schema completo e spiegazione delle scelte: [docs/database/schema.md](database/schema.md).

## Frontend

**Tecnologie scelte:** pagine **HTML, CSS e JavaScript**; i **componenti** (es. la carta) si scrivono in **React**. Decisione del 2/11: cambia quella del 26/10 "HTML, CSS e JavaScript, senza framework" solo per i componenti. Le pagine non diventano un'applicazione React. I dati arrivano dalle API del backend: vedi la [guida ai dati per il frontend](api/guida-frontend.md).

**Proposta di strumenti, da confermare da Gaia nella #72:** Vite, che gestisce più pagine HTML e i componenti React, in JavaScript (non TypeScript). Il container del frontend serve le pagine e manda `/api` al backend, così sito e API hanno lo stesso indirizzo (decisione 7 del 2/11).

> 🚧 **Da completare a cura del team WMD.**
>
> Da indicare:
> - Strumenti confermati (Vite o altri), come si caricano i componenti React nelle pagine, librerie e gestione degli stili (CSS o SCSS), con la motivazione
> - Struttura delle cartelle in `frontend/`
> - Come avviare il frontend in locale con Docker Compose
> - Regole di codice specifiche (quali parti diventano componenti, stili, gestione delle chiamate alle API)
> - Strumenti di formattazione e lint, se usati
>
> Riferimento: il brief del Laboratorio Integrato. La scelta va registrata anche in `docs/decisioni/`.

Regole che valgono comunque, qualunque tecnologia si scelga:

- Le statistiche delle carte (ATT, DIF, fazione, rarità) devono essere **leggibili anche su mobile**.
- Le chiamate alle API passano da un unico punto (es. un file `api.js`), non sparse nelle pagine e nei componenti: lì si aggiunge anche il token di accesso.
- Il frontend non decide mai l'esito di una partita né il contenuto di un pacchetto: mostra solo quello che restituisce il server.
- La carta è **un solo componente React** (#81), usato in tutte le pagine. Le cornici con la finestra trasparente e le coordinate di testi, ATT e DIF le prepara il frontend in JavaScript (decisione 21 del 2/11).
- Niente dati finti nel frontend: le pagine usano solo le API del backend (decisione 9 del 2/11).

## Sicurezza e file da non caricare

- **Mai** caricare password, chiavi o credenziali del database. Si usa `application-local.properties` (in `.gitignore`) o variabili d'ambiente.
- Le password degli utenti si salvano solo con hash (es. BCrypt).
- Non si caricano: cartelle `target/`, `build/`, `dist/`, `node_modules/`, `.idea/`, `.vscode/`, file `.DS_Store`.

## Test

- Il motore ha test JUnit per ogni regola; il backend almeno per i service.
- Un test controlla **un solo comportamento**.
- Struttura **Given / When / Then** (prepara, esegui, verifica).
- Prima di aprire una PR, tutti i test devono passare.
