# 031 · Effetti nel motore: i casi lasciati aperti

- **Data:** 2026-11-16
- **Stato:** approvata come **proposta**, da confermare con Nicolas e Maie come le regole a–f di [029](029-motore-di-gioco.md); completa [028](028-catalogo-degli-effetti.md)
- **Referenti:** Cristian; Nicolas e Maie per la conferma
- **Task:** #86, #87, #97, #103

## Contesto
Scrivendo i 19 effetti nel motore (#86) sono usciti alcuni casi che il catalogo ([028](028-catalogo-degli-effetti.md)) e le regole del round ([029](029-motore-di-gioco.md)) non decidono. Il motore deve fare una scelta precisa per ognuno, altrimenti la stessa partita si potrebbe leggere in due modi.

## Opzioni valutate
1. **Culla** quando la creatura muore in combattimento: il suo giocatore subisce il danno di quel combattimento, oppure Culla lo ferma ancora.
2. **Non Sequitur** entrato in campo dopo: i bonus che l'avversaria ha già accumulato (es. l'attacco di Intensificazione) restano, oppure si sospendono finché Non Sequitur è in campo.
3. **Immunità**: protegge solo la creatura, oppure anche i punti vita del suo giocatore.

## Decisione
- **Culla protegge fino alla fine del round in cui muore.** Ferma il danno del combattimento del round in cui viene uccisa, il Terrore puro di chi la uccide e il Miasma di quel round. Dal round dopo il giocatore non è più protetto.
- **Non Sequitur annulla solo da quando è in campo.** Gli effetti dell'avversaria non scattano più, ma quello che è già successo resta: l'attacco guadagnato con Intensificazione, la difesa recuperata, i punti vita. Quando la creatura con Non Sequitur muore, gli effetti dell'avversaria tornano subito: Terrore puro scatta anche se uccide un Non Sequitur.
- **Immunità protegge solo la creatura.** Ferma Castigo, Indebolimento, Riflesso, Ultimo respiro e Non Sequitur. Flagello, Miasma e Terrore puro colpiscono lo stesso i punti vita: per quelli c'è Culla.

Scelte più piccole, prese per scrivere il motore:
- Gli effetti "quando viene giocata" scattano dopo che tutte e due le creature sono in campo, nell'ordine della moneta: un Non Sequitur giocato nello stesso round annulla già l'effetto dell'avversaria.
- **Indebolimento** agisce nel primo round di ogni scontro, anche quando la creatura che lo ha è in campo da prima e l'avversaria è appena entrata.
- **Barriera** si consuma solo con un colpo che farebbe danno: un colpo da 0 non la toglie. Non ferma i danni degli effetti (Castigo, Riflesso, Ultimo respiro).
- **Ogni creatura morta è stata uccisa dall'avversaria**, anche se è morta per Riflesso: scattano Ultimo respiro e Terrore puro.
- **Ultimo respiro** non fa niente se anche chi ha ucciso è già morta.
- **Assorbimento** scatta solo con il danno del combattimento ai punti vita, non con i danni degli effetti.
- **Permanenza** recupera fino alla difesa scritta sulla carta, senza contare la difesa di Speranza.
- **I punti vita si controllano solo a fine round** (029, punto c.6): un giocatore arrivato a 0 durante il round e poi curato da Riposo, Ninna nanna o Assorbimento resta in partita.
- Un effetto con valore X uguale a 0 non fa niente. Un effetto che il motore non conosce, o senza il suo valore X, **ferma la partita con un errore**: giocare la carta senza effetto darebbe un risultato sbagliato senza che nessuno se ne accorga.
- Nel log un effetto si scrive solo se cambia un valore (`EFFECT`) o se viene fermato (`EFFECT_BLOCKED`).

## Motivazione
- Culla: il log della 029 prevede già `PLAYER_DAMAGE` fermato da Culla, e la 028 dice che Culla ferma Terrore puro, che scatta solo quando la creatura con Culla viene uccisa. Senza questa regola Culla non potrebbe mai fermare nessuno dei due.
- Non Sequitur: "da quel momento in poi" è più semplice da spiegare ai giocatori e da mostrare nel replay, e non richiede di ricordare bonus sospesi.
- Immunità: se proteggesse anche i punti vita, Culla sarebbe quasi inutile.

## Conseguenze
- `ENGINE_VERSION` passa a `0.2.0`: le partite con carte con effetti hanno un esito diverso da prima.
- Il motore ha un test per ognuno dei 19 effetti e per i casi di questa decisione (`BattleEngineEffectsTest`), più una prova con 500 partite tra mazzi casuali.
- Le simulazioni (#87) e il bilanciamento (#103) usano queste regole; se Nicolas e Maie ne cambiano una, si aggiorna questa decisione e cambia la versione del motore.
- Il replay (#97) riceve `EFFECT`, `EFFECT_BLOCKED` e `BONUS_EXPIRED` nel formato già scritto in [schema.md](../database/schema.md#formato-dei-campi-json-di-matches): nessun campo nuovo.
- Da dire a Nicolas e Maie; da aggiornare la proposta di regolamento (sezione 6).
