# 025 · Versioni delle carte: la full art è una carta con il suo id

- **Data:** 2026-11-04
- **Stato:** approvata, sostituisce "la full art nel mazzo è solo una scelta di aspetto" di [017](017-mazzi.md)
- **Referenti:** Marco
- **Task:** #69, #77, #78, #80, #83, #89, #93, #96

## Contesto
Fino a oggi la full art era una copia rara della stessa carta: un "sì/no" in collezione (`full_art_quantity`), nei mazzi, negli scambi e nelle aperture (`is_full_art`), più tre colonne `full_art_*` in `cards`. Le skin per le carte sono nel Backlog ([021](021-skin-e-animazioni.md)) e con quel modello avrebbero richiesto altre colonne in tutte le tabelle. Restava anche aperto cosa succede a un mazzo che usa una full art quando il giocatore la scambia.

## Opzioni valutate
1. Full art come riga di `cards` con il suo id, collegata alla carta normale, **senza statistiche proprie**: nome, ATT, DIF, fazione, rarità ed effetto si leggono dalla carta base.
2. Come la 1, ma con le statistiche copiate nella full art: letture più semplici, ma ogni modifica va ripetuta su tutte le versioni.
3. Tabella `card_variants` a parte: collezione, mazzi e scambi puntano a carta e versione, con più tabelle e collegamenti.

## Decisione
- La full art è una **carta a parte, con il suo id** e il suo codice (`BASE-004-FA`): `cards.base_card_id` punta alla carta normale e `variant_type = 'FULL_ART'`. Opzione 1.
- Non ha statistiche proprie: nome, descrizione, ATT, DIF, fazione, rarità ed effetto sono quelli della carta base. Una modifica dal backoffice vale per tutte le versioni.
- Collezione, mazzi, scambi e aperture usano solo `card_id`: la full art si possiede, si mette nel mazzo e si scambia come ogni altra carta.
- Nel mazzo **normale e full art della stessa carta contano insieme** per le copie massime e per i limiti di rarità (es. una Leggendaria: 1 copia, normale o full art).
- **Full art scambiata:** se il giocatore cede una full art che usa in un mazzo, il mazzo passa alla versione normale solo se il giocatore ne possiede una copia libera. Se non ce l'ha, il mazzo diventa non valido. In tutti e due i casi il giocatore viene avvisato prima dello scambio.
- I pacchetti estraggono le carte normali; se la carta estratta ha una full art, con la probabilità `packs.full_art_chance` esce la full art al suo posto. I numeri del Dream Pack non cambiano ([012](012-pacchetti.md)).

## Motivazione
Con un id per ogni versione, scambi e collezione non hanno casi speciali, e le skin future saranno altre versioni nello stesso modo, senza colonne nuove. Tenere le statistiche solo sulla carta base evita che le versioni diventino diverse quando si ribilancia il catalogo.

## Conseguenze
- Database: `cards.base_card_id` e `variant_type`; tolte `full_art_illustration_url`, `full_art_thumb_url`, `full_art_focus_y`, `user_cards.full_art_quantity` e le colonne `is_full_art` di `deck_cards`, `pack_opening_cards` e `trade_items` ([schema.md](../database/schema.md)).
- API: l'oggetto carta ha `baseCardId` e `variantType`; spariscono `isFullArt`, `fullArtQuantity` e i campi `fullArt*`; l'avviso sui mazzi ha `outcome` (`INVALID` o `NORMAL_VERSION`) ([guida ai dati](../api/guida-frontend.md)).
- Web: il componente `Card` (#81) monta la full art quando `variantType` è `FULL_ART`; nell'editor dei mazzi (#89) la full art è una voce della collezione, non un interruttore.
- Da aggiornare le issue toccate: #77, #78, #80, #81, #83, #89, #93, #95, #96, #101.
