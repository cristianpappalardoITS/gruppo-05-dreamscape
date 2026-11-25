-- V105__set_weekly_mission_ranked.sql
-- Missioni (#125, decisione 015): la missione giornaliera conta ogni partita, l'obiettivo settimanale solo le
-- vittorie classificate. Così i 250 DC non si prendono con sfide tra amici o partite rapide vinte d'accordo.
-- Proposta della issue, da confermare con Maie e Nicolas: per cambiarla basta questa colonna.

UPDATE missions
SET match_type = 'RANKED'
WHERE code = 'WEEKLY_WIN';
