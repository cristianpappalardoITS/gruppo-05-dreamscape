-- V11__add_google_login.sql
-- Accesso con Google (#159, decisione 032).
-- google_subject: l'id stabile dell'account Google (claim "sub" dell'id token), non l'email,
-- che su Google può cambiare. NULL per chi non ha collegato Google; un account Google si
-- collega a un solo utente.
-- password_hash diventa facoltativa: chi si registra con Google non ha una password finché
-- non ne sceglie una con il recupero password. Almeno uno dei due modi di accesso deve esserci.
ALTER TABLE users ADD COLUMN google_subject VARCHAR(255) NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_google_subject UNIQUE (google_subject);
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD CONSTRAINT chk_users_login_method
    CHECK (password_hash IS NOT NULL OR google_subject IS NOT NULL);
