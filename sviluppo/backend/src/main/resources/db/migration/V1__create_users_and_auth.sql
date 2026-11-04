-- V1__create_users_and_auth.sql
-- Utenti e accesso. Le scelte sono spiegate in docs/database/schema.md.

-- I Dreamweaver (giocatori) e gli admin.
-- Gli utenti non si cancellano: si disattivano con is_enabled = FALSE.
-- Così storico di sfide e scambi resta sempre completo.
-- rating è il punteggio del rank (Elo con K = 32, si parte da 1000). Cambia solo con le
-- partite classificate. La lega attuale si calcola dal punteggio con la tabella leagues.
-- starter_faction_id: la fazione scelta nella pagina Benvenuto per il set iniziale.
-- NULL finché il giocatore non sceglie; una volta scelta non cambia più, e così
-- set iniziale e pacchetti di benvenuto si ricevono una volta sola.
-- ranked_deck_id: il "mazzo classificato", quello che il matchmaking usa quando il
-- giocatore viene scelto come avversario (anche se non è collegato). La chiave esterna
-- si aggiunge in V3, dopo la creazione di decks.
-- last_daily_reward_on: giorno (ora italiana) dell'ultima ricompensa per l'accesso
-- giornaliero. Un UPDATE atomico su questa colonna impedisce di riceverla due volte.
-- privacy_accepted_at / terms_accepted_at: quando il giocatore ha accettato Privacy Policy
-- e Termini di servizio alla registrazione. Obbligatori per i giocatori; l'admin, creato
-- all'avvio da variabili d'ambiente, non passa dalla registrazione.
-- username: alla registrazione al massimo 30 caratteri (lo controlla il service). La colonna
-- ne tiene 40 per l'account cancellato, che diventa 'deleted-' + id senza trattini.
CREATE TABLE users (
    id                  UUID        NOT NULL DEFAULT uuidv7(),
    username            VARCHAR(40)  NOT NULL,
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    role                VARCHAR(20)  NOT NULL DEFAULT 'PLAYER',
    credits             INT          NOT NULL DEFAULT 0,
    rating              INT          NOT NULL DEFAULT 1000,
    starter_faction_id  UUID         NULL,
    ranked_deck_id      UUID         NULL,
    last_daily_reward_on DATE        NULL,
    is_enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    privacy_accepted_at TIMESTAMPTZ  NULL,
    terms_accepted_at   TIMESTAMPTZ  NULL,
    last_login_at       TIMESTAMPTZ  NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT chk_users_role CHECK (role IN ('PLAYER', 'ADMIN')),
    CONSTRAINT chk_users_credits CHECK (credits >= 0),
    CONSTRAINT chk_users_rating CHECK (rating >= 0),
    CONSTRAINT chk_users_consents CHECK (role = 'ADMIN'
                                         OR (privacy_accepted_at IS NOT NULL AND terms_accepted_at IS NOT NULL))
);

-- Username ed email unici senza distinguere maiuscole e minuscole ("Marco" = "marco").
CREATE UNIQUE INDEX uq_users_username ON users (LOWER(username));
CREATE UNIQUE INDEX uq_users_email ON users (LOWER(email));

-- Accesso con token JWT: il token di accesso dura poco e non si salva;
-- il refresh token serve a ottenerne uno nuovo. Uno per dispositivo/sessione.
-- Si salva l'hash del token, mai il token in chiaro. revoked_at = logout o revoca.
CREATE TABLE refresh_tokens (
    id         UUID        NOT NULL DEFAULT uuidv7(),
    user_id    UUID        NOT NULL,
    token_hash CHAR(64)    NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Per revocare tutti i token di un utente (logout ovunque, cambio password).
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- Recupero password: token monouso con scadenza (serve solo se il portale invia email).
CREATE TABLE password_reset_tokens (
    id         UUID        NOT NULL DEFAULT uuidv7(),
    user_id    UUID        NOT NULL,
    token_hash CHAR(64)    NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
