-- V100__seed_rules_data.sql
-- Dati del regolamento: rarità, fazioni, vantaggi di fazione ed effetti.
-- Valori: decisioni 001, 008, 017 e 028 (docs/decisioni/). Il catalogo degli effetti è
-- provvisorio: si cambia con migration nuove, mai modificando questa.

-- Rarità con i limiti del mazzo: max_per_deck NULL = nessun limite.
INSERT INTO rarities (code, name, sort_order, max_per_deck, max_copies) VALUES
    ('COMMON',    'Comune',      1, NULL, 2),
    ('RARE',      'Rara',        2, 8,    2),
    ('EPIC',      'Epica',       3, 4,    2),
    ('LEGENDARY', 'Leggendaria', 4, 2,    1);

-- Le tre fazioni. Il Lord (lord_card_id) si collega quando arrivano le carte del catalogo.
INSERT INTO factions (code, name, description, lord_title) VALUES
    ('DREAMS',     'Dreams',     'Speranza, desiderio, creatività e protezione. Stile conservativo e curativo.', 'Dreams Lord'),
    ('NIGHTMARES', 'Nightmares', 'Paura, oscurità e perdita di controllo. Stile aggressivo.',                    'Nightmares Lord'),
    ('MEMORIES',   'Memories',   'Ricordi, nostalgia, déjà-vu e illusioni. Stile difensivo.',                     'Memories Lord');

-- Ciclo dei vantaggi: Dreams → Memories → Nightmares → Dreams, +1 attacco.
INSERT INTO faction_advantages (faction_id, target_faction_id, attack_bonus)
SELECT f.id, t.id, 1
FROM factions f
JOIN factions t ON (f.code, t.code) IN (('DREAMS', 'MEMORIES'), ('MEMORIES', 'NIGHTMARES'), ('NIGHTMARES', 'DREAMS'));

-- Catalogo provvisorio degli effetti (decisione 028): 19 effetti, da bilanciare.
-- {value} nel testo si sostituisce con card_effects.effect_value.
INSERT INTO effects (code, name, description, trigger_event, has_value) VALUES
    ('PUNISHMENT',      'Castigo',          'Quando viene giocata: la creatura avversaria subisce {value} danni alla difesa, ma non scende sotto 1.', 'ON_PLAY', TRUE),
    ('SCOURGE',         'Flagello',         'Quando viene giocata: infligge {value} danni ai punti vita dell''avversario.', 'ON_PLAY', TRUE),
    ('REST',            'Riposo',           'Quando viene giocata: il tuo giocatore guadagna {value} punti vita.', 'ON_PLAY', TRUE),
    ('HOPE',            'Speranza',         'Quando viene giocata: +{value} difesa nel primo round dello scontro.', 'ON_PLAY', TRUE),
    ('VENGEANCE',       'Vendetta',         'Quando viene giocata: +{value} attacco nel primo round dello scontro, se nella tua pila degli scarti ci sono almeno 5 carte.', 'ON_PLAY', TRUE),
    ('WEAKENING',       'Indebolimento',    'Nel primo round dello scontro la creatura avversaria ha {value} attacco in meno.', 'WHILE_IN_PLAY', TRUE),
    ('NEUTRALITY',      'Neutralità',       'Non subisce il vantaggio di fazione dell''avversario, ma mantiene il proprio.', 'WHILE_IN_PLAY', FALSE),
    ('IMMUNITY',        'Immunità',         'È immune agli effetti delle carte avversarie.', 'WHILE_IN_PLAY', FALSE),
    ('NON_SEQUITUR',    'Non Sequitur',     'Finché è in campo, annulla gli effetti della creatura avversaria.', 'WHILE_IN_PLAY', FALSE),
    ('CRADLE',          'Culla',            'Finché è in campo, il tuo giocatore non subisce danni ai punti vita.', 'WHILE_IN_PLAY', FALSE),
    ('REFLECTION',      'Riflesso',         'Quando subisce danni in combattimento, la creatura avversaria subisce {value} danni alla difesa.', 'WHILE_IN_PLAY', TRUE),
    ('BARRIER',         'Barriera',         'Il primo colpo che subisce in combattimento non le fa danni.', 'WHILE_IN_PLAY', FALSE),
    ('ABSORPTION',      'Assorbimento',     'Quando infligge danni ai punti vita dell''avversario, il tuo giocatore guadagna {value} punti vita.', 'WHILE_IN_PLAY', TRUE),
    ('PERMANENCE',      'Permanenza',       'Alla fine di ogni round recupera {value} difesa, senza superare quella iniziale.', 'END_OF_ROUND', TRUE),
    ('INTENSIFICATION', 'Intensificazione', 'Alla fine di ogni round guadagna {value} attacco.', 'END_OF_ROUND', TRUE),
    ('MIASMA',          'Miasma',           'Alla fine di ogni round infligge {value} danni ai punti vita dell''avversario.', 'END_OF_ROUND', TRUE),
    ('LULLABY',         'Ninna nanna',      'Alla fine di ogni round il tuo giocatore guadagna {value} punti vita.', 'END_OF_ROUND', TRUE),
    ('LAST_BREATH',     'Ultimo respiro',   'Quando viene uccisa: la creatura che l''ha uccisa subisce {value} danni alla difesa.', 'ON_DEATH', TRUE),
    ('PURE_TERROR',     'Terrore puro',     'Quando uccide: infligge ai punti vita dell''avversario danni pari alla difesa originale della creatura uccisa.', 'ON_KILL', FALSE);
