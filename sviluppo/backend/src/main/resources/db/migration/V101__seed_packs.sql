-- V101__seed_packs.sql
-- I pacchetti veri: i sei del negozio (decisioni 012 e 030), i tre set iniziali e i due
-- pacchetti di benvenuto (decisione 014). Tutti pescano da tutte le espansioni attive
-- (card_set_id NULL); quelli con una fazione solo dalle carte di quella fazione.
-- Le immagini non ci sono ancora: niente image_url e niente copertine, il negozio mostra
-- un'immagine generica. Arriveranno dal backoffice o con una migration nuova.
-- Prezzi e pesi si cambiano con migration nuove, mai modificando questa.

-- full_art_chance è la percentuale delle Leggendarie che escono in full art:
-- 20 (una su 5) come il Dream Pack, 10 nei pacchetti di una sola fazione.
INSERT INTO packs (name, description, pack_type, price_credits, faction_id, full_art_chance)
SELECT v.name, v.description, v.pack_type, v.price_credits, f.id, v.full_art_chance
FROM (VALUES
    ('Nap Pack',               'Un sogno breve: 3 carte, con una possibilità di Rara, Epica o Leggendaria.',     'SHOP',    300,  NULL,         20),
    ('Dream Pack',             'Il pacchetto di ogni Dreamweaver: 5 carte, con una Rara sicura e uno slot speciale.', 'SHOP', 500, NULL,       20),
    ('Lucid Pack',             'Il Sogno Lucido sceglie per te: 6 carte, solo di Dreams.',                      'SHOP',    600,  'DREAMS',     10),
    ('Faceless Pack',          'L''Uomo Senza Volto sceglie per te: 6 carte, solo di Nightmares.',              'SHOP',    600,  'NIGHTMARES', 10),
    ('Still Time Pack',        'La Custode del Tempo Fermo sceglie per te: 6 carte, solo di Memories.',         'SHOP',    600,  'MEMORIES',   10),
    ('REM Pack',               'Il sonno più profondo: 10 carte, con due slot speciali.',                       'SHOP',    1000, NULL,         20),
    ('Set iniziale Dreams',     'Set iniziale: 14 Comuni e 6 Rare di Dreams.',                                  'STARTER', 0,    'DREAMS',     20),
    ('Set iniziale Nightmares', 'Set iniziale: 14 Comuni e 6 Rare di Nightmares.',                              'STARTER', 0,    'NIGHTMARES', 20),
    ('Set iniziale Memories',   'Set iniziale: 14 Comuni e 6 Rare di Memories.',                                'STARTER', 0,    'MEMORIES',   20),
    ('Welcome Pack',           'Pacchetto di benvenuto: 5 carte di tutte le fazioni, come un Dream Pack.',      'WELCOME', 0,    NULL,         20),
    ('Welcome Pack Epico',     'Pacchetto di benvenuto: 5 carte di tutte le fazioni, con un''Epica sicura.',    'WELCOME', 0,    NULL,         20)
) AS v (name, description, pack_type, price_credits, faction, full_art_chance)
LEFT JOIN factions f ON f.code = v.faction;

-- Slot: (pacchetto, ordine, carte)
INSERT INTO pack_slots (pack_id, sort_order, card_count)
SELECT p.id, v.sort_order, v.card_count
FROM (VALUES
    ('Nap Pack',                1, 2),  ('Nap Pack',                2, 1),
    ('Dream Pack',              1, 3),  ('Dream Pack',              2, 1),  ('Dream Pack',      3, 1),
    ('Lucid Pack',              1, 3),  ('Lucid Pack',              2, 2),  ('Lucid Pack',      3, 1),
    ('Faceless Pack',           1, 3),  ('Faceless Pack',           2, 2),  ('Faceless Pack',   3, 1),
    ('Still Time Pack',         1, 3),  ('Still Time Pack',         2, 2),  ('Still Time Pack', 3, 1),
    ('REM Pack',                1, 6),  ('REM Pack',                2, 2),  ('REM Pack',        3, 2),
    ('Set iniziale Dreams',     1, 14), ('Set iniziale Dreams',     2, 6),
    ('Set iniziale Nightmares', 1, 14), ('Set iniziale Nightmares', 2, 6),
    ('Set iniziale Memories',   1, 14), ('Set iniziale Memories',   2, 6),
    ('Welcome Pack',            1, 3),  ('Welcome Pack',            2, 1),  ('Welcome Pack',       3, 1),
    ('Welcome Pack Epico',      1, 3),  ('Welcome Pack Epico',      2, 1),  ('Welcome Pack Epico', 3, 1)
) AS v (pack_name, sort_order, card_count)
JOIN packs p ON p.name = v.pack_name;

-- Rarità di ogni slot con il peso. Slot speciale: Nap Pack 85/12/3; Dream Pack, pacchetti
-- di fazione e Welcome Pack 70/25/5; REM Pack 65/29/6 (lo slot 3 dà due carte, ognuna
-- estratta con questi pesi).
INSERT INTO pack_slot_rarities (pack_slot_id, rarity_id, weight)
SELECT sl.id, r.id, v.weight
FROM (VALUES
    ('Nap Pack',                1, 'COMMON', 100),
    ('Nap Pack',                2, 'RARE', 85), ('Nap Pack', 2, 'EPIC', 12), ('Nap Pack', 2, 'LEGENDARY', 3),
    ('Dream Pack',              1, 'COMMON', 100), ('Dream Pack', 2, 'RARE', 100),
    ('Dream Pack',              3, 'RARE', 70), ('Dream Pack', 3, 'EPIC', 25), ('Dream Pack', 3, 'LEGENDARY', 5),
    ('Lucid Pack',              1, 'COMMON', 100), ('Lucid Pack', 2, 'RARE', 100),
    ('Lucid Pack',              3, 'RARE', 70), ('Lucid Pack', 3, 'EPIC', 25), ('Lucid Pack', 3, 'LEGENDARY', 5),
    ('Faceless Pack',           1, 'COMMON', 100), ('Faceless Pack', 2, 'RARE', 100),
    ('Faceless Pack',           3, 'RARE', 70), ('Faceless Pack', 3, 'EPIC', 25), ('Faceless Pack', 3, 'LEGENDARY', 5),
    ('Still Time Pack',         1, 'COMMON', 100), ('Still Time Pack', 2, 'RARE', 100),
    ('Still Time Pack',         3, 'RARE', 70), ('Still Time Pack', 3, 'EPIC', 25), ('Still Time Pack', 3, 'LEGENDARY', 5),
    ('REM Pack',                1, 'COMMON', 100), ('REM Pack', 2, 'RARE', 100),
    ('REM Pack',                3, 'RARE', 65), ('REM Pack', 3, 'EPIC', 29), ('REM Pack', 3, 'LEGENDARY', 6),
    ('Set iniziale Dreams',     1, 'COMMON', 100), ('Set iniziale Dreams',     2, 'RARE', 100),
    ('Set iniziale Nightmares', 1, 'COMMON', 100), ('Set iniziale Nightmares', 2, 'RARE', 100),
    ('Set iniziale Memories',   1, 'COMMON', 100), ('Set iniziale Memories',   2, 'RARE', 100),
    ('Welcome Pack',            1, 'COMMON', 100), ('Welcome Pack', 2, 'RARE', 100),
    ('Welcome Pack',            3, 'RARE', 70), ('Welcome Pack', 3, 'EPIC', 25), ('Welcome Pack', 3, 'LEGENDARY', 5),
    ('Welcome Pack Epico',      1, 'COMMON', 100), ('Welcome Pack Epico', 2, 'RARE', 100),
    ('Welcome Pack Epico',      3, 'EPIC', 100)
) AS v (pack_name, sort_order, rarity, weight)
JOIN packs p ON p.name = v.pack_name
JOIN pack_slots sl ON sl.pack_id = p.id AND sl.sort_order = v.sort_order
JOIN rarities r ON r.code = v.rarity;
