# 012 · Pacchetti

- **Data:** 2026-10-31 (apertura subito e richiesta del cliente: 2026-11-02; struttura nel database: 2026-11-03)
- **Stato:** approvata; nuovi pacchetti e copertina del Dream Pack (solo il retro delle carte, non più le tre dei Lord): vedi [030](030-nuovi-pacchetti.md)
- **Referenti:** Sara, Maie, Nicolas
- **Task:** #61, domanda 16 del 2/11; #78, #82

## Contesto
Il documento di Sara del 31/10 proponeva il Dream Pack. Al colloquio del 2/11 il cliente ha chiesto **più tipi di pacchetti a prezzi diversi**.

## Opzioni valutate
1. Pacchetto aperto subito all'acquisto, oppure tenuto chiuso in un inventario.
2. Per i nuovi pacchetti: solo i campi che ci sono già (prezzo, slot, pesi, full art, espansione), oppure anche fazione e periodo di vendita.
3. Dentro una rarità, stessa probabilità per ogni carta oppure un peso per carta.

## Decisione
- **Dream Pack:** 5 carte, **500 DC**: 3 Comuni, 1 Rara, 1 slot speciale con Rara 70%, Epica 25%, Leggendaria 4%, Leggendaria full art 1% (cioè Leggendaria 5 e `full_art_chance` 20). Tre copertine, una per Lord, con lo stesso contenuto.
- Il pacchetto **si apre subito**, all'acquisto. Niente inventario.
- Il database permette già **più pacchetti**: prezzo, slot e pesi diversi (anche più cari con carte più rare), più **`packs.faction_id`** (solo una fazione) e **`available_from` / `available_until`** (pacchetti a tempo).
- Dentro una rarità **ogni carta ha la stessa probabilità**.

## Motivazione
Un nuovo pacchetto è una riga di dati, non una tabella nuova. L'apertura immediata evita un inventario.

## Conseguenze
- Tipi, nomi, prezzi e pesi dei nuovi pacchetti li decidono Maie e Nicolas.
- Il negozio (#82) va fatto per più pacchetti.
