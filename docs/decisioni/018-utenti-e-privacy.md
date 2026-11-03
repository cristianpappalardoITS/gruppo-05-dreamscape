# 018 · Utenti e privacy

- **Data:** 2026-11-03
- **Stato:** approvata
- **Referenti:** SWD, DGS
- **Task:** #71, #73, #114

## Contesto
Restavano aperti alcuni punti di [schema.md](../database/schema.md) su email, dati alla registrazione e cancellazione dell'account.

## Opzioni valutate
1. Email: solo recupero password se c'è tempo, nessuna email, oppure recupero e verifica dell'account.
2. Registrazione: solo username, email, password e consensi, oppure anche la data di nascita.
3. Cancellazione dell'account: anonimizzare, cancellare davvero, oppure solo a mano dall'admin.

## Decisione
- Il portale invia solo l'email di **recupero password, se c'è tempo**. Niente verifica dell'email.
- Alla registrazione si chiedono solo **username, email, password** e i due consensi (Privacy e Termini, caselle separate). L'età minima la dichiarano i Termini.
- L'account si cancella dal profilo **rendendolo anonimo**: username ed email sostituiti, password non più valida, `is_enabled = FALSE`.
- Il primo admin nasce all'avvio da variabili d'ambiente.

## Motivazione
Meno dati personali da proteggere. Lo storico di partite e scambi degli altri giocatori resta completo.

## Conseguenze
- Se il recupero password non si fa, `password_reset_tokens` si toglie con una migration.
- Nuova chiamata `DELETE /me` nella [guida ai dati](../api/guida-frontend.md).
