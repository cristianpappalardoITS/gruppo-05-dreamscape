# 024 · Copie bloccate negli scambi e avviso sui mazzi

- **Data:** 2026-11-04
- **Stato:** approvata, sostituisce la parte "carte non bloccate" di [009](009-amici-e-scambi.md)
- **Referenti:** Marco
- **Task:** #93, #96

## Contesto
Il 2/11 si era deciso di non bloccare le carte offerte in uno scambio in attesa e di ricontrollare tutto all'accettazione. Così però un giocatore può offrire la stessa copia in più scambi insieme, e solo il primo accettato va a buon fine. In più, se la carta scambiata sta in un mazzo, il mazzo diventa non valido senza che il giocatore se ne accorga. Le bozze dei mazzi non ci sono (decisione 5 del 2/11).

## Opzioni valutate
1. Bloccare le copie offerte solo negli altri scambi, e avvisare se un mazzo diventerà non valido.
2. Bloccarle anche nei mazzi: i mazzi che le contengono diventano non validi finché lo scambio è in attesa.
3. Vietare di offrire le copie che stanno in un mazzo.
4. Aggiungere le bozze dei mazzi.

## Decisione
- Le copie offerte in uno scambio **in attesa** sono **bloccate per gli altri scambi** di chi le offre. Restano usabili nei mazzi. Il blocco finisce quando lo scambio è accettato, rifiutato o annullato.
- Le carte **richieste** all'amico non si bloccano: lui non ha ancora accettato.
- Alla proposta e all'accettazione si **avvisa** se una carta che esce sta in un mazzo e quale mazzo diventerà non valido (anche se è il mazzo classificato). Lo scambio si può fare lo stesso.
- **Niente bozze:** "solo mazzi validi" vale al salvataggio. Un mazzo che diventa non valido dopo uno scambio resta salvato con `isValid: false` e il motivo, non si gioca e, se è il mazzo classificato, il matchmaking non sceglie il giocatore finché non lo sistema.

## Motivazione
Nessuno può promettere la stessa copia due volte. Il giocatore sa prima cosa succede ai suoi mazzi, senza dover gestire bozze. Bloccare le carte anche nei mazzi avrebbe reso inutilizzabili mazzi interi per uno scambio che magari viene rifiutato.

## Conseguenze
- Nessuna colonna nuova: le copie bloccate si contano dagli scambi `PENDING` ([schema.md](../database/schema.md#scambi)), con `SELECT ... FOR UPDATE` alla proposta.
- API: `409 CARDS_LOCKED_IN_TRADE`, `availableForTrade` nella collezione per gli scambi, avvisi sui mazzi prima di proporre e di accettare ([guida ai dati](../api/guida-frontend.md#scambi)).
- Da aggiornare: #93 (backend degli scambi) e #96 (pagine Amici e Scambi).
