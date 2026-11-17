-- V102__seed_base_set.sql
-- Il catalogo vero del set base: 99 carte, 33 per fazione, e le 12 full art delle Leggendarie
-- (#165). Dati da carte/compositore/carte.csv e illustrazioni da carte/illustrazioni/ (PR #157);
-- le copie WebP le crea carte/compositore/crea_webp.py in src/main/resources/images/cards/.
-- Decisioni 007 (immagini), 016 (catalogo), 025 (full art) e 028 (effetti).
-- Una carta si corregge con una migration nuova (UPDATE), mai modificando questa.

INSERT INTO card_sets (code, name, description) VALUES
    ('BASE', 'Set base', 'Le 99 carte della fase 1: 33 per fazione.');

-- Carte normali. Ritaglio dell'illustrazione: quello di base (0,40), perché in carte.csv
-- la colonna centro_y è vuota per tutte le carte.
INSERT INTO cards (code, name, description, attack, defense, faction_id, rarity_id, card_set_id,
                   illustration_url, illustration_thumb_url)
SELECT v.code, v.name, v.description, v.attack, v.defense, f.id, r.id, s.id,
       '/images/cards/' || lower(v.code) || '-v1.webp', '/images/cards/' || lower(v.code) || '-v1-thumb.webp'
FROM (VALUES
    -- Dreams
    ('BASE-001', 'Il Rifugio Sicuro', 'Il desiderio di un posto dove nulla può raggiungerti.', 5, 13, 'DREAMS', 'COMMON'),
    ('BASE-002', 'Il Volo Sopra la Città', 'Per una notte, la gravità smette di contare.', 9, 11, 'DREAMS', 'RARE'),
    ('BASE-003', 'Il Faro nella Nebbia', 'Una luce lontana che indica sempre la strada di casa.', 7, 13, 'DREAMS', 'EPIC'),
    ('BASE-004', 'Il Sogno Lucido', 'Sai di sognare, e il sogno ti obbedisce.', 12, 16, 'DREAMS', 'LEGENDARY'),
    ('BASE-013', 'La Coperta Calda', 'Il peso leggero che tiene lontano il buio.', 3, 10, 'DREAMS', 'COMMON'),
    ('BASE-014', 'Il Gatto sul Cuscino', 'Fa le fusa finché non ti addormenti.', 5, 9, 'DREAMS', 'COMMON'),
    ('BASE-015', 'La Ninna Nanna', 'Una voce lontana che rallenta tutto.', 5, 11, 'DREAMS', 'COMMON'),
    ('BASE-016', 'Il Cerbiatto di Brina', 'Lascia sul sentiero impronte che brillano fino all''alba.', 6, 12, 'DREAMS', 'COMMON'),
    ('BASE-017', 'La Piccola Nuvola', 'Soffice e candida, come lo zucchero filato.', 5, 15, 'DREAMS', 'COMMON'),
    ('BASE-018', 'La Lepre del Mattino', 'Arriva dove la notte comincia a schiarire.', 7, 9, 'DREAMS', 'COMMON'),
    ('BASE-019', 'Il Pesce di Carta', 'Nuota nei sogni lasciati a metà.', 6, 13, 'DREAMS', 'COMMON'),
    ('BASE-020', 'Il Custode del Respiro', 'Conta i respiri di chi fatica a dormire.', 5, 13, 'DREAMS', 'COMMON'),
    ('BASE-021', 'La Lucciola della Soglia', 'Fa luce quanto basta per compiere il primo passo.', 6, 8, 'DREAMS', 'COMMON'),
    ('BASE-022', 'Il Germoglio d''Argento', 'Cresce anche nei sogni che nessuno ricorda.', 5, 12, 'DREAMS', 'COMMON'),
    ('BASE-023', 'L''Amico che Risponde', 'Chi non vorrebbe poter parlare con il proprio amico a quattro zampe?', 6, 12, 'DREAMS', 'COMMON'),
    ('BASE-024', 'La Medusa del Dormiveglia', 'Custodisce il silenzio appena prima del sonno.', 5, 10, 'DREAMS', 'COMMON'),
    ('BASE-025', 'La Camera dei Desideri', 'Basta pensare a un oggetto e per magia può comparire qui dentro.', 6, 12, 'DREAMS', 'COMMON'),
    ('BASE-026', 'L''Ampolla dei Mille Colori', 'Il colore, dice l''alchimista, è una questione di presentazione.', 3, 10, 'DREAMS', 'COMMON'),
    ('BASE-027', 'L''Aquilone Rosso', 'Arriva sempre prima del vento.', 7, 10, 'DREAMS', 'RARE'),
    ('BASE-028', 'Il Giardino di Nuvole', 'Ogni passo affonda in qualcosa di morbido.', 6, 14, 'DREAMS', 'RARE'),
    ('BASE-029', 'Il Grifone di Luna', 'Una creatura mistica, dal grande potere e dal grazioso volo.', 9, 8, 'DREAMS', 'RARE'),
    ('BASE-030', 'Il Pellegrino di Vetro', 'Porta la calma oltre l''ultimo ponte.', 7, 13, 'DREAMS', 'RARE'),
    ('BASE-031', 'Il Cavaliere di Marzapane', 'Un biscotto piccolo e coraggioso. Nessuno gli dica che è stato adottato dal suo fornaio.', 7, 10, 'DREAMS', 'RARE'),
    ('BASE-032', 'L''Ingranaggio Antico', 'Una volta parte di un macchinario, non ha ancora smesso di ruotare.', 7, 13, 'DREAMS', 'RARE'),
    ('BASE-033', 'Il Cavallo di Schiuma', 'Corre dove il mare incontra il cielo.', 8, 9, 'DREAMS', 'RARE'),
    ('BASE-034', 'Il Protagonista', 'In ogni sogno c''è qualcuno che non si arrende. Di solito sei tu.', 11, 15, 'DREAMS', 'LEGENDARY'),
    ('BASE-035', 'La Balena Celeste', 'Nuota tra le stelle con i sognatori sul dorso.', 7, 16, 'DREAMS', 'EPIC'),
    ('BASE-036', 'Il Campanile tra le Nuvole', 'Al rintocco di ogni ora, la campana suona solennemente.', 6, 16, 'DREAMS', 'EPIC'),
    ('BASE-037', 'Il Peschereccio Volante', 'Una nave equipaggiata a resistere alle turbolenze di mare e di aria.', 10, 13, 'DREAMS', 'EPIC'),
    ('BASE-038', 'Il Golem della Montagna', 'Rocce senzienti che si sono agglomerate in un unico essere. Ha un animo gentile e la mano pesante.', 8, 14, 'DREAMS', 'EPIC'),
    ('BASE-039', 'Lo Stregone Distratto', 'Dice di saper trasportare chiunque ovunque, ma non sa dove finiscono le cose che fa sparire.', 9, 10, 'DREAMS', 'EPIC'),
    ('BASE-040', 'La Tessitrice dell''Alba', 'Cuce la luce del mattino sopra chi ha dormito male.', 10, 16, 'DREAMS', 'LEGENDARY'),
    ('BASE-041', 'L''Atlante delle Nuvole', 'Porta sulle corna le isole che nessuno vuole lasciar cadere.', 8, 16, 'DREAMS', 'LEGENDARY'),
    -- Nightmares
    ('BASE-005', 'Il Dente Che Cade', 'Un incubo piccolo, ma che ritorna sempre.', 10, 6, 'NIGHTMARES', 'COMMON'),
    ('BASE-006', 'La Caduta Senza Fine', 'La sensazione di perdere il controllo, senza mai toccare terra.', 12, 7, 'NIGHTMARES', 'RARE'),
    ('BASE-007', 'La Stanza Senza Porte', 'Quattro muri, nessuna uscita, e il respiro che accelera.', 11, 11, 'NIGHTMARES', 'EPIC'),
    ('BASE-008', 'L''Uomo Senza Volto', 'La paura dell''ignoto ha un corpo, ma non un viso.', 13, 11, 'NIGHTMARES', 'LEGENDARY'),
    ('BASE-042', 'L''Ombra sulla Soglia', 'Qualcuno è entrato. La porta era chiusa a chiave.', 7, 6, 'NIGHTMARES', 'COMMON'),
    ('BASE-043', 'La Mano Sotto il Letto', 'Basta lasciare un piede fuori dalle coperte.', 7, 6, 'NIGHTMARES', 'COMMON'),
    ('BASE-044', 'Il Telefono che Squilla', 'Squilla alle tre di notte. Dall''altra parte, il tuo respiro.', 7, 7, 'NIGHTMARES', 'COMMON'),
    ('BASE-045', 'La Melma della Cantina', 'Sale un gradino ogni notte.', 6, 11, 'NIGHTMARES', 'COMMON'),
    ('BASE-046', 'Il Cavaliere Vuoto', 'L''armatura cammina ancora. Dentro non c''è nessuno.', 7, 8, 'NIGHTMARES', 'COMMON'),
    ('BASE-047', 'La Tagliola Viva', 'Scatta prima che tu veda dove metti il piede.', 7, 6, 'NIGHTMARES', 'COMMON'),
    ('BASE-048', 'La Bestia della Brughiera', 'Ululati che si avvicinano, mai che si allontanano.', 9, 6, 'NIGHTMARES', 'COMMON'),
    ('BASE-049', 'Il Viandante Bendato', 'Non vede dove va. Sa dove sei tu.', 8, 6, 'NIGHTMARES', 'COMMON'),
    ('BASE-050', 'Quello nell''Angolo', 'Se smetti di guardarlo, si avvicina.', 9, 4, 'NIGHTMARES', 'COMMON'),
    ('BASE-051', 'Il Sorriso Forzato', 'Tutti ti dicono di sorridere. Lui non ti lascia smettere.', 7, 6, 'NIGHTMARES', 'COMMON'),
    ('BASE-052', 'Il Giocattolo a Molla', 'Qualcuno l''ha caricato. Nessuno sa chi.', 7, 7, 'NIGHTMARES', 'COMMON'),
    ('BASE-053', 'Il Trono di Spine', 'Siediti. Non potrai più alzarti.', 7, 9, 'NIGHTMARES', 'COMMON'),
    ('BASE-054', 'La Maschera Vuota', 'Dietro una maschera c''è sempre qualcuno. Quasi sempre.', 7, 6, 'NIGHTMARES', 'COMMON'),
    ('BASE-055', 'Il Monolito che Cammina', 'Ieri era in fondo alla valle.', 6, 12, 'NIGHTMARES', 'COMMON'),
    ('BASE-056', 'Il Letto Sospeso', 'Ti svegli e il pavimento non c''è più.', 9, 7, 'NIGHTMARES', 'RARE'),
    ('BASE-057', 'Il Lupo di Vetro', 'Ogni specchio rotto ne libera un pezzo.', 10, 8, 'NIGHTMARES', 'RARE'),
    ('BASE-058', 'L''Occhio che Non Dorme', 'Quella sensazione di essere osservato? Avevi ragione.', 9, 10, 'NIGHTMARES', 'RARE'),
    ('BASE-059', 'Il Bosco che Ti Segue', 'Gli alberi non si muovono. Questi sì.', 8, 11, 'NIGHTMARES', 'RARE'),
    ('BASE-060', 'La Casa che Respira', 'Le finestre si accendono quando le guardi.', 10, 9, 'NIGHTMARES', 'RARE'),
    ('BASE-061', 'Il Corvo dei Lampioni', 'Spegne una luce alla volta, lungo la strada di casa.', 11, 7, 'NIGHTMARES', 'RARE'),
    ('BASE-062', 'Il Ladro di Chiavi', 'Ogni porta che chiudi, lui la riapre.', 10, 8, 'NIGHTMARES', 'RARE'),
    ('BASE-063', 'Il Vuoto', 'Non è buio. È assenza.', 14, 12, 'NIGHTMARES', 'LEGENDARY'),
    ('BASE-064', 'Il Treno Senza Fermate', 'Le fermate passano, ma le porte non si aprono.', 12, 10, 'NIGHTMARES', 'EPIC'),
    ('BASE-065', 'Il Sarcofago di Ferro', 'Si apre solo dall''esterno.', 9, 11, 'NIGHTMARES', 'EPIC'),
    ('BASE-066', 'La Bestia dei Campanacci', 'La senti arrivare. Non la senti mai andare via.', 11, 7, 'NIGHTMARES', 'EPIC'),
    ('BASE-067', 'Il Trampoliere del Luna Park', 'Il luna park è chiuso da anni. Lui balla ancora.', 10, 9, 'NIGHTMARES', 'EPIC'),
    ('BASE-068', 'Il Cancello Socchiuso', 'Da quello spiraglio entra sempre il freddo.', 9, 12, 'NIGHTMARES', 'EPIC'),
    ('BASE-069', 'Il Divoratore di Echi', 'Ogni grido che lanci nel buio, lui lo mangia.', 16, 10, 'NIGHTMARES', 'LEGENDARY'),
    ('BASE-070', 'La Regina delle Porte Chiuse', 'Ogni porta che non hai il coraggio di aprire è sua.', 12, 13, 'NIGHTMARES', 'LEGENDARY'),
    -- Memories
    ('BASE-009', 'Il Ricordo Sbiadito', 'Il timore di dimenticare ciò che contava.', 6, 9, 'MEMORIES', 'COMMON'),
    ('BASE-010', 'Il Déjà-vu', 'Hai già vissuto questo momento. Sai come finisce.', 8, 11, 'MEMORIES', 'RARE'),
    ('BASE-011', 'Il Corridoio Infinito', 'Smarrimento e incertezza, porta dopo porta.', 7, 12, 'MEMORIES', 'EPIC'),
    ('BASE-012', 'La Custode del Tempo Fermo', 'Custodisce l''attimo che non vuoi lasciare andare.', 9, 12, 'MEMORIES', 'LEGENDARY'),
    ('BASE-071', 'La Foto Ingiallita', 'I volti ci sono ancora. I nomi no.', 7, 11, 'MEMORIES', 'COMMON'),
    ('BASE-072', 'La Casa dei Nonni', 'Profuma ancora di domenica.', 5, 13, 'MEMORIES', 'COMMON'),
    ('BASE-073', 'Il Ritornello', 'La canzone di un''estate che non ti esce dalla testa.', 6, 9, 'MEMORIES', 'COMMON'),
    ('BASE-074', 'Il Diario Segreto', 'Quando lo leggi, rivivi una parte di te.', 7, 11, 'MEMORIES', 'COMMON'),
    ('BASE-075', 'Gomma e Matita', 'Una scrive, l''altra cancella. Nulla di più semplice.', 6, 10, 'MEMORIES', 'COMMON'),
    ('BASE-076', 'Il Filo del Maglione', 'Ritrova le mani che lo hanno annodato.', 6, 10, 'MEMORIES', 'COMMON'),
    ('BASE-077', 'La Biglia in Tasca', 'Dentro gira ancora il cielo del cortile.', 8, 8, 'MEMORIES', 'COMMON'),
    ('BASE-078', 'Il Segno sul Muro', 'Ogni anno resta un poco più in basso.', 6, 13, 'MEMORIES', 'COMMON'),
    ('BASE-079', 'Il Guanto Smarrito', 'Aspetta l''inverno in cui lo ritroverai.', 7, 9, 'MEMORIES', 'COMMON'),
    ('BASE-080', 'La Conchiglia del Cassetto', 'Fa ancora il rumore di quella spiaggia.', 6, 10, 'MEMORIES', 'COMMON'),
    ('BASE-081', 'La Poltrona che Ricorda', 'La seduta conserva ancora la forma di chi manca.', 6, 9, 'MEMORIES', 'COMMON'),
    ('BASE-082', 'L''Altalena Libera', 'Puoi dondolartici quanto vuoi. Non c''è nessuno in coda.', 7, 9, 'MEMORIES', 'COMMON'),
    ('BASE-083', 'La Tazza Sbeccata', 'Resta tiepida a lungo dopo l''ultimo sorso.', 6, 13, 'MEMORIES', 'COMMON'),
    ('BASE-084', 'Il Riflesso della Pioggia', 'Nel vetro piove ancora come quel pomeriggio.', 6, 10, 'MEMORIES', 'COMMON'),
    ('BASE-085', 'L''Orologio a Cucù', 'Sempre puntuale, anche quando il tempo si è fermato.', 9, 10, 'MEMORIES', 'RARE'),
    ('BASE-086', 'La Lettera Mai Spedita', 'Le parole giuste, scritte troppo tardi.', 8, 11, 'MEMORIES', 'RARE'),
    ('BASE-087', 'Il Cervo dell''Album', 'Si riconosce in tutte le foto di famiglia.', 7, 11, 'MEMORIES', 'RARE'),
    ('BASE-088', 'Il Geco della Carta da Parati', 'Tiene addosso il disegno della stanza in cui sei cresciuto.', 8, 13, 'MEMORIES', 'RARE'),
    ('BASE-089', 'L''Archivista di Cera', 'Raccoglie gli oggetti di cui tutti hanno perso il nome.', 7, 12, 'MEMORIES', 'RARE'),
    ('BASE-090', 'L''Autoritratto', 'Dopo avercela messa tutta, non ti assomiglia affatto, ma ne vai fiero lo stesso.', 9, 11, 'MEMORIES', 'RARE'),
    ('BASE-091', 'La Minestra Senza Fine', 'Patata, carota, zucchina, pomodoro, cavolfiore, piselli, fagioli e carciofi.', 7, 9, 'MEMORIES', 'RARE'),
    ('BASE-092', 'L''Anello della Promessa', 'Al dito dell''unica persona cui si giura fedeltà, questo anello simboleggia quel legame indissolubile.', 13, 15, 'MEMORIES', 'LEGENDARY'),
    ('BASE-093', 'Il Carillon', 'Gira ancora, anche se nessuno lo carica più.', 10, 13, 'MEMORIES', 'EPIC'),
    ('BASE-094', 'Il Teatro delle Voci Perse', 'Ricordi l''applauso. Il resto della serata è svanito.', 10, 13, 'MEMORIES', 'EPIC'),
    ('BASE-095', 'Il Mastino dei Ritratti', 'Riconosce le persone che hai dimenticato.', 10, 13, 'MEMORIES', 'EPIC'),
    ('BASE-096', 'La Clessidra Capovolta', 'Molta sabbia si è posata, e ancora molta dovrà cadere.', 7, 12, 'MEMORIES', 'EPIC'),
    ('BASE-097', 'Il Pavone dei Negativi', 'Apre la coda sulle immagini che nessuno ha mai stampato.', 10, 11, 'MEMORIES', 'EPIC'),
    ('BASE-098', 'Il Bibliotecario delle Estati', 'Conserva ogni estate in un cassetto, anche quelle che hai dimenticato.', 10, 16, 'MEMORIES', 'LEGENDARY'),
    ('BASE-099', 'L''Elefante dei Giorni Uguali', 'Ogni giorno è uguale al precedente, e lui li ricorda tutti.', 11, 15, 'MEMORIES', 'LEGENDARY')
) AS v (code, name, description, attack, defense, faction, rarity)
JOIN factions f ON f.code = v.faction
JOIN rarities r ON r.code = v.rarity
JOIN card_sets s ON s.code = 'BASE';

-- Le full art: versioni senza statistiche proprie, che le leggono dalla carta base (decisione 025).
-- Codice con -FA, illustrazione verticale 2:3 (base-004-fullart.png).
INSERT INTO cards (code, base_card_id, variant_type, card_set_id, illustration_url, illustration_thumb_url)
SELECT b.code || '-FA', b.id, 'FULL_ART', b.card_set_id,
       '/images/cards/' || lower(b.code) || '-fullart-v1.webp', '/images/cards/' || lower(b.code) || '-fullart-v1-thumb.webp'
FROM cards b
WHERE b.code IN ('BASE-004', 'BASE-008', 'BASE-012', 'BASE-034', 'BASE-040', 'BASE-041', 'BASE-063', 'BASE-069', 'BASE-070', 'BASE-092', 'BASE-098', 'BASE-099');

-- Effetti: uno per carta (84 carte; le altre 15 non hanno effetti). effect_value è la X
-- del testo (es. "Riposo 4"), NULL per gli effetti senza numero.
INSERT INTO card_effects (card_id, effect_id, effect_value)
SELECT c.id, e.id, v.effect_value
FROM (VALUES
    ('BASE-001', 'BARRIER', NULL::INT),
    ('BASE-002', 'IMMUNITY', NULL),
    ('BASE-003', 'REST', 4),
    ('BASE-004', 'NEUTRALITY', NULL),
    ('BASE-005', 'LAST_BREATH', 4),
    ('BASE-006', 'VENGEANCE', 6),
    ('BASE-007', 'MIASMA', 1),
    ('BASE-008', 'WEAKENING', 4),
    ('BASE-009', 'REST', 1),
    ('BASE-010', 'PUNISHMENT', 4),
    ('BASE-011', 'PERMANENCE', 6),
    ('BASE-012', 'PERMANENCE', 7),
    ('BASE-013', 'CRADLE', NULL),
    ('BASE-014', 'REST', 1),
    ('BASE-015', 'LULLABY', 1),
    ('BASE-020', 'PERMANENCE', 3),
    ('BASE-021', 'HOPE', 4),
    ('BASE-022', 'INTENSIFICATION', 2),
    ('BASE-024', 'WEAKENING', 2),
    ('BASE-026', 'REST', 2),
    ('BASE-027', 'WEAKENING', 4),
    ('BASE-028', 'PERMANENCE', 4),
    ('BASE-029', 'HOPE', 4),
    ('BASE-030', 'LULLABY', 1),
    ('BASE-031', 'BARRIER', NULL),
    ('BASE-032', 'INTENSIFICATION', 2),
    ('BASE-033', 'REST', 2),
    ('BASE-034', 'INTENSIFICATION', 4),
    ('BASE-035', 'CRADLE', NULL),
    ('BASE-036', 'PERMANENCE', 6),
    ('BASE-037', 'IMMUNITY', NULL),
    ('BASE-038', 'HOPE', 9),
    ('BASE-039', 'WEAKENING', 5),
    ('BASE-040', 'LULLABY', 2),
    ('BASE-041', 'PERMANENCE', 6),
    ('BASE-042', 'VENGEANCE', 2),
    ('BASE-043', 'PUNISHMENT', 2),
    ('BASE-044', 'MIASMA', 1),
    ('BASE-047', 'PUNISHMENT', 2),
    ('BASE-049', 'WEAKENING', 1),
    ('BASE-050', 'VENGEANCE', 4),
    ('BASE-051', 'ABSORPTION', 1),
    ('BASE-052', 'INTENSIFICATION', 2),
    ('BASE-053', 'LAST_BREATH', 4),
    ('BASE-054', 'SCOURGE', 1),
    ('BASE-056', 'WEAKENING', 3),
    ('BASE-057', 'PUNISHMENT', 4),
    ('BASE-058', 'MIASMA', 1),
    ('BASE-059', 'INTENSIFICATION', 2),
    ('BASE-060', 'ABSORPTION', 2),
    ('BASE-061', 'SCOURGE', 1),
    ('BASE-062', 'PUNISHMENT', 4),
    ('BASE-063', 'NON_SEQUITUR', NULL),
    ('BASE-064', 'INTENSIFICATION', 2),
    ('BASE-065', 'PUNISHMENT', 6),
    ('BASE-066', 'SCOURGE', 3),
    ('BASE-067', 'WEAKENING', 4),
    ('BASE-068', 'VENGEANCE', 6),
    ('BASE-069', 'ABSORPTION', 3),
    ('BASE-070', 'MIASMA', 2),
    ('BASE-072', 'PERMANENCE', 2),
    ('BASE-073', 'MIASMA', 1),
    ('BASE-074', 'NEUTRALITY', NULL),
    ('BASE-075', 'NON_SEQUITUR', NULL),
    ('BASE-076', 'PERMANENCE', 2),
    ('BASE-079', 'LAST_BREATH', 4),
    ('BASE-080', 'REFLECTION', 2),
    ('BASE-081', 'REST', 1),
    ('BASE-084', 'REFLECTION', 2),
    ('BASE-085', 'MIASMA', 1),
    ('BASE-086', 'MIASMA', 1),
    ('BASE-087', 'PERMANENCE', 4),
    ('BASE-088', 'NEUTRALITY', NULL),
    ('BASE-089', 'NON_SEQUITUR', NULL),
    ('BASE-090', 'IMMUNITY', NULL),
    ('BASE-091', 'REST', 2),
    ('BASE-092', 'IMMUNITY', NULL),
    ('BASE-093', 'INTENSIFICATION', 2),
    ('BASE-094', 'NON_SEQUITUR', NULL),
    ('BASE-095', 'REFLECTION', 6),
    ('BASE-096', 'REST', 4),
    ('BASE-097', 'MIASMA', 2),
    ('BASE-098', 'LULLABY', 2),
    ('BASE-099', 'REFLECTION', 9)
) AS v (card_code, effect_code, effect_value)
JOIN cards c ON c.code = v.card_code
JOIN effects e ON e.code = v.effect_code;

-- I Lord delle tre fazioni: Il Sogno Lucido, L'Uomo Senza Volto, La Custode del Tempo Fermo.
UPDATE factions f
SET lord_card_id = c.id
FROM (VALUES ('DREAMS', 'BASE-004'), ('NIGHTMARES', 'BASE-008'), ('MEMORIES', 'BASE-012')) AS v (faction, card_code)
JOIN cards c ON c.code = v.card_code
WHERE f.code = v.faction;
