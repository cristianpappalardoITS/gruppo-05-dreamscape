# 027 · Effetto "Non Sequitur" senza valore

- **Data:** 2026-11-04
- **Stato:** approvata
- **Referenti:** Marco
- **Task:** #74, #86

## Contesto
Nella lista degli effetti di Nicolas (`carte/effetti/`) c'è "Non Sequitur X": *"Annulla gli effetti della creatura avversaria"*, finché la carta resta in campo; quando viene uccisa, gli effetti dell'avversaria tornano. Il nome ha la X, ma il testo non usa nessun numero.

## Opzioni valutate
1. Senza valore: annulla l'effetto della creatura avversaria finché resta in campo.
2. Con valore: annulla l'effetto avversario per X round.

## Decisione
**Senza valore**: l'effetto si chiama "Non Sequitur" (senza X) e ha `effects.has_value = FALSE`. Annulla gli effetti della creatura avversaria finché la carta resta in campo (`trigger_event = 'WHILE_IN_PLAY'`).

## Motivazione
È quello che dice il testo dell'effetto; la X era in più.

## Conseguenze
- Nel catalogo degli effetti (migration di dati) "Non Sequitur" non ha valore.
- Il motore (#86) lo implementa come effetto attivo finché la carta è in campo.
- Da dire a Nicolas, che ha scritto la lista.
- *Il 4/11 il catalogo completo è stato rivisto nella [028](028-catalogo-degli-effetti.md): Non Sequitur resta senza valore, non funziona contro Immunità e due Non Sequitur si annullano a vicenda.*
