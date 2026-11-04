-- V9__create_missions.sql
-- Missioni. Le scelte sono spiegate in docs/database/schema.md.

-- Missione giornaliera e obiettivo settimanale (versione semplice, Sprint 6).
-- All'inizio ci sono solo le due missioni fisse; altre si aggiungono con una riga nuova.
-- period: DAILY = si azzera ogni giorno a mezzanotte, ora italiana; WEEKLY = si azzera
-- il lunedì a mezzanotte, ora italiana.
-- goal_type: cosa si conta, es. PLAY_MATCHES (partite giocate) o WIN_MATCHES (vittorie).
-- goal_count: quante volte farlo; reward_credits: il premio in DC.
-- match_type: quali partite contano. NULL = tutte; CASUAL = solo rapide; RANKED = solo
-- classificate. Quali partite contano per le due missioni lo decidono Maie e Nicolas
-- (proposta: giornaliera tutte, settimanale solo RANKED): basta cambiare il dato.
CREATE TABLE missions (
    id             UUID        NOT NULL DEFAULT uuidv7(),
    code           VARCHAR(40)  NOT NULL,
    name           VARCHAR(80)  NOT NULL,
    description    VARCHAR(255) NOT NULL,
    period         VARCHAR(10)  NOT NULL,
    goal_type      VARCHAR(30)  NOT NULL,
    goal_count     INT          NOT NULL,
    reward_credits INT          NOT NULL,
    match_type     VARCHAR(10)  NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_missions PRIMARY KEY (id),
    CONSTRAINT uq_missions_code UNIQUE (code),
    CONSTRAINT chk_missions_period CHECK (period IN ('DAILY', 'WEEKLY')),
    CONSTRAINT chk_missions_goal_type CHECK (goal_type IN ('PLAY_MATCHES', 'WIN_MATCHES')),
    CONSTRAINT chk_missions_goal_count CHECK (goal_count > 0),
    CONSTRAINT chk_missions_reward CHECK (reward_credits >= 0),
    CONSTRAINT chk_missions_match_type CHECK (match_type IS NULL OR match_type IN ('CASUAL', 'RANKED'))
);

-- L'avanzamento di ogni giocatore in ogni missione, per periodo.
-- period_start: il giorno (DAILY) o il lunedì (WEEKLY) del periodo, in ora italiana.
-- La riga nasce alla prima partita utile del periodo. Quando progress arriva a
-- goal_count si salva completed_at e si paga il premio (credit_transaction_id).
-- Il vincolo UNIQUE impedisce di prendere il premio due volte nello stesso periodo.
CREATE TABLE user_missions (
    id                    UUID        NOT NULL DEFAULT uuidv7(),
    user_id               UUID        NOT NULL,
    mission_id            UUID        NOT NULL,
    period_start          DATE        NOT NULL,
    progress              INT         NOT NULL DEFAULT 0,
    completed_at          TIMESTAMPTZ NULL,
    credit_transaction_id UUID        NULL,
    CONSTRAINT pk_user_missions PRIMARY KEY (id),
    CONSTRAINT uq_user_missions_period UNIQUE (user_id, mission_id, period_start),
    CONSTRAINT fk_user_missions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_missions_mission FOREIGN KEY (mission_id) REFERENCES missions (id),
    CONSTRAINT fk_user_missions_credit FOREIGN KEY (credit_transaction_id) REFERENCES credit_transactions (id) ON DELETE SET NULL,
    CONSTRAINT chk_user_missions_progress CHECK (progress >= 0)
);

-- Le due missioni della versione semplice. I premi sono quelli confermati dal cliente;
-- che cosa si conta lo propongono Maie e Nicolas: per ora contano tutte le partite (match_type NULL).
INSERT INTO missions (code, name, description, period, goal_type, goal_count, reward_credits) VALUES
    ('DAILY_PLAY', 'Missione giornaliera', 'Gioca una partita.', 'DAILY', 'PLAY_MATCHES', 1, 100),
    ('WEEKLY_WIN', 'Obiettivo settimanale', 'Vinci 5 partite.', 'WEEKLY', 'WIN_MATCHES', 5, 250);
