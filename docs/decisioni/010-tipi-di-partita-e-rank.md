# 010 · Partite classificate e rapide, matchmaking e rank

- **Data:** 2026-11-03
- **Stato:** approvata, sostituisce "sfide aperte a tutti" del 26/10 ([009](009-amici-e-scambi.md))
- **Referenti:** Marco
- **Task:** issue da aprire per matchmaking e rank

## Contesto
Fino al 2/11 le sfide erano aperte a tutti, con una sola modalità, e il rank era previsto dopo le funzionalità obbligatorie. Il rischio: due amici che si sfidano all'infinito per prendere crediti, e nessun modo di trovare avversari del proprio livello.

## Opzioni valutate
1. Partita rapida solo tra amici, oppure anche con gli altri.
2. Matchmaking asincrono (l'avversario non deve essere collegato, gioca con il suo mazzo classificato), oppure coda in tempo reale.
3. Rank Elo con K = 32, con o senza limite di partite contro lo stesso avversario.
4. Nella classificata, matchmaking che esclude gli amici, oppure che li ammette senza crediti.

## Decisione
- Regole sempre della Base ([008](008-regole-della-partita.md)). Cambia solo il **tipo di partita**:

| Tipo | Avversario | Rank | Dream Credits |
|------|------------|------|---------------|
| **Classificata** | Scelto dal matchmaking con punteggio simile | Sì | 30 DC per vittoria, massimo 3 al giorno; **niente DC se l'avversario è un amico** |
| **Rapida** | Scelto a caso dal matchmaking | No | No |
| **Sfida a un amico** | Un amico, che accetta (è una partita rapida) | No | No |

- **Matchmaking asincrono:** chi preme "Cerca partita" viene abbinato subito. L'avversario non deve essere collegato: gioca con il suo **mazzo classificato**, scelto nella pagina Mazzi.
- **Rank Elo** con K = 32, si parte da 1000, in **fase 1**. Lo stesso avversario dà punti e DC al massimo **3 volte al giorno** (giorno da mezzanotte, ora italiana).
- Le sfide dirette si mandano **solo agli amici**, al massimo una in attesa per coppia.

## Motivazione
Con gli amici si gioca liberamente senza poter accumulare crediti. Il matchmaking asincrono funziona anche con pochi giocatori e si adatta alla partita simulata dal server. Il limite per avversario serve perché con pochi giocatori il matchmaking ripropone spesso le stesse persone.

## Conseguenze
- Database: `matches.match_type` (`CASUAL`, `RANKED`), `challenger_id`, `opponent_id`, `challenge_id` facoltativo, `users.ranked_deck_id`, al massimo una sfida in attesa per coppia ([schema.md](../database/schema.md#partite)).
- Pagine nuove: Gioca (classificata e rapida), Classifica; Sfide solo tra amici. Il SiteMap lo aggiorna Alessandro.
- Da aggiornare: proposta di regolamento, slide 12, issue delle sfide e del rank.
- Il "matchmaking asincrono" nel budget di Digital è corretto.
- Ancora aperti: se il giocatore scelto come avversario riceve i DC quando vince (proposta: no) e la distanza massima di punteggio (proposta: ±100, poi si allarga).
