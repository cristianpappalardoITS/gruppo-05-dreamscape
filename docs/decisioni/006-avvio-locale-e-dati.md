# 006 · Avvio in locale e dati per il frontend

- **Data:** 2026-11-02
- **Stato:** approvata
- **Referenti:** Gaia, Marco, Cristian
- **Task:** domande 7 e 9 del 2/11; #68, #71, #72

## Contesto
Il refresh token viaggia in un cookie `HttpOnly`, che funziona senza problemi solo se sito e API hanno lo stesso indirizzo. Backend (#68) e accesso (#71) nascono nello stesso sprint delle prime pagine.

## Opzioni valutate
1. Stesso indirizzo con Docker Compose: il container del frontend serve le pagine e manda `/api` al backend.
2. Due indirizzi (es. Live Server sulla porta 5500, backend sulla 8080): avvio più veloce, ma servono CORS e configurazione del cookie, da cambiare alla consegna.
3. Dati finti in `frontend/mock/` finché il backend non c'è, oppure aspettare il backend.

## Decisione
- **Sito e API sullo stesso indirizzo**, avviati con `docker compose up`. `api.js` usa `/api/v1`, uguale in locale e alla consegna.
- **Niente dati finti** nel frontend: Web aspetta il backend. Nel frattempo prepara progetto, layout, design system e form.

## Motivazione
Nessuna configurazione da cambiare alla consegna e nessun lavoro doppio sui dati.

## Conseguenze
- Le pagine dello Sprint 3 che usano le API dipendono da #68 e #71.
- I dati finti restano solo nel backend (`db/testdata`, profilo `dev` e test).
