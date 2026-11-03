# 003 · Accesso con JWT

- **Data:** 2026-10-26 (durata dei token: 2026-11-02)
- **Stato:** approvata
- **Referenti:** SWD
- **Task:** #71, #73; domanda 22 del 2/11

## Contesto
Alla riunione di avvio SWD doveva scegliere come gestire l'accesso: sessioni di Spring Security oppure token JWT.

## Opzioni valutate
1. Sessioni di Spring Security: pro: semplici / contro: backend con stato, più difficile da scalare (es. su AWS).
2. JWT: pro: backend senza stato, stesso meccanismo per sito e API / contro: serve gestire rinnovo e revoca.

## Decisione
- Accesso con **JWT**. Token di accesso di **15 minuti**, tenuto solo in memoria dal JavaScript. **Refresh token di 7 giorni** in un cookie `HttpOnly`, salvato nel database come hash (`refresh_tokens`) e revocato al logout.
- Un solo login per giocatori e admin; il ruolo (`PLAYER`, `ADMIN`) è nel token.
- Password con BCrypt. Il primo admin nasce all'avvio da variabili d'ambiente.

## Motivazione
Il backend resta senza stato. Il token in memoria e il cookie `HttpOnly` proteggono da script estranei (XSS): mai token in `localStorage`.

## Conseguenze
- Il cookie funziona senza problemi solo con sito e API sullo stesso indirizzo: vedi [006](006-avvio-locale-e-dati.md).
- `api.js` rinnova il token da solo all'apertura di ogni pagina ([guida ai dati](../api/guida-frontend.md#3-accesso-con-jwt)).
