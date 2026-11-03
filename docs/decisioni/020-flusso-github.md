# 020 · Flusso di lavoro su GitHub

- **Data:** 2026-11-02 (Sara e Maie nel repository: 2026-11-03)
- **Stato:** approvata
- **Referenti:** Cristian
- **Task:** domande 1, 2 e 3 del 2/11; #68, #111

## Contesto
[workflow-github.md](../workflow-github.md) dice che ogni branch parte da `develop`, ma `develop` non esisteva. [template.md](../template.md#editorconfig) ha un `.editorconfig` che non era nella radice. Il 2/11 Sara e Maie non erano ancora nel repository.

## Opzioni valutate
1. Creare `develop` da `main` e proteggerlo, oppure lavorare su `main`.
2. `.editorconfig` subito, oppure con il progetto backend.

## Decisione
- **`develop`** creato da `main` e protetto. `main` si aggiorna solo da `develop` e resta la versione consegnabile.
- **`.editorconfig`** nella radice insieme al progetto backend (#68).
- **Sara e Maie** sono collaboratrici del repository dal 3/11 (`saramoutawakil-dotcom` e `maie444`), come gli altri membri.

## Motivazione
Si segue il flusso già scritto, e `main` resta sempre pronto per SAL e consegna. Con l'account si possono assegnare a Sara e Maie le loro issue.

## Conseguenze
- Ogni branch parte da `develop` aggiornato; le pull request vanno verso `develop`.
- Le issue di Sara e Maie sono assegnate come in [divisione-task.md](../proposte/divisione-task.md).
- Le consegne di Digital restano sul Drive ([019](019-consegne-di-digital.md)).
