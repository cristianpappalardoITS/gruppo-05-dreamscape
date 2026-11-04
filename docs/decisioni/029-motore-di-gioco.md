# 029 · Motore di gioco: ingresso, seed, log a eventi

- **Data:** 2026-11-04
- **Stato:** approvata; le regole del round (punti a–f) sono una **proposta** da confermare con Nicolas e Maie e da verificare con le simulazioni
- **Referenti:** Marco; Nicolas e Maie per le regole del round
- **Task:** #44, #70, #79, #86, #87, #97

## Contesto
Prima di scrivere il motore (#70, #79, #86) serviva fissare cosa riceve, cosa restituisce e com'è fatto il log che Web usa per il replay (#97). Il formato di `round_log` in [schema.md](../database/schema.md#formato-dei-campi-json-di-matches) aveva un solo oggetto per round: non diceva quando una carta entra in campo, in che ordine scattano gli effetti, né i danni ai giocatori causati dagli effetti. La [proposta di regolamento](../proposte/proposta-regolamento-dreamscape.pdf) lasciava aperti alcuni casi (sezione 11) che il catalogo della [028](028-catalogo-degli-effetti.md) rende più importanti.

## Opzioni valutate
1. Dove sta il motore: sottoprogetto Gradle `sviluppo/engine`, oppure package `it.somnia.dreamscape.engine` dentro il backend.
2. Formato del log: lista di eventi in ordine dentro ogni round, oppure un oggetto per round come in schema.md.
3. Testi della cronaca ("Indebolimento 2: l'attacco scende da 4 a 2"): li compone il frontend dai dati, oppure li scrive il motore in italiano.
4. Regole del round lasciate aperte dal regolamento: decise ora una per una, oppure scritte come proposta da confermare.

## Decisione

### Dove sta
- **Sottoprogetto Gradle `sviluppo/engine`**, package `it.somnia.dreamscape.engine`, usato dal backend come dipendenza. Spring non è nel suo classpath, quindi la regola "Java puro" la controlla la build. Come collegare i due progetti (build composita o un `settings.gradle` in `sviluppo/`) si sceglie nella #70.

### Cosa riceve
- Le **copie dei due mazzi** già pronte, le stesse che il backend salva in `matches.*_deck_snapshot`: per ogni carta id, nome, ATT, DIF, fazione, rarità, quantità ed effetto (codice e valore X).
- Le **regole** (`GameRules`): punti vita, limite di round e vantaggi di fazione, letti dal backend da `game_modes` e `faction_advantages`. Nessun numero di gioco nel codice del motore.
- Il **seed** (`long`), scelto dal backend e salvato in `matches.random_seed`.
- Il motore non legge il database e non controlla la validità dei mazzi (la controlla il backend prima della partita). Rifiuta solo dati rotti, per esempio un mazzo vuoto.
- Non conosce gli utenti: parla di lato `CHALLENGER` e lato `OPPONENT`. Il backend traduce i lati negli utenti.

### Mescolamento e moneta
- Dal seed il motore crea **un solo `java.util.Random`** e lo usa sempre in quest'ordine: mescola il mazzo di `CHALLENGER`, poi quello di `OPPONENT`, poi tira la moneta. Nessun altro valore casuale.
- Con lo stesso seed, gli stessi mazzi, le stesse regole e la stessa versione del motore, la partita è sempre identica.

### Cosa restituisce
- Il **risultato**: vincitore (`CHALLENGER`, `OPPONENT` o nessuno se pareggio), motivo della fine (`LIFE_POINTS`, `DECK_EXHAUSTED`, `ROUND_LIMIT`), round totali, punti vita finali, chi ha vinto la moneta.
- Il **log**, che il backend salva così com'è in `matches.round_log`: per ogni round una **lista di eventi in ordine** (carta giocata, effetto, effetto bloccato, colpo, morte, danno al giocatore, fine di un bonus, fine della partita) e lo stato dei due lati a fine round. Formato in [schema.md](../database/schema.md#formato-dei-campi-json-di-matches).
- Crediti, rank, missioni e leghe non sono del motore: li calcola il backend dopo la partita.
- Per le simulazioni (#87) il log si può spegnere: il motore restituisce solo il risultato.

### Testi della cronaca
- Il log contiene **solo dati**: codici, valori, id delle carte, valori prima e dopo. Le frasi le compone il frontend, con i nomi delle carte e degli effetti presi dalle copie dei mazzi.

### Regole del round (proposta da confermare)
- **a. Danno al giocatore:** attacco totale della vincitrice (attacco + bonus di fazione + modifiche degli effetti) meno la difesa della sconfitta **all'inizio del combattimento**, cioè dopo gli effetti "quando viene giocata" come Castigo. Mai sotto 0.
- **b. Attacco:** non scende mai sotto 0. Uno scontro bloccato lo ferma il limite di round.
- **c. Ordine della fase finale:**
  1. muoiono le creature con difesa a 0 o meno;
  2. si applica il danno del combattimento al giocatore;
  3. scattano "quando uccide" e "quando viene uccisa", nell'ordine della moneta;
  4. scattano gli effetti "fine di ogni round", solo per le creature ancora in campo;
  5. finiscono i bonus che valgono un solo round;
  6. si controlla la fine della partita: punti vita, mazzo esaurito, limite di round.

  Con quest'ordine Ultimo respiro può uccidere la creatura che ha ucciso, ma il danno al giocatore già inflitto resta.
- **d. Speranza:** la difesa in più si consuma per prima e quello che ne resta sparisce a fine round.
- **e. Terrore puro:** scatta anche quando muoiono tutte e due le creature.
- **f. Riflesso:** il danno riflesso arriva nello stesso combattimento, dopo i due colpi, e può uccidere.
- Restano come nel regolamento: i due colpi sono contemporanei; con una sola creatura morta c'è il danno al giocatore, con due no; i punti vita non scendono sotto 0 e possono superare quelli iniziali; il mazzo esaurito si controlla a fine round, per chi ha la creatura morta e nessuna carta da giocare.

## Motivazione
Un modulo a parte rende il motore riusabile dalle simulazioni e impedisce di legarlo a Spring per sbaglio. Con un solo `Random` usato in un ordine fisso, seed e log bastano per rigiocare e verificare una partita. Gli eventi in ordine permettono al replay di mostrare ogni passaggio, anche quelli che un oggetto per round nasconde (Castigo prima del colpo, Barriera, Culla). Senza testi il motore non dipende dalla lingua e il log pesa meno. Le regole a–f servono per scrivere il motore subito: i valori e i casi si correggono con le simulazioni.

## Conseguenze
- [schema.md](../database/schema.md#formato-dei-campi-json-di-matches) e la [guida per il frontend](../api/guida-frontend.md) hanno il nuovo formato di `round_log` e del risultato della partita. **Nessuna migration:** `round_log` resta JSONB, cambia solo il contenuto.
- Da dire a Gaia e a Yuri (#97): il replay legge gli eventi e compone le frasi.
- Da dire a Cristian: #44 (progettazione), #70 (modulo in `sviluppo/engine`) e #86 (che dice ancora "23 effetti": ora sono i 19 della 028).
- Da confermare con Nicolas e Maie i punti a–f; da aggiornare la proposta di regolamento (sezione 4).
- `engineVersion` nel log cambia a ogni modifica delle regole del motore: una partita si rigioca uguale solo con la stessa versione.
