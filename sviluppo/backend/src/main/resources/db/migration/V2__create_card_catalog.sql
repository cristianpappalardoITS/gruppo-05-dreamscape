-- V2__create_card_catalog.sql
-- Catalogo: fazioni, rarità, espansioni, carte, versioni ed effetti. Le scelte sono spiegate in docs/database/schema.md.

-- Le fazioni di Dreamscape: Dreams, Nightmares, Memories.
-- Ogni fazione ha un Lord: una sua carta Leggendaria che la rappresenta
-- (es. "Dreams Lord" = Il Sogno Lucido). È solo titolo e immagine, senza regole
-- di gioco speciali. lord_card_id riceve la chiave esterna dopo la creazione di cards.
CREATE TABLE factions (
    id           UUID        NOT NULL DEFAULT uuidv7(),
    code         VARCHAR(30) NOT NULL,
    name         VARCHAR(50) NOT NULL,
    description  TEXT        NULL,
    lord_title   VARCHAR(50) NULL,
    lord_card_id UUID        NULL,
    CONSTRAINT pk_factions PRIMARY KEY (id),
    CONSTRAINT uq_factions_code UNIQUE (code),
    CONSTRAINT uq_factions_name UNIQUE (name),
    CONSTRAINT uq_factions_lord_card UNIQUE (lord_card_id)
);

-- Una riga = "faction_id è forte contro target_faction_id".
-- I bonus si applicano alla creatura di faction_id quando affronta target_faction_id.
-- Regolamento attuale: ciclo Dreams → Memories → Nightmares → Dreams, +1 attacco.
-- defense_bonus resta disponibile se il bilanciamento lo richiede.
CREATE TABLE faction_advantages (
    id                UUID        NOT NULL DEFAULT uuidv7(),
    faction_id        UUID   NOT NULL,
    target_faction_id UUID   NOT NULL,
    attack_bonus      INT    NOT NULL DEFAULT 0,
    defense_bonus     INT    NOT NULL DEFAULT 0,
    CONSTRAINT pk_faction_advantages PRIMARY KEY (id),
    CONSTRAINT uq_faction_advantages_pair UNIQUE (faction_id, target_faction_id),
    CONSTRAINT fk_faction_advantages_faction FOREIGN KEY (faction_id) REFERENCES factions (id),
    CONSTRAINT fk_faction_advantages_target FOREIGN KEY (target_faction_id) REFERENCES factions (id),
    CONSTRAINT chk_faction_advantages_different CHECK (faction_id <> target_faction_id),
    CONSTRAINT chk_faction_advantages_bonus CHECK (attack_bonus >= 0 AND defense_bonus >= 0
                                                   AND (attack_bonus > 0 OR defense_bonus > 0))
);

-- I TIER di rarità con i vincoli del mazzo.
-- max_per_deck: quante carte di questo TIER al massimo in un mazzo (NULL = nessun limite).
-- max_copies: quante copie della stessa carta di questo TIER al massimo in un mazzo.
-- Regolamento attuale: COMMON NULL/2, RARE 8/2, EPIC 4/2, LEGENDARY 2/1.
CREATE TABLE rarities (
    id           UUID        NOT NULL DEFAULT uuidv7(),
    code         VARCHAR(30) NOT NULL,
    name         VARCHAR(50) NOT NULL,
    sort_order   INT         NOT NULL,
    max_per_deck INT         NULL,
    max_copies   INT         NOT NULL DEFAULT 2,
    CONSTRAINT pk_rarities PRIMARY KEY (id),
    CONSTRAINT uq_rarities_code UNIQUE (code),
    CONSTRAINT uq_rarities_sort_order UNIQUE (sort_order),
    CONSTRAINT chk_rarities_max_per_deck CHECK (max_per_deck IS NULL OR max_per_deck >= 0),
    CONSTRAINT chk_rarities_max_copies CHECK (max_copies > 0)
);

-- Le espansioni (set di carte): set base, espansione 1, ...
-- Servono per i pacchetti che contengono solo le carte di un'espansione.
CREATE TABLE card_sets (
    id          UUID        NOT NULL DEFAULT uuidv7(),
    code        VARCHAR(20) NOT NULL,
    name        VARCHAR(80) NOT NULL,
    description TEXT        NULL,
    released_at DATE        NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_card_sets PRIMARY KEY (id),
    CONSTRAINT uq_card_sets_code UNIQUE (code),
    CONSTRAINT uq_card_sets_name UNIQUE (name)
);

-- Le carte del catalogo (tutte creature) e le loro versioni.
-- code è un identificativo stabile (es. BASE-001): resta uguale anche se il nome
-- cambia, si usa nelle migration con i dati e per il nome dei file immagine.
-- Versioni: la full art (domani anche le skin) è una carta con il suo id e il suo codice
-- (es. BASE-004-FA), collegata alla carta normale da base_card_id. Per collezione, mazzi,
-- scambi e aperture è una carta diversa, ma non ha statistiche proprie: nome, descrizione,
-- ATT, DIF, fazione, rarità ed effetto si leggono dalla carta base (nella versione sono
-- NULL), così una modifica dal backoffice vale per tutte le versioni. Nel mazzo le
-- versioni della stessa carta contano insieme per le copie massime (lo controlla il service).
-- variant_type: NULL = carta normale; FULL_ART = illustrazione verticale 2:3 su tutta la
-- carta, senza cornice. Una versione non ha a sua volta versioni (lo controlla il service).
-- La carta la monta il frontend in JavaScript: illustrazione, cornice della fazione e
-- della rarità, simbolo, nome, testi, ATT e DIF. Qui si salvano solo l'illustrazione
-- e il suo ritaglio; cornici e simboli sono file del frontend.
-- illustration_url: illustrazione della carta (3:2 per la normale, 2:3 per la full art).
-- illustration_focus_y: ritaglio verticale, da 0 (in alto) a 1 (in basso),
-- come la colonna centro_y del compositore (es. Il Sogno Lucido 0.27).
-- Immagini: il PNG originale resta intatto, con i suoi metadati, e non si serve al
-- browser. Dall'originale si creano due copie WebP: grande (larghezza 1024 px) e
-- miniatura (larghezza 400 px). illustration_url = copia grande, illustration_thumb_url
-- = miniatura. Il nome del file ha un numero di versione (es. base-004-v1.webp): una
-- nuova illustrazione ha un nome nuovo, così il browser tiene i file in cache per sempre.
-- attack >= 1: con attacco almeno 1 ogni round toglie difesa e la partita va avanti.
-- is_active = FALSE: la carta non esce più dai pacchetti e non si può usare nei mazzi.
-- Chi la possiede la tiene; i mazzi che la contengono diventano non validi e il
-- giocatore viene avvisato. Una carta posseduta da qualcuno non si cancella mai.
-- Se la carta base è disattivata, valgono come disattivate anche le sue versioni.
CREATE TABLE cards (
    id                     UUID         NOT NULL DEFAULT uuidv7(),
    code                   VARCHAR(20)  NOT NULL,
    base_card_id           UUID         NULL,
    variant_type           VARCHAR(20)  NULL,
    name                   VARCHAR(80)  NULL,
    description            TEXT         NULL,
    attack                 INT          NULL,
    defense                INT          NULL,
    faction_id             UUID         NULL,
    rarity_id              UUID         NULL,
    card_set_id            UUID         NOT NULL,
    illustration_url       VARCHAR(500) NULL,
    illustration_thumb_url VARCHAR(500) NULL,
    illustration_focus_y   NUMERIC(3,2) NOT NULL DEFAULT 0.40,
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_cards PRIMARY KEY (id),
    CONSTRAINT uq_cards_code UNIQUE (code),
    CONSTRAINT uq_cards_name UNIQUE (name),
    CONSTRAINT fk_cards_base_card FOREIGN KEY (base_card_id) REFERENCES cards (id),
    CONSTRAINT fk_cards_faction FOREIGN KEY (faction_id) REFERENCES factions (id),
    CONSTRAINT fk_cards_rarity FOREIGN KEY (rarity_id) REFERENCES rarities (id),
    CONSTRAINT fk_cards_card_set FOREIGN KEY (card_set_id) REFERENCES card_sets (id),
    CONSTRAINT chk_cards_variant_type CHECK (variant_type IS NULL OR variant_type IN ('FULL_ART')),
    -- La carta normale ha tutti i suoi dati; la versione nessuno, li prende dalla base
    CONSTRAINT chk_cards_variant CHECK (
        (base_card_id IS NULL AND variant_type IS NULL AND name IS NOT NULL AND attack IS NOT NULL
         AND defense IS NOT NULL AND faction_id IS NOT NULL AND rarity_id IS NOT NULL)
        OR (base_card_id IS NOT NULL AND variant_type IS NOT NULL AND name IS NULL AND description IS NULL
            AND attack IS NULL AND defense IS NULL AND faction_id IS NULL AND rarity_id IS NULL)),
    CONSTRAINT chk_cards_not_own_base CHECK (base_card_id <> id),
    CONSTRAINT chk_cards_attack CHECK (attack >= 1),
    CONSTRAINT chk_cards_defense CHECK (defense >= 1),
    CONSTRAINT chk_cards_focus CHECK (illustration_focus_y BETWEEN 0 AND 1),
    -- Copia grande e miniatura ci sono sempre insieme
    CONSTRAINT chk_cards_thumbs CHECK ((illustration_url IS NULL) = (illustration_thumb_url IS NULL))
);

-- Le versioni di una carta; al massimo una full art per carta.
CREATE INDEX idx_cards_base_card ON cards (base_card_id);
CREATE UNIQUE INDEX uq_cards_full_art ON cards (base_card_id) WHERE variant_type = 'FULL_ART';

-- Ora che factions esiste, si collega la fazione del set iniziale scelta dal giocatore.
ALTER TABLE users ADD CONSTRAINT fk_users_starter_faction FOREIGN KEY (starter_faction_id) REFERENCES factions (id);

-- Ora che cards esiste, si collega il Lord di ogni fazione alla sua carta.
-- Che sia una Leggendaria normale (non una versione) della stessa fazione lo controlla il service.
ALTER TABLE factions
    ADD CONSTRAINT fk_factions_lord_card FOREIGN KEY (lord_card_id) REFERENCES cards (id);

-- Catalogo degli effetti (decisione 028: catalogo provvisorio, da bilanciare; gli effetti
-- si aggiungono o tolgono con migration di dati). Fanno parte della modalità Base:
-- valgono in ogni partita.
-- code collega la riga all'implementazione Java nel motore di gioco.
-- trigger_event dice quando agisce l'effetto:
--   ON_PLAY      quando la carta viene giocata (fase iniziale)
--   WHILE_IN_PLAY finché la carta resta in campo (es. Barriera, Culla)
--   END_OF_ROUND alla fine di ogni round, se la carta è ancora in campo
--   ON_DEATH     quando la carta viene uccisa
--   ON_KILL      quando la carta uccide una creatura avversaria
-- description è il testo per i giocatori; {value} viene sostituito con card_effects.effect_value.
-- has_value: TRUE se l'effetto ha il valore X (es. "Indebolimento 2"). Il backoffice chiede
-- il numero solo per questi effetti; il service controlla che card_effects.effect_value
-- ci sia se e solo se has_value è TRUE.
-- is_active = FALSE: l'effetto è spento e non si può più assegnare alle carte. Il catalogo è
-- provvisorio (decisione 028): così un effetto si toglie senza cancellare la riga. Le carte che
-- lo hanno già si cambiano con una migration di dati.
CREATE TABLE effects (
    id            UUID        NOT NULL DEFAULT uuidv7(),
    code          VARCHAR(50)  NOT NULL,
    name          VARCHAR(80)  NOT NULL,
    description   VARCHAR(255) NOT NULL,
    trigger_event VARCHAR(20)  NOT NULL,
    has_value     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_effects PRIMARY KEY (id),
    CONSTRAINT uq_effects_code UNIQUE (code),
    CONSTRAINT uq_effects_name UNIQUE (name),
    CONSTRAINT chk_effects_trigger_event CHECK (trigger_event IN ('ON_PLAY', 'WHILE_IN_PLAY', 'END_OF_ROUND',
                                                                  'ON_DEATH', 'ON_KILL'))
);

-- Gli effetti di ogni carta. Lo stesso effetto può avere valori diversi
-- su carte diverse (es. "Castigo {value}": 1 su una carta, 2 su un'altra).
-- effect_value è NULL per gli effetti che non hanno un numero (es. Neutralità).
-- Il regolamento attuale prevede un solo effetto per carta: lo controlla il service,
-- così lo schema resta pronto se in futuro se ne vorranno di più.
-- Gli effetti stanno solo sulle carte normali: le versioni (full art) usano quelli della base.
CREATE TABLE card_effects (
    id           UUID        NOT NULL DEFAULT uuidv7(),
    card_id      UUID   NOT NULL,
    effect_id    UUID   NOT NULL,
    effect_value INT    NULL,
    CONSTRAINT pk_card_effects PRIMARY KEY (id),
    CONSTRAINT uq_card_effects_card_effect UNIQUE (card_id, effect_id),
    CONSTRAINT fk_card_effects_card FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE,
    CONSTRAINT fk_card_effects_effect FOREIGN KEY (effect_id) REFERENCES effects (id)
);
