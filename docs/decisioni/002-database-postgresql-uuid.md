# 002 · Database: PostgreSQL 18 e chiavi UUID v7

- **Data:** 2026-10-26
- **Stato:** approvata
- **Referenti:** SWD (Cristian, Marco, Nicolas)
- **Task:** proposta di database ([schema.md](../database/schema.md))

## Contesto
La prima proposta di database usava MySQL con chiavi numeriche. Gli id finiscono negli URL e nelle API (`/sfide/{id}`), e servono tipi come JSON per lo storico delle partite.

## Opzioni valutate
1. MySQL 8.4 con `BIGINT AUTO_INCREMENT`: pro: conosciuto / contro: id che si indovinano negli URL, meno tipi utili.
2. PostgreSQL 18 con UUID v7: pro: `JSONB`, `TIMESTAMPTZ`, indici su `LOWER()`, `uuidv7()` nativo; un UUID non si indovina ed è ordinato nel tempo / contro: id più lunghi.

## Decisione
**PostgreSQL 18**, chiave primaria `id` di tipo **UUID v7** su ogni tabella. In Java gli UUID li genera Hibernate (`@UuidGenerator(style = VERSION_7)`), il `DEFAULT uuidv7()` serve alle migration con i dati. Schema solo con migration **Flyway**, test con **Testcontainers** (PostgreSQL vero, non H2).

## Motivazione
Id sicuri negli URL, indici efficienti perché ordinati nel tempo, e i tipi che servono allo storico (`JSONB`) e alle date (`TIMESTAMPTZ`). H2 non supporta `JSONB` né gli indici su espressioni.

## Conseguenze
- Il frontend tratta gli id come testi: mai `parseInt(id)`.
- Va controllato che Flyway e RDS supportino PostgreSQL 18.
