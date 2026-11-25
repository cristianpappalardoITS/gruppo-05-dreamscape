# 015 · Missione giornaliera e obiettivo settimanale

- **Data:** 2026-11-02 (struttura e settimana: 2026-11-03)
- **Stato:** approvata; partite che contano: applicata la proposta il 25/11 (#125), da confermare con Maie e Nicolas
- **Referenti:** tutti; Maie e Nicolas per il contenuto
- **Task:** domanda 12 del 2/11; #125 (SWD) e #126 (WMD)

## Contesto
Al cliente abbiamo mostrato 100 DC per la missione giornaliera e 250 per l'obiettivo settimanale, ma nessuna issue li prevedeva.

## Opzioni valutate
1. Versione semplice nello Sprint 6, missioni configurabili dal backoffice, oppure rimandarle al Backlog.
2. Salvarle in colonne di `users` oppure in tabelle `missions` e `user_missions`.
3. Settimana dal lunedì oppure 7 giorni dalla registrazione.

## Decisione
- **Versione semplice, nello Sprint 6**: una missione giornaliera fissa ("Gioca una partita", 100 DC) e un obiettivo settimanale fisso ("Vinci 5 partite", 250 DC).
- Tabelle **`missions`** e **`user_missions`**, così una missione nuova è una riga in più. Il premio si prende una volta per periodo.
- La settimana inizia il **lunedì a mezzanotte, ora italiana**.

## Motivazione
Le ricompense promesse al cliente ci sono, con poco lavoro. Le tabelle lasciano spazio a missioni nuove senza cambiare lo schema.

## Conseguenze
- Migration `V9__create_missions.sql`; tipo `MISSION_REWARD`.
- Da decidere con Maie e Nicolas: se le partite rapide contano (proposta: la giornaliera conta ogni partita, la settimanale solo le vittorie classificate).

## Aggiornamento del 25/11 (#125)
- **Partite che contano:** applicata la proposta. La missione giornaliera conta ogni partita (rapida, classificata, sfida tra amici); l'obiettivo settimanale solo le vittorie classificate (`missions.match_type = 'RANKED'`, migration `V105__set_weekly_mission_ranked.sql`). Così i 250 DC non si prendono con sfide tra amici vinte d'accordo. Se Maie e Nicolas decidono diversamente basta una migration che cambia `match_type`.
- **Chi avanza:** solo chi ha scelto di giocare. Nel matchmaking chi avvia la partita, non l'avversario scelto, che può non essere collegato (come i 30 DC della vittoria, [decisione 010](010-tipi-di-partita-e-rank.md)); in una sfida tra amici tutti e due.
- **Premio:** arriva subito, nella stessa transazione della partita, con il movimento `MISSION_REWARD` collegato alla partita (`match_id`). Non entra in `creditsEarned` del risultato della partita.
- Le missioni si leggono in `GET /me/summary` (`missions`), come nella guida per il frontend.
