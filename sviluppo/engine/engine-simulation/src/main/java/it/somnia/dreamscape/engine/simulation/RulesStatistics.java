package it.somnia.dreamscape.engine.simulation;

import it.somnia.dreamscape.engine.BattleResult;
import it.somnia.dreamscape.engine.EndReason;
import it.somnia.dreamscape.engine.Side;
import it.somnia.dreamscape.engine.input.Faction;
import it.somnia.dreamscape.engine.input.GameRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Raccoglie i risultati delle partite giocate con una combinazione di regole.
 *
 * <p>Le percentuali su chi vince (moneta, mazzo forte, vantaggio di fazione) contano solo le
 * partite con un vincitore: i pareggi hanno la loro colonna.
 */
final class RulesStatistics {

    private final GameRules rules;
    private final List<Integer> rounds = new ArrayList<>();
    private final int[] endReasons = new int[EndReason.values().length];
    private int draws;
    private int decided;
    private int coinWinnerWins;
    private int strongVsTypicalDecided;
    private int strongWins;
    private int advantageDecided;
    private int advantagedWins;
    private long winnerLifePoints;
    private final int[][] factionGames = new int[Faction.values().length][Faction.values().length];
    private final int[][] factionWins = new int[Faction.values().length][Faction.values().length];

    /**
     * Crea le statistiche vuote di una combinazione di regole.
     *
     * @param rules le regole delle partite, non {@code null}
     */
    RulesStatistics(GameRules rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    GameRules rules() {
        return rules;
    }

    /**
     * Aggiunge il risultato di una partita.
     *
     * @param game   la partita giocata, non {@code null}
     * @param result il suo risultato, non {@code null}
     */
    void record(SimulatedGame game, BattleResult result) {
        rounds.add(result.totalRounds());
        endReasons[result.endReason().ordinal()]++;
        if (result.isDraw()) {
            draws++;
            return;
        }
        decided++;
        Side winner = result.winner();
        if (winner == result.coinTossWinner()) {
            coinWinnerWins++;
        }
        winnerLifePoints += winner == Side.CHALLENGER ? result.challengerLifePoints() : result.opponentLifePoints();

        if (game.challengerProfile() != game.opponentProfile()) {
            strongVsTypicalDecided++;
            Side strongSide = game.challengerProfile() == DeckProfile.STRONG ? Side.CHALLENGER : Side.OPPONENT;
            if (winner == strongSide) {
                strongWins++;
            }
        }

        if (game.isFactionMatchup()) {
            Faction challenger = game.challengerFaction();
            Faction opponent = game.opponentFaction();
            Faction winnerFaction = winner == Side.CHALLENGER ? challenger : opponent;
            factionGames[challenger.ordinal()][opponent.ordinal()]++;
            factionGames[opponent.ordinal()][challenger.ordinal()]++;
            Faction loserFaction = winnerFaction == challenger ? opponent : challenger;
            factionWins[winnerFaction.ordinal()][loserFaction.ordinal()]++;

            // Nel giro dei vantaggi, tra due fazioni diverse una sola ha il +1 sull'altra
            Faction advantaged = rules.attackBonus(challenger, opponent) > 0 ? challenger
                    : rules.attackBonus(opponent, challenger) > 0 ? opponent : null;
            if (advantaged != null) {
                advantageDecided++;
                if (winnerFaction == advantaged) {
                    advantagedWins++;
                }
            }
        }
    }

    int games() {
        return rounds.size();
    }

    double averageRounds() {
        return rounds.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    /**
     * Restituisce il numero di round entro cui finisce la quota indicata delle partite.
     *
     * @param share la quota, tra 0 e 1 (0,5 è la mediana)
     * @return il round, oppure 0 se non ci sono partite
     */
    int roundsPercentile(double share) {
        if (rounds.isEmpty()) {
            return 0;
        }
        List<Integer> sorted = new ArrayList<>(rounds);
        Collections.sort(sorted);
        int index = (int) Math.ceil(share * sorted.size()) - 1;
        return sorted.get(Math.clamp(index, 0, sorted.size() - 1));
    }

    int maxRounds() {
        return rounds.stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /**
     * Restituisce quante partite sono finite con un numero di round compreso tra i due valori.
     *
     * @param from il primo round, incluso
     * @param to   l'ultimo round, incluso
     * @return il numero di partite
     */
    long gamesEndedBetween(int from, int to) {
        return rounds.stream().filter(round -> round >= from && round <= to).count();
    }

    double endReasonShare(EndReason reason) {
        return share(endReasons[reason.ordinal()], games());
    }

    double drawShare() {
        return share(draws, games());
    }

    double coinWinnerShare() {
        return share(coinWinnerWins, decided);
    }

    double strongWinShare() {
        return share(strongWins, strongVsTypicalDecided);
    }

    double advantagedWinShare() {
        return share(advantagedWins, advantageDecided);
    }

    double averageWinnerLifePoints() {
        return decided == 0 ? 0 : (double) winnerLifePoints / decided;
    }

    /**
     * Restituisce la quota di vittorie di una fazione contro un'altra, tra mazzi di una sola fazione.
     *
     * @param faction  la fazione di cui si contano le vittorie
     * @param opponent la fazione avversaria, diversa da {@code faction}
     * @return la quota tra 0 e 1 sulle partite con un vincitore
     */
    double factionWinShare(Faction faction, Faction opponent) {
        return share(factionWins[faction.ordinal()][opponent.ordinal()],
                factionGames[faction.ordinal()][opponent.ordinal()]);
    }

    private static double share(long part, long total) {
        return total == 0 ? 0 : (double) part / total;
    }
}
