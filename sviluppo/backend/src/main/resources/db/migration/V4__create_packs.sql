-- V4__create_packs.sql
-- Pacchetti e aperture. Le scelte sono spiegate in docs/database/schema.md.

-- I pacchetti (gestiti dal backoffice). Oggi in vendita c'è il Dream Pack; il cliente vuole
-- più tipi di pacchetti a prezzi diversi, che sono righe di questa tabella: prezzo, slot,
-- pesi e full_art_chance bastano anche per pacchetti più cari con carte più rare.
-- pack_type: SHOP = in vendita nel negozio; STARTER = set iniziale; WELCOME = pacchetto
-- di benvenuto. STARTER e WELCOME si regalano quando il giocatore sceglie la fazione
-- nella pagina Benvenuto.
-- faction_id: se valorizzato, il pacchetto pesca solo le carte di quella fazione
-- (es. i tre set iniziali, uno per fazione, o un futuro pacchetto di fazione).
-- NULL = tutte le fazioni.
-- available_from / available_until: periodo di vendita per i pacchetti a tempo
-- (es. un evento). NULL = nessun limite da quel lato.
-- Il set iniziale (slot 14 Comuni + 6 Rare) si estrae a caso ma senza superare le copie
-- massime per carta (rarities.max_copies): così forma sempre un mazzo valido, che il
-- giocatore compone da sé. Il limite lo applica il service solo ai pacchetti STARTER.
-- Solo i pacchetti SHOP hanno un prezzo; gli altri non si possono comprare.
-- card_set_id: se valorizzato, il pacchetto pesca solo le carte di quell'espansione;
-- NULL = pesca da tutte le espansioni attive.
-- Si estraggono solo carte normali; poi, se la carta estratta ha una versione full art,
-- con probabilità full_art_chance (in percentuale) esce quella versione al suo posto.
-- Si regola per pacchetto, es. più alta in un pacchetto speciale.
-- Dream Pack: 20% delle Leggendarie, cioè l'1% del pacchetto.
CREATE TABLE packs (
    id              UUID        NOT NULL DEFAULT uuidv7(),
    name            VARCHAR(60)  NOT NULL,
    description     TEXT         NULL,
    pack_type       VARCHAR(20)  NOT NULL DEFAULT 'SHOP',
    price_credits   INT          NOT NULL DEFAULT 0,
    card_set_id     UUID         NULL,
    faction_id      UUID         NULL,
    full_art_chance NUMERIC(5,2) NOT NULL DEFAULT 20.00,
    available_from  TIMESTAMPTZ  NULL,
    available_until TIMESTAMPTZ  NULL,
    image_url       VARCHAR(500) NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_packs PRIMARY KEY (id),
    CONSTRAINT uq_packs_name UNIQUE (name),
    CONSTRAINT fk_packs_card_set FOREIGN KEY (card_set_id) REFERENCES card_sets (id),
    CONSTRAINT fk_packs_faction FOREIGN KEY (faction_id) REFERENCES factions (id),
    CONSTRAINT chk_packs_type CHECK (pack_type IN ('SHOP', 'STARTER', 'WELCOME')),
    CONSTRAINT chk_packs_price CHECK ((pack_type = 'SHOP' AND price_credits > 0)
                                      OR (pack_type <> 'SHOP' AND price_credits = 0)),
    CONSTRAINT chk_packs_full_art_chance CHECK (full_art_chance BETWEEN 0 AND 100),
    CONSTRAINT chk_packs_availability CHECK (available_from IS NULL OR available_until IS NULL
                                             OR available_from < available_until)
);

-- Le copertine di un pacchetto: stesso contenuto, immagine diversa.
-- Es. il pacchetto base ha tre copertine, una per ogni Lord di fazione:
-- il giocatore sceglie quella che preferisce quando compra.
-- Un pacchetto senza copertine usa packs.image_url.
-- Le immagini dei pacchetti e delle copertine seguono le regole delle illustrazioni:
-- una copia WebP con la versione nel nome (es. base-dreams-v1.webp), niente miniatura.
CREATE TABLE pack_covers (
    id         UUID        NOT NULL DEFAULT uuidv7(),
    pack_id    UUID         NOT NULL,
    faction_id UUID         NULL,
    name       VARCHAR(60)  NOT NULL,
    image_url  VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_pack_covers PRIMARY KEY (id),
    CONSTRAINT uq_pack_covers_pack_name UNIQUE (pack_id, name),
    CONSTRAINT fk_pack_covers_pack FOREIGN KEY (pack_id) REFERENCES packs (id) ON DELETE CASCADE,
    CONSTRAINT fk_pack_covers_faction FOREIGN KEY (faction_id) REFERENCES factions (id)
);

-- Gli slot del pacchetto: ogni slot dà card_count carte.
-- La rarità di ogni carta dello slot si estrae con i pesi di pack_slot_rarities.
-- Es. Dream Pack: slot 1 = 3 carte COMMON; slot 2 = 1 carta RARE;
--     slot 3 (speciale) = 1 carta RARE (peso 70), EPIC (peso 25) o LEGENDARY (peso 5).
CREATE TABLE pack_slots (
    id         UUID        NOT NULL DEFAULT uuidv7(),
    pack_id    UUID   NOT NULL,
    sort_order INT    NOT NULL,
    card_count INT    NOT NULL,
    CONSTRAINT pk_pack_slots PRIMARY KEY (id),
    CONSTRAINT uq_pack_slots_pack_order UNIQUE (pack_id, sort_order),
    CONSTRAINT fk_pack_slots_pack FOREIGN KEY (pack_id) REFERENCES packs (id) ON DELETE CASCADE,
    CONSTRAINT chk_pack_slots_card_count CHECK (card_count > 0)
);

-- Le rarità possibili in uno slot, con il loro peso.
-- Probabilità di una rarità = suo peso / somma dei pesi dello slot.
-- Uno slot con una sola riga dà sempre quella rarità (es. Epica garantita).
CREATE TABLE pack_slot_rarities (
    id           UUID        NOT NULL DEFAULT uuidv7(),
    pack_slot_id UUID   NOT NULL,
    rarity_id    UUID   NOT NULL,
    weight       INT    NOT NULL,
    CONSTRAINT pk_pack_slot_rarities PRIMARY KEY (id),
    CONSTRAINT uq_pack_slot_rarities_slot_rarity UNIQUE (pack_slot_id, rarity_id),
    CONSTRAINT fk_pack_slot_rarities_slot FOREIGN KEY (pack_slot_id) REFERENCES pack_slots (id) ON DELETE CASCADE,
    CONSTRAINT fk_pack_slot_rarities_rarity FOREIGN KEY (rarity_id) REFERENCES rarities (id),
    CONSTRAINT chk_pack_slot_rarities_weight CHECK (weight > 0)
);

-- Ogni apertura di un pacchetto (comprato e aperto subito, oppure regalato).
-- source: PURCHASE = comprato nel negozio; SIGNUP = set iniziale e pacchetti di benvenuto,
-- regalati quando il giocatore sceglie la fazione nella pagina Benvenuto.
-- price_paid salva il prezzo del momento, che può cambiare in seguito (0 se regalato).
-- pack_cover_id: la copertina scelta (NULL se il pacchetto non ne ha).
CREATE TABLE pack_openings (
    id            UUID        NOT NULL DEFAULT uuidv7(),
    user_id       UUID        NOT NULL,
    pack_id       UUID        NOT NULL,
    pack_cover_id UUID        NULL,
    source        VARCHAR(20) NOT NULL DEFAULT 'PURCHASE',
    price_paid    INT         NOT NULL,
    opened_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_pack_openings PRIMARY KEY (id),
    CONSTRAINT fk_pack_openings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_pack_openings_pack FOREIGN KEY (pack_id) REFERENCES packs (id),
    CONSTRAINT fk_pack_openings_cover FOREIGN KEY (pack_cover_id) REFERENCES pack_covers (id) ON DELETE SET NULL,
    CONSTRAINT chk_pack_openings_source CHECK (source IN ('PURCHASE', 'SIGNUP')),
    CONSTRAINT chk_pack_openings_price CHECK ((source = 'PURCHASE' AND price_paid > 0)
                                              OR (source <> 'PURCHASE' AND price_paid = 0))
);

CREATE INDEX idx_pack_openings_user_opened ON pack_openings (user_id, opened_at);

-- Le carte uscite da un'apertura: una riga per carta (i doppioni sono righe diverse).
-- card_id è la carta uscita: la full art, se è uscita quella.
CREATE TABLE pack_opening_cards (
    id              UUID NOT NULL DEFAULT uuidv7(),
    pack_opening_id UUID NOT NULL,
    card_id         UUID NOT NULL,
    CONSTRAINT pk_pack_opening_cards PRIMARY KEY (id),
    CONSTRAINT fk_pack_opening_cards_opening FOREIGN KEY (pack_opening_id) REFERENCES pack_openings (id) ON DELETE CASCADE,
    CONSTRAINT fk_pack_opening_cards_card FOREIGN KEY (card_id) REFERENCES cards (id)
);

-- Per mostrare le carte di un'apertura (pagina /negozio/apertura/{id}).
CREATE INDEX idx_pack_opening_cards_opening ON pack_opening_cards (pack_opening_id);
