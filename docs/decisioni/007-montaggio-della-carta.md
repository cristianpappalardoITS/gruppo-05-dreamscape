# 007 · Montaggio della carta e immagini

- **Data:** 2026-10-26 (immagini WebP: 2026-10-27; cornici in JavaScript: 2026-11-02; immagini dal backend: 2026-11-03)
- **Stato:** approvata
- **Referenti:** Marco, Gaia, Yuri
- **Task:** domande 20 e 21 del 2/11; #81, #77

## Contesto
Il 25/10 le carte erano immagini già pronte, fatte dal compositore Python. Ma se cambia una statistica, un'immagine pronta va rifatta. Le illustrazioni originali pesano 2,5–3,3 MB ciascuna.

## Opzioni valutate
1. Immagine della carta già pronta: pro: semplice da mostrare / contro: da rifare a ogni modifica.
2. Carta montata dal frontend con dati e illustrazione: pro: sempre aggiornata / contro: servono cornici con la finestra trasparente e coordinate delle zone.
3. Cornici e coordinate preparate dal compositore Python, oppure dal frontend in JavaScript.
4. Illustrazioni nella cartella del frontend, oppure servite dal backend.

## Decisione
- La carta la **monta il frontend** con il componente React `Card`: illustrazione con il suo ritaglio, cornice della fazione e della rarità, simbolo, nome, testi, ATT e DIF.
- **Cornici con la finestra trasparente e coordinate le prepara il frontend in JavaScript**, partendo dalle 12 cornici di `carte/cornici/`. Il compositore Python non le prepara.
- Le **illustrazioni le serve il backend** (`/images/...`), in due copie WebP: grande (1024 px) e miniatura (400 px), con la versione nel nome e cache per sempre. I PNG originali restano intatti, con i loro metadati. Le copie del catalogo le crea uno script, quelle caricate dal backoffice il backend.
- Nel database solo URL e ritaglio (`illustration_focus_y`).

## Motivazione
La carta si aggiorna da sola. Con le miniature la prima visita alla collezione pesa meno di 1 MB invece di circa 55 MB. Con le immagini nel backend l'admin può aggiungere carte dal backoffice.

## Conseguenze
- Il riferimento visivo sono le carte di prova in `carte/carte-finali/`.
- [Capitolo 7 della guida ai dati](../api/guida-frontend.md#7-come-si-monta-una-carta).
