# 011 · Leghe e premi di lega

- **Data:** 2026-11-03
- **Stato:** approvata; soglie e importi da proporre
- **Referenti:** Marco; Maie per gli importi
- **Task:** issue del rank, da aprire

## Contesto
Le leghe (Dormiente, Sognatore, Lucido, Dreamweaver) si calcolavano dal punteggio e non davano niente. Un premio per chi sale incoraggia a giocare le classificate.

## Opzioni valutate
1. Leghe e premi in tabella (`leagues`, `user_league_rewards`), oppure la lega più alta in una colonna di `users` e i premi nella configurazione.
2. Premio solo la prima volta, ogni volta che si sale, oppure una volta per stagione.

## Decisione
- Tabella **`leagues`** con nome, punteggio minimo e premio in DC, modificabili dal backoffice. Tabella **`user_league_rewards`** con le leghe raggiunte da ogni giocatore.
- Il premio arriva **solo la prima volta** che si entra in una lega (tipo `LEAGUE_REWARD`).
- Si fa insieme al rank. Soglie e importi li propone **Maie** (proposta di partenza: Dormiente 0, Sognatore 1100, Lucido 1300, Dreamweaver 1500).

## Motivazione
Chi scende e risale non riprende il premio: nessuno perde apposta per accumulare crediti. Senza stagioni non serve azzerare i punteggi.

## Conseguenze
- Gli importi cambiano l'economia del gioco e il budget di Digital: vanno decisi con Maie.
- Migration `V10__create_leagues.sql`.
