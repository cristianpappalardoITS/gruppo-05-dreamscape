# 028 · Catalogo degli effetti, versione provvisoria

- **Data:** 2026-11-04
- **Stato:** approvata come **catalogo provvisorio**: gli effetti sono da bilanciare e si possono aggiungere o togliere in corso d'opera. Sostituisce "i 23 della lista di Nicolas" di [008](008-regole-della-partita.md)
- **Referenti:** Marco; Nicolas e Maie per il bilanciamento
- **Task:** #74, #86, #87, #103

## Contesto
La [lista di Nicolas](../../carte/effetti/Effetti%20Keyword%20Descrizioni.txt) aveva 23 effetti. Confrontata con le regole del round (proposta di regolamento, sezione 4) ha mostrato tre problemi:
- alcuni effetti erano troppo forti: con 20 punti vita quasi tutte le partite finiscono a mazzo esaurito, quindi gli effetti che danno carte in più (Ricorrenza, Ritorno) decidono la partita, e Speranza e Vendetta potevano dare +10 o più;
- alcuni potevano bloccare il motore in un ciclo infinito (Esilio, Rimescolanza, Rimpianto) o bloccare lo scontro (Paralisi, Protezione);
- altri erano scritti in modo ambiguo (a chi va il bonus, chi vince tra due effetti).

## Opzioni valutate
Per ogni effetto: tenerlo com'è, cambiarlo o toglierlo. Al posto di quelli tolti, nuovi effetti semplici che non toccano l'ordine né il numero di carte del mazzo.

## Decisione
**19 effetti**, al massimo uno per carta. È una versione di partenza: valori, effetti e fazioni consigliate si cambiano con le simulazioni del motore (#87) e il bilanciamento del catalogo (#103), aggiungendo o togliendo effetti quando serve.

| Effetto | Codice proposto | Quando | Cosa fa | Valore X | Fazione consigliata |
|---------|-----------------|--------|---------|----------|---------------------|
| Castigo X | `PUNISHMENT` | Quando viene giocata | X danni alla difesa della creatura avversaria, che però resta almeno a 1: Castigo non uccide | Sì | Nightmares |
| Flagello X | `SCOURGE` | Quando viene giocata | X danni ai punti vita dell'avversario | Sì | Nightmares |
| Riposo X | `REST` | Quando viene giocata | Il tuo giocatore guadagna X punti vita, anche oltre quelli iniziali | Sì | Dreams, Memories |
| Speranza X | `HOPE` | Quando viene giocata | +X difesa nel primo round dello scontro | Sì | Dreams |
| Vendetta X | `VENGEANCE` | Quando viene giocata | +X attacco nel primo round dello scontro, solo se nella tua pila degli scarti ci sono almeno 5 carte | Sì | Nightmares |
| Indebolimento X | `WEAKENING` | Mentre è in campo | La creatura avversaria ha X attacco in meno, solo nel primo round dello scontro | Sì | Nightmares, Dreams |
| Neutralità | `NEUTRALITY` | Mentre è in campo | Non subisce il vantaggio di fazione dell'avversario, ma tiene il proprio; due Neutralità si colpiscono senza vantaggi | No | Memories |
| Immunità | `IMMUNITY` | Mentre è in campo | Immune agli effetti dell'avversario (non a quelli delle tue carte) | No | Tutte |
| Non Sequitur | `NON_SEQUITUR` | Mentre è in campo | Annulla gli effetti della creatura avversaria. Non funziona contro Immunità; due Non Sequitur si annullano a vicenda | No | Memories |
| Culla | `CRADLE` | Mentre è in campo | Il tuo giocatore non subisce danni ai punti vita, nemmeno da Flagello, Miasma o Terrore puro | No | Dreams |
| Riflesso X *(nuovo)* | `REFLECTION` | Mentre è in campo | Quando subisce danni in combattimento, la creatura avversaria subisce X danni alla difesa. Il danno riflesso non si riflette di nuovo | Sì | Memories |
| Barriera *(nuovo)* | `BARRIER` | Mentre è in campo | Il primo colpo che subisce in combattimento è annullato (0 danni), una volta sola | No | Dreams |
| Assorbimento X *(nuovo)* | `ABSORPTION` | Mentre è in campo | Quando infligge danni ai punti vita dell'avversario, il tuo giocatore guadagna X punti vita | Sì | Nightmares |
| Permanenza X | `PERMANENCE` | Fine di ogni round | Recupera X difesa, ma mai oltre la sua difesa iniziale | Sì | Dreams, Memories |
| Intensificazione X | `INTENSIFICATION` | Fine di ogni round | +X attacco | Sì | Tutte |
| Miasma X | `MIASMA` | Fine di ogni round | X danni ai punti vita dell'avversario | Sì | Nightmares, Memories |
| Ninna nanna X *(nuovo)* | `LULLABY` | Fine di ogni round | Il tuo giocatore guadagna X punti vita | Sì | Dreams |
| Ultimo respiro X *(nuovo)* | `LAST_BREATH` | Quando viene uccisa | La creatura che l'ha uccisa subisce X danni alla difesa | Sì | Nightmares, Memories |
| Terrore puro | `PURE_TERROR` | Quando uccide | Danni ai punti vita dell'avversario pari alla difesa originale della creatura uccisa | No | Nightmares |

**Tolti dalla lista di Nicolas (9):** Ricorrenza, Rimescolanza, Paralisi, Esilio, Rimpianto, Ritorno, Eredità difensiva, Eredità offensiva, Protezione. "Non Sequitur" resta senza valore ([027](027-effetto-non-sequitur.md)).

Regole comuni:
- Gli effetti "fine di ogni round" scattano solo se la carta è ancora in campo.
- Quando due effetti scattano nello stesso momento, li applica per primo chi ha vinto il lancio della moneta.
- Nei testi si dice **round**, mai "turno"; "il tuo giocatore" è il proprietario della carta.

## Motivazione
Tolti gli effetti che decidono la partita da soli o che possono bloccare il motore, il catalogo è più facile da implementare e da bilanciare. Nessun effetto rimasto tocca il mazzo o la pila degli scarti, tranne Vendetta, che conta soltanto le carte negli scarti. Gli effetti nuovi coprono i ruoli rimasti scoperti: la difesa di Memories e la cura di Dreams.

## Conseguenze
- **Provvisorio:** ogni modifica al catalogo (valori, effetti nuovi o tolti) si registra con una decisione nuova o un aggiornamento di questa, e diventa una migration di dati. Un effetto non si cancella: si spegne (`effects.is_active = FALSE`), così non si assegna più alle carte, e le carte che lo hanno si cambiano con una migration di dati.
- Lo schema cambia solo per `effects.is_active`, che serve a spegnere un effetto. I cinque momenti (`ON_PLAY`, `WHILE_IN_PLAY`, `END_OF_ROUND`, `ON_DEATH`, `ON_KILL`) sono tutti usati, `effects.has_value` vale per gli effetti con la X. I codici sono una proposta per la migration dei dati.
- Il motore (#86) ha un test per ogni effetto; i valori di X si scelgono carta per carta nel catalogo (#74) e si verificano con le simulazioni (#87, #103).
- Carte di prova da riabbinare: Il Rifugio Sicuro (aveva Protezione; la più vicina è Barriera), Il Corridoio Infinito (aveva Ritorno), La Caduta Senza Fine (ancora da scegliere). I Lord (La Custode aveva Eredità difensiva) si decidono insieme agli effetti dei Lord, ancora da discutere.
- Da dire a Nicolas e Maie; da aggiornare la proposta di regolamento (sezione 6).
