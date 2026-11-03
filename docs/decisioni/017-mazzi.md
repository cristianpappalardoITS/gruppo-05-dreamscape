# 017 · Mazzi

- **Data:** 2026-10-26 (solo mazzi validi: 2026-11-02; mazzo classificato: 2026-11-03)
- **Stato:** approvata
- **Referenti:** Nicolas, SWD, WMD
- **Task:** domanda 5 del 2/11; #80, #89

## Contesto
Il SiteMap salvava un mazzo non valido come bozza; lo schema prevedeva solo mazzi validi.

## Opzioni valutate
1. Salvare solo i mazzi validi, oppure anche le bozze.

## Decisione
- **20 carte**, al massimo 2 copie per carta (1 per le Leggendarie). Limiti per rarità: Leggendarie 2, Epiche 4, Rare 8, Comuni libere.
- Si salvano **solo i mazzi validi**. Un mazzo che diventa non valido dopo uno scambio viene segnalato e ricontrollato prima di ogni partita.
- Ogni giocatore sceglie un **mazzo classificato**, usato quando il matchmaking lo sceglie come avversario ([010](010-tipi-di-partita-e-rank.md)).
- La full art nel mazzo è solo una scelta di aspetto.

## Motivazione
Meno stati da gestire: niente colonna `is_valid` né badge Valido / Non valido.

## Conseguenze
- `users.ranked_deck_id`; se il mazzo classificato non è valido, il giocatore non viene scelto dal matchmaking.
