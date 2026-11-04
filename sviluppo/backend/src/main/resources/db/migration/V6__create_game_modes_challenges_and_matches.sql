-- V6__create_game_modes_challenges_and_matches.sql
-- Modalità di gioco, sfide e partite. Le scelte sono spiegate in docs/database/schema.md.

-- Le modalità di gioco, cioè le regole. Oggi ce n'è una sola, BASE, con gli effetti delle
-- carte (il brief aggiornato permette di modificare la modalità Base). Partite rapide e
-- classificate usano le stesse regole: il tipo di partita sta in matches.match_type.
-- starting_life_points e round_limit sono i valori del regolamento: un formato futuro
-- che cambia solo i numeri è una riga nuova. is_active lo accende e spegne dal backoffice.
-- Carte per mazzo e copie restano uguali per tutte le modalità (configurazione e rarities),
-- così un mazzo valido si può usare in ogni modalità.
CREATE TABLE game_modes (
    id                   UUID        NOT NULL DEFAULT uuidv7(),
    code                 VARCHAR(30)  NOT NULL,
    name                 VARCHAR(60)  NOT NULL,
    description          VARCHAR(255) NULL,
    starting_life_points INT          NOT NULL DEFAULT 20,
    round_limit          INT          NOT NULL DEFAULT 60,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_game_modes PRIMARY KEY (id),
    CONSTRAINT uq_game_modes_code UNIQUE (code),
    CONSTRAINT chk_game_modes_life_points CHECK (starting_life_points > 0),
    CONSTRAINT chk_game_modes_round_limit CHECK (round_limit > 0)
);

-- La modalità BASE serve sempre: il service la cerca per code e la usa per ogni partita.
INSERT INTO game_modes (code, name, description, starting_life_points, round_limit, is_active)
VALUES ('BASE', 'Base', 'Regolamento di Dreamscape, con gli effetti delle carte.', 20, 60, TRUE);

-- La sfida tra amici: A sceglie il mazzo e sfida un amico B; B accetta scegliendo il suo
-- mazzo. È sempre una partita rapida: niente rank e niente crediti.
-- Che siano amici lo controlla il service. Al massimo una sfida PENDING per coppia
-- (indice uq_challenges_pending_pair). Le partite con chi non è amico non passano da qui:
-- le crea il matchmaking.
-- Se un mazzo viene cancellato la colonna diventa NULL: lo storico resta
-- comunque completo grazie alle copie dei mazzi salvate in matches.
CREATE TABLE challenges (
    id                 UUID        NOT NULL DEFAULT uuidv7(),
    challenger_id      UUID        NOT NULL,
    opponent_id        UUID        NOT NULL,
    challenger_deck_id UUID        NULL,
    opponent_deck_id   UUID        NULL,
    game_mode_id       UUID        NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at        TIMESTAMPTZ NULL,
    CONSTRAINT pk_challenges PRIMARY KEY (id),
    CONSTRAINT fk_challenges_challenger FOREIGN KEY (challenger_id) REFERENCES users (id),
    CONSTRAINT fk_challenges_opponent FOREIGN KEY (opponent_id) REFERENCES users (id),
    CONSTRAINT fk_challenges_challenger_deck FOREIGN KEY (challenger_deck_id) REFERENCES decks (id) ON DELETE SET NULL,
    CONSTRAINT fk_challenges_opponent_deck FOREIGN KEY (opponent_deck_id) REFERENCES decks (id) ON DELETE SET NULL,
    CONSTRAINT fk_challenges_game_mode FOREIGN KEY (game_mode_id) REFERENCES game_modes (id),
    CONSTRAINT chk_challenges_status CHECK (status IN ('PENDING', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_challenges_different_users CHECK (challenger_id <> opponent_id)
);

-- Una sola sfida in attesa per coppia, in qualunque direzione.
CREATE UNIQUE INDEX uq_challenges_pending_pair ON challenges (LEAST(challenger_id, opponent_id),
                                                              GREATEST(challenger_id, opponent_id))
    WHERE status = 'PENDING';
CREATE INDEX idx_challenges_opponent_status ON challenges (opponent_id, status);
CREATE INDEX idx_challenges_challenger_status ON challenges (challenger_id, status);

-- Il risultato della partita simulata dal motore. winner_id NULL = pareggio.
-- match_type: CASUAL = partita rapida (niente rank, niente crediti); RANKED = partita
-- classificata (rank e crediti per la vittoria).
-- challenger_id = chi ha avviato la partita (chi ha lanciato la sfida o premuto
-- "Cerca partita"); opponent_id = chi l'ha accettata o chi ha scelto il matchmaking.
-- challenge_id: valorizzato solo per le sfide tra amici, che sono sempre CASUAL.
-- Le partite del matchmaking (rapide o classificate) non hanno una sfida.
-- starting_life_points e round_limit: copia dei valori della modalità usati in questa
-- partita, così lo storico resta corretto anche se la modalità cambia.
-- *_deck_snapshot: copia dei mazzi al momento della partita (carte con ATT, DIF, fazione,
-- effetti e la versione usata, es. la full art).
-- round_log: lo svolgimento round per round, così come lo restituisce il motore.
-- random_seed: permette di rigiocare la stessa partita per verifiche e test
-- (decide anche il lancio della moneta per l'ordine degli effetti).
-- *_life_points: punti vita alla fine, mai sotto 0 (il motore si ferma a 0).
-- *_rating_change: variazione del punteggio di rank; NULL se la partita non conta
-- per il rank (partita rapida, o classificata oltre la terza del giorno contro lo
-- stesso avversario).
CREATE TABLE matches (
    id                       UUID        NOT NULL DEFAULT uuidv7(),
    match_type               VARCHAR(10) NOT NULL,
    challenge_id             UUID        NULL,
    challenger_id            UUID        NOT NULL,
    opponent_id              UUID        NOT NULL,
    winner_id                UUID        NULL,
    end_reason               VARCHAR(30) NOT NULL,
    total_rounds             INT         NOT NULL,
    starting_life_points     INT         NOT NULL,
    round_limit              INT         NOT NULL,
    challenger_life_points   INT         NOT NULL,
    opponent_life_points     INT         NOT NULL,
    challenger_rating_change INT         NULL,
    opponent_rating_change   INT         NULL,
    random_seed              BIGINT      NOT NULL,
    challenger_deck_snapshot JSONB       NOT NULL,
    opponent_deck_snapshot   JSONB       NOT NULL,
    round_log                JSONB       NOT NULL,
    played_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_matches PRIMARY KEY (id),
    CONSTRAINT uq_matches_challenge UNIQUE (challenge_id),
    CONSTRAINT fk_matches_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_challenger FOREIGN KEY (challenger_id) REFERENCES users (id),
    CONSTRAINT fk_matches_opponent FOREIGN KEY (opponent_id) REFERENCES users (id),
    -- Niente ON DELETE SET NULL: una vittoria non deve mai diventare un pareggio.
    CONSTRAINT fk_matches_winner FOREIGN KEY (winner_id) REFERENCES users (id),
    -- Vince uno dei due giocatori, oppure nessuno (pareggio).
    CONSTRAINT chk_matches_winner CHECK (winner_id IS NULL OR winner_id IN (challenger_id, opponent_id)),
    CONSTRAINT chk_matches_life_points CHECK (challenger_life_points >= 0 AND opponent_life_points >= 0),
    CONSTRAINT chk_matches_type CHECK (match_type IN ('CASUAL', 'RANKED')),
    CONSTRAINT chk_matches_different_users CHECK (challenger_id <> opponent_id),
    -- Le sfide tra amici sono solo partite rapide.
    CONSTRAINT chk_matches_challenge_casual CHECK (challenge_id IS NULL OR match_type = 'CASUAL'),
    -- Le partite rapide non cambiano il rank.
    CONSTRAINT chk_matches_rating CHECK (match_type = 'RANKED'
                                         OR (challenger_rating_change IS NULL AND opponent_rating_change IS NULL)),
    CONSTRAINT chk_matches_end_reason CHECK (end_reason IN ('LIFE_POINTS', 'DECK_EXHAUSTED', 'ROUND_LIMIT')),
    CONSTRAINT chk_matches_total_rounds CHECK (total_rounds >= 1 AND total_rounds <= round_limit)
);

-- Storico delle partite di un giocatore e conteggio delle partite del giorno
-- (massimo 3 contro lo stesso avversario per rank e crediti, massimo 3 vittorie premiate).
CREATE INDEX idx_matches_challenger_played ON matches (challenger_id, played_at);
CREATE INDEX idx_matches_opponent_played ON matches (opponent_id, played_at);
