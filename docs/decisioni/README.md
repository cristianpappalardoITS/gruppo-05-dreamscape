# Decisioni del gruppo

Qui c'è **cosa** si è deciso e **perché**, un file per tema, con il [template](../template.md#template-decisione-docsdecisioninnn-titolomd). Le decisioni le registrano Marco o Cristian, su comunicazione dei referenti ([documentazione.md](../documentazione.md)).

Se una decisione cambia, non si riscrive il file vecchio: si crea un file nuovo e quello vecchio diventa "sostituita da NNN".

| N. | Tema | Data | Stato |
|----|------|------|-------|
| [001](001-tema-e-nomi.md) | Tema e nomi: Dreamscape, Somnia, Dreamweaver, fazioni in inglese | 26/10, 2/11 | Approvata |
| [002](002-database-postgresql-uuid.md) | Database: PostgreSQL 18 e chiavi UUID v7 | 26/10 | Approvata |
| [003](003-accesso-jwt.md) | Accesso con JWT, token di 15 minuti e 7 giorni | 26/10, 2/11 | Approvata |
| [004](004-frontend-senza-framework.md) | Frontend in HTML, CSS e JavaScript senza framework | 26/10 | Sostituita da 005 |
| [005](005-frontend-componenti-react.md) | Frontend: pagine HTML e componenti React | 2/11, 3/11 | Approvata |
| [006](006-avvio-locale-e-dati.md) | Sito e API sullo stesso indirizzo, niente dati finti | 2/11 | Approvata |
| [007](007-montaggio-della-carta.md) | Carta montata dal frontend, cornici in JavaScript, immagini WebP dal backend | 26/10–3/11 | Approvata |
| [008](008-regole-della-partita.md) | Regole della partita: una modalità Base con effetti, 20 punti vita, vantaggio di fazione | 26/10–2/11 | Approvata (effetti: vedi 028) |
| [009](009-amici-e-scambi.md) | Amici e scambi | 26/10–3/11 | Approvata (sfide: vedi 010; carte bloccate: vedi 024; amicizia tolta: vedi 026) |
| [010](010-tipi-di-partita-e-rank.md) | Partite classificate e rapide, matchmaking e rank | 3/11 | Approvata |
| [011](011-premi-di-lega.md) | Leghe e premi di lega | 3/11 | Approvata, importi da proporre |
| [012](012-pacchetti.md) | Pacchetti: Dream Pack, apertura subito, più tipi | 31/10–3/11 | Approvata, nuovi tipi da decidere |
| [013](013-crediti-e-ricompense.md) | Dream Credits e ricompense | 31/10–3/11 | Approvata |
| [014](014-dotazione-iniziale.md) | Dotazione iniziale: 500 DC, set di una fazione, benvenuto | 2/11, 3/11 | Approvata |
| [015](015-missioni.md) | Missione giornaliera e obiettivo settimanale | 2/11, 3/11 | Approvata |
| [016](016-catalogo-fase-1.md) | Catalogo della fase 1: 99 o 102 carte | 2/11, 3/11 | Approvata |
| [017](017-mazzi.md) | Mazzi: solo validi, mazzo classificato | 26/10–3/11 | Approvata (full art: vedi 025; carte disattivate: vedi 026) |
| [018](018-utenti-e-privacy.md) | Utenti e privacy | 3/11 | Approvata |
| [019](019-consegne-di-digital.md) | Consegne di Digital sul Drive | 2/11, 3/11 | Approvata |
| [020](020-flusso-github.md) | Flusso su GitHub: `develop` ed `.editorconfig` | 2/11, 3/11 | Approvata |
| [021](021-skin-e-animazioni.md) | Skin e animazioni nel Backlog | 3/11 | Approvata (skin delle carte: vedi 025) |
| [022](022-risposte-del-cliente.md) | Risposte del cliente al colloquio del 2/11 | 2/11 | Approvata |
| [023](023-buyer-persona.md) | Una sola buyer persona | 2/11 | Approvata |
| [024](024-copie-bloccate-negli-scambi.md) | Copie offerte bloccate negli altri scambi, avviso sui mazzi | 4/11 | Approvata, sostituisce una parte di 009 |
| [025](025-versioni-delle-carte.md) | Versioni delle carte: la full art è una carta con il suo id | 4/11 | Approvata, sostituisce una parte di 017 |
| [026](026-carte-disattivate-e-amicizie-tolte.md) | Carte disattivate fuori dai mazzi, scambi e sfide annullati se si toglie l'amicizia | 4/11 | Approvata |
| [027](027-effetto-non-sequitur.md) | Effetto "Non Sequitur" senza valore | 4/11 | Approvata (catalogo: vedi 028) |
| [028](028-catalogo-degli-effetti.md) | Catalogo degli effetti: 19 effetti, versione provvisoria | 4/11 | Approvata come provvisoria: da bilanciare, effetti da aggiungere o togliere in corso d'opera |
| [029](029-motore-di-gioco.md) | Motore di gioco: microservizio in `sviluppo/engine`, seed, log a eventi | 4/11 | Approvata; regole del round da confermare |

## Ancora da decidere

| Cosa | Chi | Dove |
|------|-----|------|
| Tipi, prezzi e pesi dei nuovi pacchetti | Maie, Nicolas | [012](012-pacchetti.md) |
| Numero esatto di carte: 99 o 102 | Nicolas, Maie | [016](016-catalogo-fase-1.md) |
| Soglie e premi delle leghe | Maie | [011](011-premi-di-lega.md) |
| DC a chi viene scelto dal matchmaking e vince | Maie, SWD | [010](010-tipi-di-partita-e-rank.md) |
| Partite che contano per le missioni | Maie, Nicolas | [015](015-missioni.md) |
| Punti vita dopo le simulazioni | Marco, Maie | [008](008-regole-della-partita.md) |
| Bilanciamento degli effetti: valori di X, effetti da aggiungere o togliere | Nicolas, Maie | [028](028-catalogo-degli-effetti.md) |
| Regole del round lasciate aperte dal regolamento (punti a–f) | Nicolas, Maie | [029](029-motore-di-gioco.md) |
| Effetti unici per i tre Lord | Tutto il gruppo | [028](028-catalogo-degli-effetti.md) (cambierebbe [001](001-tema-e-nomi.md)) |

Le proposte di partenza sono nei [punti aperti dello schema](../database/schema.md#punti-aperti).
