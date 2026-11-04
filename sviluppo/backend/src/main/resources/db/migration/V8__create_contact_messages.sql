-- V8__create_contact_messages.sql
-- Messaggi del form Contatti. Le scelte sono spiegate in docs/database/schema.md.

-- I messaggi inviati dal form della pagina Contatti. L'admin li legge nella pagina
-- Messaggi del backoffice: nessun invio di email da configurare.
-- user_id è valorizzato se chi scrive ha fatto l'accesso, NULL per i visitatori.
-- status: NEW = da leggere, READ = letto, ARCHIVED = archiviato.
CREATE TABLE contact_messages (
    id         UUID        NOT NULL DEFAULT uuidv7(),
    user_id    UUID         NULL,
    name       VARCHAR(80)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    read_at    TIMESTAMPTZ  NULL,
    CONSTRAINT pk_contact_messages PRIMARY KEY (id),
    CONSTRAINT fk_contact_messages_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_contact_messages_status CHECK (status IN ('NEW', 'READ', 'ARCHIVED'))
);

CREATE INDEX idx_contact_messages_status_created ON contact_messages (status, created_at);
