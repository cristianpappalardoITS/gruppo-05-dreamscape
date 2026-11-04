-- V3__create_collections_and_decks.sql
-- Collezioni e mazzi. Le scelte sono spiegate in docs/database/schema.md.

-- La collezione: una riga per (utente, carta) con il numero di copie.
-- La full art è una carta a parte: ha la sua riga, con le sue copie.
-- Quando quantity arriverebbe a 0 la riga si cancella.
CREATE TABLE user_cards (
    id                UUID        NOT NULL DEFAULT uuidv7(),
    user_id           UUID        NOT NULL,
    card_id           UUID        NOT NULL,
    quantity          INT         NOT NULL,
    acquired_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_cards PRIMARY KEY (id),
    CONSTRAINT uq_user_cards_user_card UNIQUE (user_id, card_id),
    CONSTRAINT fk_user_cards_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_cards_card FOREIGN KEY (card_id) REFERENCES cards (id),
    CONSTRAINT chk_user_cards_quantity CHECK (quantity > 0)
);

CREATE TABLE decks (
    id         UUID        NOT NULL DEFAULT uuidv7(),
    user_id    UUID        NOT NULL,
    name       VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_decks PRIMARY KEY (id),
    CONSTRAINT uq_decks_user_name UNIQUE (user_id, name),
    CONSTRAINT fk_decks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Ora che decks esiste, si collega il mazzo classificato. Se il mazzo viene cancellato
-- il giocatore resta senza mazzo classificato e il matchmaking non lo sceglie più.
-- Che il mazzo sia del giocatore e valido lo controlla il service.
ALTER TABLE users ADD CONSTRAINT fk_users_ranked_deck FOREIGN KEY (ranked_deck_id) REFERENCES decks (id) ON DELETE SET NULL;

-- Le carte di un mazzo. I vincoli del regolamento (20 carte, copie massime per rarità,
-- limiti per TIER, possesso delle carte, carte attive) si verificano nel service, non qui.
-- card_id può essere una versione (es. la full art): il giocatore mette nel mazzo la
-- versione che vuole mostrare. Le versioni della stessa carta contano insieme per le
-- copie massime (es. una Leggendaria: 1 copia, normale o full art).
CREATE TABLE deck_cards (
    id       UUID NOT NULL DEFAULT uuidv7(),
    deck_id  UUID NOT NULL,
    card_id  UUID NOT NULL,
    quantity INT  NOT NULL,
    CONSTRAINT pk_deck_cards PRIMARY KEY (id),
    CONSTRAINT uq_deck_cards_deck_card UNIQUE (deck_id, card_id),
    CONSTRAINT fk_deck_cards_deck FOREIGN KEY (deck_id) REFERENCES decks (id) ON DELETE CASCADE,
    CONSTRAINT fk_deck_cards_card FOREIGN KEY (card_id) REFERENCES cards (id),
    CONSTRAINT chk_deck_cards_quantity CHECK (quantity > 0)
);
