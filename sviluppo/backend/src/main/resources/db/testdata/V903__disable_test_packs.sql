-- V903__disable_test_packs.sql
-- Dalla V101 ci sono i pacchetti veri (decisioni 014 e 030): i pacchetti di prova di V901 si
-- spengono, altrimenti il negozio ne mostrerebbe uno in più e il Benvenuto aprirebbe quattro
-- pacchetti invece di due. In dev e nei test valgono i pacchetti veri, che pescano anche dalle
-- carte di prova (Set di prova attivo).

UPDATE packs
SET is_active = FALSE
WHERE name IN ('Pacchetto di prova', 'Set iniziale di prova Dreams', 'Set iniziale di prova Nightmares',
               'Set iniziale di prova Memories', 'Benvenuto di prova', 'Benvenuto di prova con Epica');
