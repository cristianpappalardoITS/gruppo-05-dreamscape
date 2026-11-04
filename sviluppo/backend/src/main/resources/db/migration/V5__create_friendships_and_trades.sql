-- V5__create_friendships_and_trades.sql
-- Amici e scambi. Le scelte sono spiegate in docs/database/schema.md.

-- La lista amici. A (requester) manda la richiesta, B (addressee) la accetta.
-- status: PENDING = in attesa di risposta; ACCEPTED = amici.
-- Se B rifiuta, o uno dei due toglie l'amicizia, la riga si cancella.
-- Una sola riga per coppia, in qualunque direzione (indice su LEAST/GREATEST).
CREATE TABLE friendships (
    id           UUID        NOT NULL DEFAULT uuidv7(),
    requester_id UUID        NOT NULL,
    addressee_id UUID        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at  TIMESTAMPTZ NULL,
    CONSTRAINT pk_friendships PRIMARY KEY (id),
    CONSTRAINT fk_friendships_requester FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_friendships_addressee FOREIGN KEY (addressee_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_friendships_status CHECK (status IN ('PENDING', 'ACCEPTED')),
    CONSTRAINT chk_friendships_different_users CHECK (requester_id <> addressee_id),
    CONSTRAINT chk_friendships_accepted CHECK ((status = 'ACCEPTED') = (accepted_at IS NOT NULL))
);

CREATE UNIQUE INDEX uq_friendships_pair ON friendships (LEAST(requester_id, addressee_id),
                                                        GREATEST(requester_id, addressee_id));
CREATE INDEX idx_friendships_addressee_status ON friendships (addressee_id, status);
CREATE INDEX idx_friendships_requester_status ON friendships (requester_id, status);

-- Gli scambi: solo tra amici (lo controlla il service, alla proposta e all'accettazione).
-- Quantità e rarità libere: si può dare 1 Epica per 5 Comuni. Lo scambio avviene
-- solo se il destinatario accetta.
-- Niente ON DELETE CASCADE verso users: gli utenti non si cancellano e lo storico resta.
CREATE TABLE trades (
    id           UUID        NOT NULL DEFAULT uuidv7(),
    proposer_id  UUID         NOT NULL,
    recipient_id UUID         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    message      VARCHAR(255) NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at  TIMESTAMPTZ  NULL,
    CONSTRAINT pk_trades PRIMARY KEY (id),
    CONSTRAINT fk_trades_proposer FOREIGN KEY (proposer_id) REFERENCES users (id),
    CONSTRAINT fk_trades_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT chk_trades_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_trades_different_users CHECK (proposer_id <> recipient_id)
);

CREATE INDEX idx_trades_recipient_status ON trades (recipient_id, status);
-- Serve anche a contare le copie già offerte negli scambi in attesa (bloccate per gli altri scambi).
CREATE INDEX idx_trades_proposer_status ON trades (proposer_id, status);

-- Le carte dello scambio. side = OFFERED (le dà chi propone)
-- oppure REQUESTED (le dà chi riceve). Permette scambi 3 contro 1, 1 contro 5...
-- La full art è una carta a parte: si scambia con il suo card_id.
-- Le copie OFFERED di uno scambio PENDING sono bloccate per gli altri scambi di chi
-- propone (le conta il service da qui), ma restano usabili nei mazzi.
CREATE TABLE trade_items (
    id          UUID        NOT NULL DEFAULT uuidv7(),
    trade_id    UUID        NOT NULL,
    card_id     UUID        NOT NULL,
    side        VARCHAR(10) NOT NULL,
    quantity    INT         NOT NULL,
    CONSTRAINT pk_trade_items PRIMARY KEY (id),
    CONSTRAINT uq_trade_items_trade_card_side UNIQUE (trade_id, card_id, side),
    CONSTRAINT fk_trade_items_trade FOREIGN KEY (trade_id) REFERENCES trades (id) ON DELETE CASCADE,
    CONSTRAINT fk_trade_items_card FOREIGN KEY (card_id) REFERENCES cards (id),
    CONSTRAINT chk_trade_items_side CHECK (side IN ('OFFERED', 'REQUESTED')),
    CONSTRAINT chk_trade_items_quantity CHECK (quantity > 0)
);
