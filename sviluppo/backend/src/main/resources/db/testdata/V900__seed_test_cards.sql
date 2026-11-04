-- V900__seed_test_cards.sql
-- Dati finti, solo nel profilo dev e nei test (docs/database/schema.md, "Dati finti per
-- sviluppo e test"). Espansione TEST, codici TEST-, nomi con "(prova)", illustrazioni vuote.
-- Per ogni fazione: 7 Comuni, 3 Rare, 2 Epiche e 1 Leggendaria con la sua full art.
-- 7 Comuni e 3 Rare diverse servono al set iniziale (14 Comuni e 6 Rare, al massimo 2 copie).
-- ATT + DIF seguono la proposta di regolamento: Comune 8-9, Rara 10, Epica 11-12, Leggendaria 13-14.

INSERT INTO card_sets (code, name, description) VALUES
    ('TEST', 'Set di prova', 'Carte finte per sviluppo e test: non vanno nel database di consegna.');

INSERT INTO cards (code, name, attack, defense, faction_id, rarity_id, card_set_id)
SELECT v.code, v.name, v.attack, v.defense, f.id, r.id, s.id
FROM (VALUES
    -- Dreams
    ('TEST-001', 'Lucciola (prova)',               3, 5, 'DREAMS',     'COMMON'),
    ('TEST-002', 'Nuvola Morbida (prova)',         2, 7, 'DREAMS',     'COMMON'),
    ('TEST-003', 'Cuscino di Piume (prova)',       3, 6, 'DREAMS',     'COMMON'),
    ('TEST-004', 'Aquilone (prova)',               4, 4, 'DREAMS',     'COMMON'),
    ('TEST-005', 'Bolla di Sapone (prova)',        2, 6, 'DREAMS',     'COMMON'),
    ('TEST-006', 'Stella Cadente (prova)',         5, 3, 'DREAMS',     'COMMON'),
    ('TEST-007', 'Gatto Sognante (prova)',         4, 5, 'DREAMS',     'COMMON'),
    ('TEST-008', 'Faro Lontano (prova)',           4, 6, 'DREAMS',     'RARE'),
    ('TEST-009', 'Volo Notturno (prova)',          5, 5, 'DREAMS',     'RARE'),
    ('TEST-010', 'Giardino Sospeso (prova)',       3, 7, 'DREAMS',     'RARE'),
    ('TEST-011', 'Arcobaleno Notturno (prova)',    5, 7, 'DREAMS',     'EPIC'),
    ('TEST-012', 'Guardiano dei Sogni (prova)',    4, 8, 'DREAMS',     'EPIC'),
    ('TEST-013', 'Sognatrice Lucida (prova)',      6, 8, 'DREAMS',     'LEGENDARY'),
    -- Nightmares
    ('TEST-014', 'Ombra Strisciante (prova)',      4, 4, 'NIGHTMARES', 'COMMON'),
    ('TEST-015', 'Ragno del Soffitto (prova)',     5, 3, 'NIGHTMARES', 'COMMON'),
    ('TEST-016', 'Passi nel Buio (prova)',         4, 5, 'NIGHTMARES', 'COMMON'),
    ('TEST-017', 'Porta che Cigola (prova)',       3, 5, 'NIGHTMARES', 'COMMON'),
    ('TEST-018', 'Dente Spezzato (prova)',         5, 4, 'NIGHTMARES', 'COMMON'),
    ('TEST-019', 'Sussurro nel Corridoio (prova)', 4, 4, 'NIGHTMARES', 'COMMON'),
    ('TEST-020', 'Specchio Rotto (prova)',         5, 4, 'NIGHTMARES', 'COMMON'),
    ('TEST-021', 'Mostro sotto il Letto (prova)',  6, 4, 'NIGHTMARES', 'RARE'),
    ('TEST-022', 'Caduta nel Vuoto (prova)',       5, 5, 'NIGHTMARES', 'RARE'),
    ('TEST-023', 'Corvo dell''Alba (prova)',       6, 4, 'NIGHTMARES', 'RARE'),
    ('TEST-024', 'Incubo Ricorrente (prova)',      7, 5, 'NIGHTMARES', 'EPIC'),
    ('TEST-025', 'Volto nel Vetro (prova)',        7, 4, 'NIGHTMARES', 'EPIC'),
    ('TEST-026', 'Re delle Ombre (prova)',         9, 5, 'NIGHTMARES', 'LEGENDARY'),
    -- Memories
    ('TEST-027', 'Foto Ingiallita (prova)',        3, 5, 'MEMORIES',   'COMMON'),
    ('TEST-028', 'Carillon (prova)',               2, 7, 'MEMORIES',   'COMMON'),
    ('TEST-029', 'Lettera Dimenticata (prova)',    3, 6, 'MEMORIES',   'COMMON'),
    ('TEST-030', 'Orologio a Pendolo (prova)',     3, 5, 'MEMORIES',   'COMMON'),
    ('TEST-031', 'Vecchio Diario (prova)',         4, 5, 'MEMORIES',   'COMMON'),
    ('TEST-032', 'Chiave Arrugginita (prova)',     3, 6, 'MEMORIES',   'COMMON'),
    ('TEST-033', 'Profumo d''Infanzia (prova)',    2, 6, 'MEMORIES',   'COMMON'),
    ('TEST-034', 'Stanza dei Giochi (prova)',      4, 6, 'MEMORIES',   'RARE'),
    ('TEST-035', 'Eco Lontana (prova)',            4, 6, 'MEMORIES',   'RARE'),
    ('TEST-036', 'Soffitta Polverosa (prova)',     3, 7, 'MEMORIES',   'RARE'),
    ('TEST-037', 'Album di Famiglia (prova)',      5, 7, 'MEMORIES',   'EPIC'),
    ('TEST-038', 'Treno dei Ricordi (prova)',      6, 6, 'MEMORIES',   'EPIC'),
    ('TEST-039', 'Custode della Memoria (prova)',  6, 7, 'MEMORIES',   'LEGENDARY')
) AS v (code, name, attack, defense, faction, rarity)
JOIN factions f ON f.code = v.faction
JOIN rarities r ON r.code = v.rarity
JOIN card_sets s ON s.code = 'TEST';

-- Le full art delle tre Leggendarie: versioni senza statistiche proprie (decisione 025).
INSERT INTO cards (code, base_card_id, variant_type, card_set_id)
SELECT b.code || '-FA', b.id, 'FULL_ART', b.card_set_id
FROM cards b
WHERE b.code IN ('TEST-013', 'TEST-026', 'TEST-039');

-- Effetti: un esempio per ognuno dei 19 effetti del catalogo, al massimo uno per carta.
INSERT INTO card_effects (card_id, effect_id, effect_value)
SELECT c.id, e.id, v.effect_value
FROM (VALUES
    ('TEST-002', 'BARRIER',         NULL::INT),
    ('TEST-003', 'LULLABY',         1),
    ('TEST-005', 'HOPE',            2),
    ('TEST-007', 'REST',            1),
    ('TEST-008', 'PERMANENCE',      1),
    ('TEST-009', 'INTENSIFICATION', 1),
    ('TEST-010', 'CRADLE',          NULL),
    ('TEST-011', 'WEAKENING',       2),
    ('TEST-012', 'BARRIER',         NULL),
    ('TEST-013', 'REST',            3),
    ('TEST-015', 'PUNISHMENT',      1),
    ('TEST-017', 'MIASMA',          1),
    ('TEST-018', 'SCOURGE',         1),
    ('TEST-020', 'LAST_BREATH',     1),
    ('TEST-021', 'PUNISHMENT',      2),
    ('TEST-022', 'VENGEANCE',       3),
    ('TEST-023', 'ABSORPTION',      1),
    ('TEST-024', 'MIASMA',          2),
    ('TEST-025', 'PURE_TERROR',     NULL),
    ('TEST-026', 'WEAKENING',       2),
    ('TEST-028', 'REFLECTION',      1),
    ('TEST-030', 'PERMANENCE',      1),
    ('TEST-032', 'LAST_BREATH',     1),
    ('TEST-033', 'REST',            2),
    ('TEST-034', 'NEUTRALITY',      NULL),
    ('TEST-035', 'NON_SEQUITUR',    NULL),
    ('TEST-036', 'REFLECTION',      2),
    ('TEST-037', 'IMMUNITY',        NULL),
    ('TEST-038', 'MIASMA',          1),
    ('TEST-039', 'PERMANENCE',      2)
) AS v (card_code, effect_code, effect_value)
JOIN cards c ON c.code = v.card_code
JOIN effects e ON e.code = v.effect_code;
