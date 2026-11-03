# 013 · Dream Credits e ricompense

- **Data:** 2026-10-31 (confermate dal cliente: 2026-11-02; vittorie solo in classificata: 2026-11-03)
- **Stato:** approvata
- **Referenti:** Sara, Maie
- **Task:** #61, domande 11, 13 e 17 del 2/11; #78

## Contesto
Il brief chiede crediti virtuali per comprare pacchetti. Il documento di Sara del 31/10 ha proposto moneta e ricompense, mostrate al cliente nella slide 12.

## Opzioni valutate
1. Ricompense di Sara, oppure valori più alti delle prime bozze (bonus di 1.000, vittoria +150).
2. "Giorno" delle ricompense da mezzanotte italiana oppure in UTC.

## Decisione
- Moneta: **Dream Credits (DC)**, numeri interi. Niente denaro reale in fase 1.
- Ricompense: registrazione **500** · accesso giornaliero **50** · missione giornaliera **100** · vittoria in partita classificata **30** (massimo 3 al giorno, non contro un amico) · obiettivo settimanale **250** · premio di lega ([011](011-premi-di-lega.md)).
- Il **giorno inizia a mezzanotte, ora italiana**; la settimana il lunedì. Nel database le date restano in UTC.
- 1.000 DC = 1 € solo per le proiezioni economiche, mai nei testi per i giocatori.

## Motivazione
Il cliente ha confermato le ricompense della slide 12. Un giorno uguale per tutti i giocatori italiani è più facile da spiegare.

## Conseguenze
- Ogni movimento in `credit_transactions`, con il tipo. Ricompensa giornaliera con `users.last_daily_reward_on` e un `UPDATE` atomico.
