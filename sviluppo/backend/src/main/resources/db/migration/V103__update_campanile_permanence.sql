-- V103__update_campanile_permanence.sql
-- Il Campanile tra le Nuvole (BASE-036, 6/16) passa da Permanenza 6 a Permanenza 5 (#87,
-- approvato da Maie e Nicolas il 18/11, decisione 008). Con Permanenza 6 due copie in campo
-- si fanno 6 danni e ne recuperano 6 a ogni round: nessuna muore e la partita resta ferma
-- fino al limite di round. Dettagli in docs/proposte/simulazioni-partite.md.
-- Stessa forma delle righe degli effetti di V102: le simulazioni del motore (engine-simulation)
-- leggono queste righe dopo quelle di V102.

UPDATE card_effects ce
SET effect_value = v.effect_value
FROM (VALUES
    ('BASE-036', 'PERMANENCE', 5)
) AS v (card_code, effect_code, effect_value)
JOIN cards c ON c.code = v.card_code
JOIN effects e ON e.code = v.effect_code
WHERE ce.card_id = c.id
  AND ce.effect_id = e.id;
