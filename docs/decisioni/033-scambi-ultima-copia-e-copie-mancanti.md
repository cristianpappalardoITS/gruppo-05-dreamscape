# 033 · Scambi: carte richieste, copie mancanti e ultima copia

- **Data:** 2026-11-17
- **Stato:** approvata, completa [024](024-copie-bloccate-negli-scambi.md) e cambia la regola delle copie mancanti all'accettazione di [schema.md](../database/schema.md#scambi)
- **Referenti:** Marco
- **Task:** #93, #96

## Contesto
Scrivendo gli scambi (#93) sono rimasti tre casi senza regola, o con una regola da rivedere:
- alla proposta non si sapeva se controllare già le carte chieste all'amico, che non si bloccano ([024](024-copie-bloccate-negli-scambi.md));
- all'accettazione, se il destinatario non ha più le copie chieste (le ha date in un altro scambio), schema.md annullava lo scambio;
- un giocatore può cedere senza accorgersene l'ultima copia di una carta.

## Opzioni valutate
1. Carte richieste: controllarle già alla proposta, oppure solo all'accettazione.
2. Copie mancanti all'accettazione: lo scambio si annulla, oppure resta in attesa.
3. Ultima copia: nessun avviso, avviso solo a chi propone, oppure a tutti e due.
4. Blocco delle copie: solo quelle offerte (come la 024), oppure anche quelle chieste.

## Decisione
- **Alla proposta** si controlla che l'amico abbia oggi abbastanza copie **disponibili** delle carte richieste (possedute meno quelle che lui ha offerto in altri scambi in attesa). Le sue copie però **non si bloccano**: restano libere finché non accetta, come nella 024.
- **Si bloccano solo le copie offerte** da chi propone, come nella 024.
- **All'accettazione**, se il destinatario non ha più abbastanza copie disponibili, nessuna carta si muove, risponde `409 REQUESTED_CARDS_NOT_AVAILABLE` con il motivo e **lo scambio resta in attesa**: lo può rifiutare, oppure liberare le copie e riprovare.
- **Se non sono più amici** lo scambio si annulla (`CANCELLED`), come nella [026](026-carte-disattivate-e-amicizie-tolte.md).
- **Avviso "ultima copia"**: prima di proporre (`POST /trades/preview`) e prima di accettare (`GET /trades/{tradeId}/deck-warnings`) si avvisa se una carta che esce è l'ultima copia della collezione. La full art si conta da sola. Lo scambio si può fare lo stesso.

## Motivazione
Controllare subito le carte richieste evita proposte che non si potranno mai accettare, senza permettere a un amico di bloccare le carte di un altro. Uno scambio che resta in attesa lascia la scelta al destinatario: annullarlo da solo gli farebbe perdere una proposta che magari vuole accettare dopo aver liberato le copie. L'avviso sull'ultima copia protegge da scambi fatti di fretta, senza vietarli.

## Conseguenze
- [schema.md](../database/schema.md#scambi) (sezione Scambi) e la [guida per il frontend](../api/guida-frontend.md#scambi) hanno le nuove regole, `REQUESTED_CARDS_NOT_AVAILABLE`, `TRADE_CANCELLED` e `lastCopyWarnings` accanto a `deckWarnings`.
- Nessuna migration.
- Da dire a Daniele (#96, pagine Amici e Scambi): mostrare `availableForTrade`, gli avvisi su mazzi e ultima copia, e che uno scambio può restare in attesa dopo un `409`.
