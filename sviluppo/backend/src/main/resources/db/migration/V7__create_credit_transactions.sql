-- V7__create_credit_transactions.sql
-- Movimenti dei crediti. Le scelte sono spiegate in docs/database/schema.md.

-- Registro di ogni movimento di crediti. users.credits è il saldo attuale;
-- questa tabella spiega come ci si è arrivati.
-- I pacchetti regalati (set iniziale e benvenuto) non muovono crediti.
-- Tipi: SIGNUP_BONUS (500 alla registrazione), DAILY_REWARD (50 al giorno),
-- PACK_PURCHASE, MATCH_REWARD (30 per vittoria classificata, massimo 3 al giorno, non
-- contro un amico), MISSION_REWARD (missioni, collegate da user_missions),
-- LEAGUE_REWARD (prima volta in una lega, collegata da user_league_rewards),
-- ADMIN_ADJUSTMENT.
CREATE TABLE credit_transactions (
    id              UUID        NOT NULL DEFAULT uuidv7(),
    user_id         UUID        NOT NULL,
    type            VARCHAR(30) NOT NULL,
    amount          INT         NOT NULL,
    balance_after   INT         NOT NULL,
    pack_opening_id UUID        NULL,
    match_id        UUID        NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_credit_transactions PRIMARY KEY (id),
    CONSTRAINT fk_credit_transactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_credit_transactions_pack_opening FOREIGN KEY (pack_opening_id) REFERENCES pack_openings (id) ON DELETE SET NULL,
    CONSTRAINT fk_credit_transactions_match FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE SET NULL,
    CONSTRAINT chk_credit_transactions_type CHECK (type IN ('SIGNUP_BONUS', 'DAILY_REWARD', 'PACK_PURCHASE',
                                                            'MATCH_REWARD', 'MISSION_REWARD', 'LEAGUE_REWARD',
                                                            'ADMIN_ADJUSTMENT')),
    CONSTRAINT chk_credit_transactions_amount CHECK (amount <> 0),
    CONSTRAINT chk_credit_transactions_balance CHECK (balance_after >= 0)
);

CREATE INDEX idx_credit_transactions_user_created ON credit_transactions (user_id, created_at);
