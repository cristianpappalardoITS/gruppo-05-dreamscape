# 009 · Amici e scambi

- **Data:** 2026-10-26 (scambi: 2026-11-02; limiti e scadenze: 2026-11-03)
- **Stato:** approvata. La parte "sfide aperte a tutti" del 26/10 è sostituita da [010](010-tipi-di-partita-e-rank.md); quella sulle carte non bloccate è sostituita da [024](024-copie-bloccate-negli-scambi.md)
- **Referenti:** SWD, DGS
- **Task:** domanda 6 del 2/11; #93

## Contesto
Il brief chiede scambi in quantità diverse, solo se il destinatario accetta. Il SiteMap bloccava le carte offerte finché lo scambio non si chiudeva.

## Opzioni valutate
1. Scambi con chiunque, oppure solo tra amici.
2. Carte offerte bloccate finché lo scambio non si chiude, oppure non bloccate e ricontrollate all'accettazione.
3. Scambi e sfide che scadono dopo un certo tempo, oppure senza scadenza.
4. Un numero massimo di amici e di richieste, oppure nessun limite.

## Decisione
- **Lista amici**: A manda la richiesta, B la accetta. Nessun limite di amici né di richieste.
- **Scambi solo tra amici**, quantità e rarità libere (es. 1 Epica per 5 Comuni), sempre solo se l'altro accetta. Niente crediti negli scambi.
- Le carte offerte **non si bloccano**: all'accettazione si ricontrolla tutto e, se mancano carte o l'amicizia è stata tolta, lo scambio si annulla da solo con un messaggio. *Sostituito il 4/11 da [024](024-copie-bloccate-negli-scambi.md).*
- Scambi e sfide in attesa **non scadono**: chi propone può annullare. *Se si toglie l'amicizia si annullano da soli: completato il 4/11 da [026](026-carte-disattivate-e-amicizie-tolte.md).*
- Al posto delle notifiche, un **contatore** delle richieste in attesa nel menu.

## Motivazione
Gli scambi tra amici evitano abusi tra sconosciuti. Senza blocchi, scadenze e notifiche ci sono meno stati da gestire e meno codice.

## Conseguenze
- Tabelle `friendships`, `trades`, `trade_items`; nessuno stato `EXPIRED`, nessuna tabella `notifications`.
