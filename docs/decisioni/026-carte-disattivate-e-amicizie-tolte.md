# 026 · Carte disattivate e amicizie tolte

- **Data:** 2026-11-04
- **Stato:** approvata, completa [009](009-amici-e-scambi.md) e [017](017-mazzi.md)
- **Referenti:** Marco
- **Task:** #80, #89, #93, #95, #96, #115

## Contesto
Il controllo dello schema prima delle migration (#69) ha trovato due casi senza regola:
- l'admin può disattivare una carta (`cards.is_active = FALSE`) che qualcuno ha già nei mazzi;
- se si toglie un'amicizia, gli scambi e le sfide in attesa tra i due restano aperti, e le copie offerte restano bloccate ([024](024-copie-bloccate-negli-scambi.md)) finché qualcuno non annulla.

## Opzioni valutate
1. Carte disattivate: si possono ancora usare nei mazzi, oppure no.
2. Amicizia tolta: scambi e sfide in attesa restano aperti finché qualcuno li annulla, oppure si annullano da soli.

## Decisione
- Una carta **disattivata non si può usare nei mazzi**. I mazzi che la contengono diventano non validi e il giocatore **viene avvisato** nella lista dei mazzi, con il motivo. Chi la possiede la tiene (e la può scambiare); non esce più dai pacchetti. Se è disattivata la carta base, valgono come disattivate anche le sue versioni ([025](025-versioni-delle-carte.md)).
- Quando si **toglie un'amicizia**, gli scambi e le sfide in attesa tra i due **si annullano da soli** (`CANCELLED`) e le copie offerte tornano disponibili. Lo stesso succede quando un account viene cancellato.

## Motivazione
Una carta tolta dal gioco non deve finire in partita. Annullare subito gli scambi evita copie bloccate per scambi che non si possono più accettare.

## Conseguenze
- Il controllo dei mazzi (#80) verifica anche che le carte siano attive; la lista dei mazzi (#89) mostra l'avviso.
- Il backoffice (#95, #101), prima di disattivare una carta, dice quanti mazzi diventeranno non validi.
- `DELETE /friends/{userId}` (#93) e la cancellazione dell'account (#115) annullano scambi e sfide in attesa nella stessa transazione.
- Il matchmaking non sceglie chi ha il mazzo classificato con una carta disattivata, finché non lo sistema.
