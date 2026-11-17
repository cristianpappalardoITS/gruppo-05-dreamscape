-- V950__disable_test_cards.sql
-- Solo nel profilo dev (non nei test): dalla V102 c'è il catalogo vero, quindi le carte di prova
-- di V900 si spengono (#165). Catalogo, pacchetti e Benvenuto mostrano solo le 99 carte vere.
-- Gli utenti di prova tengono le carte TEST che hanno già, ma i mazzi che le usano diventano
-- non validi (decisione 026). I test continuano a usare le carte di prova, attive.

UPDATE card_sets
SET is_active = FALSE
WHERE code = 'TEST';

-- Le full art seguono la loro carta base (decisione 026): basta spegnere le carte normali
UPDATE cards
SET is_active = FALSE, updated_at = now()
WHERE base_card_id IS NULL
  AND card_set_id = (SELECT id FROM card_sets WHERE code = 'TEST');
