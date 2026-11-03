# 005 · Frontend: pagine HTML e componenti React

- **Data:** 2026-11-02 (precisata il 2026-11-03)
- **Stato:** approvata, sostituisce [004](004-frontend-senza-framework.md)
- **Referenti:** Gaia, Yuri
- **Task:** domande 8 e 20 del 2/11; #72, #81

## Contesto
La domanda 20 chiedeva come disegnare la carta: canvas oppure livelli HTML e CSS. La carta compare in quasi tutte le pagine (collezione, mazzi, aperture, scambi, partite) e deve essere sempre uguale.

## Opzioni valutate
1. Canvas: pro: stesso risultato delle carte di prova / contro: testi non selezionabili, accessibilità più difficile.
2. Livelli HTML e CSS a mano: pro: testi veri, accessibili / contro: codice ripetuto in ogni pagina.
3. Componenti React: pro: un solo componente `Card` riusato ovunque / contro: serve uno strumento per i componenti.

## Decisione
- Le **pagine restano HTML, CSS e JavaScript**. I **componenti** che si ripetono, come la carta (`Card`, #81), si scrivono in **React**. Il sito **non** diventa un'applicazione React a pagina unica.
- Gli strumenti (proposta: Vite, che gestisce più pagine HTML e i componenti, in JavaScript) li sceglie Gaia nella #72, insieme alla struttura di `frontend/` e ai nomi delle classi CSS (domanda 8).

## Motivazione
Un solo componente per la carta evita copie diverse nelle pagine. Le pagine HTML separate restano semplici, come deciso il 26/10.

## Conseguenze
- Aggiornati [regole-codice.md](../regole-codice.md#frontend), [naming.md](../naming.md#frontend), la [guida ai dati](../api/guida-frontend.md) e la [guida di Web](../guide/web-development.md).
- Le sezioni "Frontend" di naming e regole le completa Gaia dopo la #72.
