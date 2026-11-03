# 008 · Regole della partita

- **Data:** 2026-10-26 (vantaggio di fazione: 2026-10-31; punti vita confermati dal cliente: 2026-11-02)
- **Stato:** approvata; i punti vita restano da verificare con le simulazioni
- **Referenti:** Nicolas, Maie, Marco
- **Task:** domanda 11 del 2/11; #79, #86, #87

## Contesto
Il brief chiedeva una modalità Base obbligatoria e una seconda modalità con gli effetti. Il brief aggiornato permette di modificare la Base. Il documento di Sara (31/10) proponeva un giro dei vantaggi di fazione opposto a quello del regolamento.

## Opzioni valutate
1. Due modalità, Base senza effetti ed Effetti: pro: segue il primo brief / contro: due regolamenti da sviluppare e spiegare.
2. Una sola modalità Base con gli effetti: pro: un solo regolamento / contro: la Base diventa più ricca.
3. Vantaggi di fazione: giro del regolamento o giro inverso del documento di Sara.
4. Punti vita: 20 (partite più tattiche) o 10 (più rapide).

## Decisione
- **Una sola modalità, la Base, con gli effetti delle carte.** Tutte le partite, classificate e rapide, usano queste regole ([010](010-tipi-di-partita-e-rank.md)).
- Partita simulata dal server, senza interazione dei giocatori. Si dice **round**, non turno.
- **20 punti vita** (scelta del cliente, da verificare con le simulazioni del motore, #87), **massimo 60 round**, poi pareggio. Fine anche a mazzo esaurito.
- **Vantaggio di fazione:** Dreams batte Memories, Memories batte Nightmares, Nightmares batte Dreams. Chi è in vantaggio ha **+1 ATT**.
- Effetti: i 23 della lista di Nicolas, al massimo uno per carta.

## Motivazione
Una sola modalità riduce il lavoro su motore, pagine e regolamento. Il giro dei vantaggi del regolamento era già nelle carte e nella presentazione.

## Conseguenze
- Valori in `game_modes` (punti vita, round) e `faction_advantages`, copiati in `matches`.
- Dettagli nella [proposta di regolamento](../proposte/proposta-regolamento-dreamscape.pdf).
