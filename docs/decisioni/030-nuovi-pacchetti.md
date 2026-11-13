# 030 · Nuovi pacchetti: Nap Pack, pacchetti dei Lord, REM Pack

- **Data:** 2026-11-13 (proposta: 2026-11-09)
- **Stato:** approvata, sostituisce "tre copertine, una per Lord" del Dream Pack di [012](012-pacchetti.md); percentuali provvisorie, da rivedere con i test e le simulazioni (#87)
- **Referenti:** Marco e Cristian (proposta); Maie (prezzi e percentuali)
- **Task:** #129, #78, #82

## Contesto
Al colloquio del 2/11 il cliente ha chiesto più tipi di pacchetti a prezzi diversi ([012](012-pacchetti.md), [022](022-risposte-del-cliente.md)). Il database li prevedeva già: un pacchetto nuovo è una riga di `packs` con i suoi `pack_slots` e `pack_slot_rarities`, più `faction_id` per i pacchetti di una sola fazione. Il 9/11 Marco e Cristian hanno proposto tre nuovi tipi accanto al Dream Pack, con prezzi e percentuali da confermare.

## Opzioni valutate
1. Quali tipi: solo pacchetti più cari, oppure una scala dal piccolo al grande più un pacchetto per fazione.
2. Pacchetti a tempo (`available_from` / `available_until`) già da ora, oppure più avanti.
3. Prezzi in DC oppure anche in euro.
4. Nomi dei pacchetti di fazione: "Dreams Pack", "Nightmares Pack", "Memories Pack", oppure nomi presi dai Lord. "Dreams Pack" si confondeva con "Dream Pack".
5. Copertina del Dream Pack: le tre copertine dei Lord da scegliere, oppure una sola immagine.

## Decisione

### I pacchetti nel negozio
Sei pacchetti, tutti a circa **100 DC a carta**:

| Pacchetto | Carte | Prezzo | Composizione | Fazione |
|-----------|-------|--------|--------------|---------|
| **Nap Pack** | 3 | 300 DC | 2 Comuni, 1 slot speciale piccolo | Tutte |
| **Dream Pack** | 5 | 500 DC | 3 Comuni, 1 Rara, 1 slot speciale | Tutte |
| **Lucid Pack** | 6 | 600 DC | 3 Comuni, 2 Rare, 1 slot speciale | Dreams |
| **Faceless Pack** | 6 | 600 DC | 3 Comuni, 2 Rare, 1 slot speciale | Nightmares |
| **Still Time Pack** | 6 | 600 DC | 3 Comuni, 2 Rare, 1 slot speciale | Memories |
| **REM Pack** | 10 | 1.000 DC | 6 Comuni, 2 Rare, 2 slot speciali migliori | Tutte |

- I nomi seguono il sonno: **Nap** (un pisolino), **Dream**, **REM** (la fase in cui si sogna di più). I tre pacchetti di fazione prendono il nome dal loro Lord: **Lucid** dal Sogno Lucido, **Faceless** dall'Uomo Senza Volto, **Still Time** dalla Custode del Tempo Fermo ([001](001-tema-e-nomi.md)).
- Un pacchetto di fazione pesca **solo le carte della sua fazione** (`packs.faction_id`).
- Prezzi **solo in DC** ([013](013-crediti-e-ricompense.md)). **Niente pacchetti a tempo** per ora.

### Probabilità
Lo slot speciale dà una Rara, un'Epica o una Leggendaria con questi pesi (`pack_slot_rarities.weight`):

| Pacchetto | Slot speciale: Rara / Epica / Leggendaria | `full_art_chance` | Almeno un'Epica | Almeno una Leggendaria | Full art nel pacchetto |
|-----------|-------------------------------------------|-------------------|-----------------|------------------------|------------------------|
| Nap Pack | 85 / 12 / 3 | 20 | 12% | 3% | 0,6% (1 su 170) |
| Dream Pack | 70 / 25 / 5 | 20 | 25% | 5% | 1% (1 su 100) |
| Lucid, Faceless, Still Time Pack | 70 / 25 / 5 | 10 | 25% | 5% | 0,5% (1 su 200) |
| REM Pack | 65 / 29 / 6, in tutti e due gli slot | 20 | 50% | 12% | 2,4% (1 su 42) |

- La full art esce solo al posto di una Leggendaria che ne ha una ([025](025-versioni-delle-carte.md)): `full_art_chance` è la probabilità per ogni Leggendaria. Resta una su 5 come nel Dream Pack, una su 10 nei pacchetti di fazione.
- Nei pacchetti di fazione la Leggendaria esce tra quelle di una sola fazione, quindi una Leggendaria precisa è circa tre volte più facile da trovare: per questo la full art è la metà.
- Valori **provvisori**: si rivedono con i test e le simulazioni.

### Copertine
- **Dream Pack:** una sola immagine, il **retro delle carte** (`packs.image_url`). Niente scelta della copertina all'acquisto.
- **Lucid, Faceless e Still Time Pack:** la copertina del loro Lord (DGS-11).
- **Nap Pack e REM Pack:** immagine ancora da decidere.

### Cosa resta uguale
- Il pacchetto si apre subito, all'acquisto: niente inventario ([012](012-pacchetti.md)).
- Dentro una rarità ogni carta ha la stessa probabilità.

## Motivazione
Con lo stesso prezzo a carta il giocatore sceglie per comodità, non per convenienza nascosta. Ogni 1.000 DC il REM Pack dà circa il 20% di Epiche e Leggendarie in più (premia chi risparmia), mentre Nap Pack e pacchetti di fazione ne danno un po' meno in cambio del prezzo basso o della fazione scelta. I nomi presi dal sonno e dai Lord restano nel tema e non si confondono con il Dream Pack. Con una sola immagine il Dream Pack si riconosce come pacchetto base, e le copertine dei Lord distinguono i pacchetti di fazione.

## Conseguenze
- Una migration aggiunge i sei pacchetti con slot e pesi, insieme ai dati veri dei set iniziali e dei pacchetti di benvenuto ([014](014-dotazione-iniziale.md)). Nessuna tabella nuova.
- Il negozio (#82) mostra sei pacchetti; il Dream Pack non ha più la scelta della copertina.
- Guide di Digital e di Web da aggiornare: nomi, prezzi e copertine.
- Digital prepara le immagini: retro delle carte per il Dream Pack, i tre Lord, e quelle di Nap Pack e REM Pack quando sono decise.
