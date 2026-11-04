-- V10__create_leagues.sql
-- Leghe e premi di lega. Le scelte sono spiegate in docs/database/schema.md.

-- Le leghe del rank, dalla più bassa alla più alta: Dormiente, Sognatore, Lucido,
-- Dreamweaver. Si è in una lega quando rating >= min_rating (e sotto la successiva).
-- reward_credits: premio in DC la prima volta che si entra nella lega (importi di Maie).
-- Soglie e premi si cambiano dal backoffice, come le rarità.
CREATE TABLE leagues (
    id             UUID        NOT NULL DEFAULT uuidv7(),
    code           VARCHAR(30) NOT NULL,
    name           VARCHAR(40) NOT NULL,
    min_rating     INT         NOT NULL,
    reward_credits INT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_leagues PRIMARY KEY (id),
    CONSTRAINT uq_leagues_code UNIQUE (code),
    CONSTRAINT uq_leagues_name UNIQUE (name),
    CONSTRAINT uq_leagues_min_rating UNIQUE (min_rating),
    CONSTRAINT chk_leagues_min_rating CHECK (min_rating >= 0),
    CONSTRAINT chk_leagues_reward CHECK (reward_credits >= 0)
);

-- Le leghe raggiunte da ogni giocatore. Una riga nasce la prima volta che entra nella
-- lega e non si cancella se poi scende: per questo il premio arriva una volta sola.
-- credit_transaction_id è NULL se la lega non ha premio.
-- match_id: la partita con cui si è entrati nella lega, per mostrarlo nel risultato
-- (leagueReached) anche se si ricarica la pagina.
CREATE TABLE user_league_rewards (
    id                    UUID        NOT NULL DEFAULT uuidv7(),
    user_id               UUID        NOT NULL,
    league_id             UUID        NOT NULL,
    reached_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    match_id              UUID        NULL,
    credit_transaction_id UUID        NULL,
    CONSTRAINT pk_user_league_rewards PRIMARY KEY (id),
    CONSTRAINT uq_user_league_rewards_user_league UNIQUE (user_id, league_id),
    CONSTRAINT fk_user_league_rewards_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_league_rewards_league FOREIGN KEY (league_id) REFERENCES leagues (id),
    CONSTRAINT fk_user_league_rewards_match FOREIGN KEY (match_id) REFERENCES matches (id) ON DELETE SET NULL,
    CONSTRAINT fk_user_league_rewards_credit FOREIGN KEY (credit_transaction_id) REFERENCES credit_transactions (id) ON DELETE SET NULL
);

-- Le quattro leghe con le soglie proposte. I premi restano a 0 finché Maie non li decide:
-- soglie e premi si cambiano dal backoffice. Chi si registra parte da 1000, cioè Dormiente.
INSERT INTO leagues (code, name, min_rating, reward_credits) VALUES
    ('DORMANT',     'Dormiente',   0,    0),
    ('DREAMER',     'Sognatore',   1100, 0),
    ('LUCID',       'Lucido',      1300, 0),
    ('DREAMWEAVER', 'Dreamweaver', 1500, 0);
