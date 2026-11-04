-- V901__seed_test_packs.sql
-- Pacchetti finti, solo nel profilo dev e nei test: pescano solo dal Set di prova (TEST).
-- Stessi numeri del Dream Pack (decisione 012) e della dotazione iniziale (decisione 014).
-- Quando arriva il catalogo vero, i dati finti restano solo per i test.

INSERT INTO packs (name, description, pack_type, price_credits, card_set_id, faction_id, full_art_chance)
SELECT v.name, v.description, v.pack_type, v.price_credits, s.id, f.id, 20
FROM (VALUES
    ('Pacchetto di prova',              '5 carte del Set di prova.',                        'SHOP',    500, NULL),
    ('Set iniziale di prova Dreams',     'Set iniziale di prova: 14 Comuni e 6 Rare Dreams.',     'STARTER', 0,   'DREAMS'),
    ('Set iniziale di prova Nightmares', 'Set iniziale di prova: 14 Comuni e 6 Rare Nightmares.', 'STARTER', 0,   'NIGHTMARES'),
    ('Set iniziale di prova Memories',   'Set iniziale di prova: 14 Comuni e 6 Rare Memories.',   'STARTER', 0,   'MEMORIES'),
    ('Benvenuto di prova',              'Pacchetto di benvenuto di prova.',                 'WELCOME', 0,   NULL),
    ('Benvenuto di prova con Epica',    'Pacchetto di benvenuto di prova con un''Epica.',   'WELCOME', 0,   NULL)
) AS v (name, description, pack_type, price_credits, faction)
JOIN card_sets s ON s.code = 'TEST'
LEFT JOIN factions f ON f.code = v.faction;

-- Slot: (pacchetto, ordine, carte)
INSERT INTO pack_slots (pack_id, sort_order, card_count)
SELECT p.id, v.sort_order, v.card_count
FROM (VALUES
    ('Pacchetto di prova',               1, 3), ('Pacchetto di prova',               2, 1), ('Pacchetto di prova',               3, 1),
    ('Set iniziale di prova Dreams',     1, 14), ('Set iniziale di prova Dreams',     2, 6),
    ('Set iniziale di prova Nightmares', 1, 14), ('Set iniziale di prova Nightmares', 2, 6),
    ('Set iniziale di prova Memories',   1, 14), ('Set iniziale di prova Memories',   2, 6),
    ('Benvenuto di prova',               1, 3), ('Benvenuto di prova',               2, 1), ('Benvenuto di prova',               3, 1),
    ('Benvenuto di prova con Epica',     1, 3), ('Benvenuto di prova con Epica',     2, 1), ('Benvenuto di prova con Epica',     3, 1)
) AS v (pack_name, sort_order, card_count)
JOIN packs p ON p.name = v.pack_name;

-- Rarità di ogni slot con il peso: lo slot speciale del Dream Pack è Rara 70, Epica 25, Leggendaria 5.
INSERT INTO pack_slot_rarities (pack_slot_id, rarity_id, weight)
SELECT sl.id, r.id, v.weight
FROM (VALUES
    ('Pacchetto di prova',               1, 'COMMON', 100), ('Pacchetto di prova',               2, 'RARE', 100),
    ('Pacchetto di prova',               3, 'RARE', 70), ('Pacchetto di prova', 3, 'EPIC', 25), ('Pacchetto di prova', 3, 'LEGENDARY', 5),
    ('Set iniziale di prova Dreams',     1, 'COMMON', 100), ('Set iniziale di prova Dreams',     2, 'RARE', 100),
    ('Set iniziale di prova Nightmares', 1, 'COMMON', 100), ('Set iniziale di prova Nightmares', 2, 'RARE', 100),
    ('Set iniziale di prova Memories',   1, 'COMMON', 100), ('Set iniziale di prova Memories',   2, 'RARE', 100),
    ('Benvenuto di prova',               1, 'COMMON', 100), ('Benvenuto di prova',               2, 'RARE', 100),
    ('Benvenuto di prova',               3, 'RARE', 70), ('Benvenuto di prova', 3, 'EPIC', 25), ('Benvenuto di prova', 3, 'LEGENDARY', 5),
    ('Benvenuto di prova con Epica',     1, 'COMMON', 100), ('Benvenuto di prova con Epica',     2, 'RARE', 100),
    ('Benvenuto di prova con Epica',     3, 'EPIC', 100)
) AS v (pack_name, sort_order, rarity, weight)
JOIN packs p ON p.name = v.pack_name
JOIN pack_slots sl ON sl.pack_id = p.id AND sl.sort_order = v.sort_order
JOIN rarities r ON r.code = v.rarity;
