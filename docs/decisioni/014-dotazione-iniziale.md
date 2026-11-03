# 014 · Dotazione iniziale

- **Data:** 2026-11-02 (quando arrivano i pacchetti: 2026-11-03)
- **Stato:** approvata
- **Referenti:** Maie, Nicolas, Marco, Alessandro
- **Task:** domande 13 e 15 del 2/11; #71, #73, #114, #115

## Contesto
Alla registrazione erano previsti 500 DC, un set iniziale di 20 carte (14 Comuni, 6 Rare) e 2 pacchetti di benvenuto. Restava da decidere se il nuovo Dreamweaver li riceve tutti, di quali fazioni e quando.

## Opzioni valutate
1. Tutti e tre i regali, oppure solo due.
2. Set iniziale con tutte e tre le fazioni, oppure di una fazione scelta dal giocatore.
3. Scelta della fazione nel form di registrazione oppure nella pagina Benvenuto.
4. Pacchetti di benvenuto alla registrazione oppure insieme al set.
5. Pacchetti di benvenuto con tutte le fazioni oppure con quella scelta.

## Decisione
- Alla registrazione: **500 DC**.
- Nella pagina **Benvenuto** il giocatore **sceglie una fazione** e riceve insieme, una volta sola:
  - il **set iniziale** di 20 carte **di quella fazione** (14 Comuni, 6 Rare, mai oltre le copie massime: forma sempre un mazzo valido);
  - **2 pacchetti di benvenuto** con carte **di tutte le fazioni**, uno con l'Epica garantita.
- Il mazzo non si crea in automatico.

## Motivazione
La registrazione resta corta e il regalo si scopre in un solo momento. Il set di una fazione dà un mazzo coerente; i pacchetti di benvenuto fanno conoscere le altre fazioni.

## Conseguenze
- `users.starter_faction_id`, tre pacchetti STARTER con `packs.faction_id`, chiamata `POST /me/welcome`.
- Ogni fazione deve avere almeno 7 Comuni e 3 Rare diverse ([016](016-catalogo-fase-1.md)).
