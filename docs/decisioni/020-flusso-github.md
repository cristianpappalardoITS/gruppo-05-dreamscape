# 020 · Flusso di lavoro su GitHub

- **Data:** 2026-11-02
- **Stato:** approvata; come entrano Sara e Maie è ancora da decidere
- **Referenti:** Cristian
- **Task:** domande 1, 2 e 3 del 2/11; #68

## Contesto
[workflow-github.md](../workflow-github.md) dice che ogni branch parte da `develop`, ma `develop` non esisteva. [template.md](../template.md#editorconfig) ha un `.editorconfig` che non era nella radice. Sara e Maie non sono ancora nel repository.

## Opzioni valutate
1. Creare `develop` da `main` e proteggerlo, oppure lavorare su `main`.
2. `.editorconfig` subito, oppure con il progetto backend.

## Decisione
- **`develop`** creato da `main` e protetto. `main` si aggiorna solo da `develop` e resta la versione consegnabile.
- **`.editorconfig`** nella radice insieme al progetto backend (#68).
- **Da decidere:** come entrano Sara e Maie nel repository (domanda 3).

## Motivazione
Si segue il flusso già scritto, e `main` resta sempre pronto per SAL e consegna.

## Conseguenze
- Ogni branch parte da `develop` aggiornato; le pull request vanno verso `develop`.
