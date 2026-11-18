package it.somnia.dreamscape.engine.simulation;

import it.somnia.dreamscape.engine.EndReason;
import it.somnia.dreamscape.engine.input.Faction;
import it.somnia.dreamscape.engine.input.GameRules;

import java.util.List;
import java.util.Locale;

/**
 * Compone le tabelle dei risultati in Markdown, pronte da copiare nella proposta.
 */
final class ReportWriter {

    /** La larghezza delle fasce della tabella sulla durata delle partite. */
    private static final int ROUND_BAND = 10;

    private ReportWriter() {
    }

    /**
     * Compone il resoconto delle simulazioni.
     *
     * @param statistics     le statistiche di ogni combinazione di regole, nell'ordine delle tabelle
     * @param reference      le regole di oggi (decisione 008), per la tabella delle fazioni
     * @param description    le righe che descrivono le partite giocate (numero, seed, catalogo, motore)
     * @return il testo in Markdown
     */
    static String write(List<RulesStatistics> statistics, GameRules reference, List<String> description) {
        StringBuilder text = new StringBuilder("# Simulazioni delle partite (#87)\n\n");
        for (String line : description) {
            text.append("- ").append(line).append('\n');
        }

        text.append("\n## Durata e fine delle partite\n\n")
                .append("| Punti vita | Limite | Round medi | Mediana | 90% entro | Massimo"
                        + " | Fine per punti vita | Mazzo esaurito | Limite di round | Pareggi |\n")
                .append("|---|---|---|---|---|---|---|---|---|---|\n");
        for (RulesStatistics rules : statistics) {
            text.append(String.format(Locale.ITALY, "| %d | %d | %.1f | %d | %d | %d | %s | %s | %s | %s |%n",
                    rules.rules().startingLifePoints(), rules.rules().roundLimit(), rules.averageRounds(),
                    rules.roundsPercentile(0.5), rules.roundsPercentile(0.9), rules.maxRounds(),
                    percent(rules.endReasonShare(EndReason.LIFE_POINTS)),
                    percent(rules.endReasonShare(EndReason.DECK_EXHAUSTED)),
                    percent(rules.endReasonShare(EndReason.ROUND_LIMIT)), percent(rules.drawShare())));
        }

        text.append("\n## Chi vince\n\n")
                .append("Percentuali sulle partite con un vincitore.\n\n")
                .append("| Punti vita | Limite | Vince chi vince la moneta | Vince il mazzo Forte contro il Tipico"
                        + " | Vince chi ha il vantaggio di fazione | Punti vita rimasti al vincitore |\n")
                .append("|---|---|---|---|---|---|\n");
        for (RulesStatistics rules : statistics) {
            text.append(String.format(Locale.ITALY, "| %d | %d | %s | %s | %s | %.1f |%n",
                    rules.rules().startingLifePoints(), rules.rules().roundLimit(),
                    percent(rules.coinWinnerShare()), percent(rules.strongWinShare()),
                    percent(rules.advantagedWinShare()), rules.averageWinnerLifePoints()));
        }

        int longestLimit = statistics.stream().mapToInt(rules -> rules.rules().roundLimit()).max().orElse(0);
        text.append("\n## Quanti round dura una partita\n\n")
                .append("Quota di partite che finisce in ogni fascia di round, con il limite più alto (")
                .append(longestLimit).append("), così il limite non taglia le partite lunghe.\n\n")
                .append("| Punti vita |");
        for (int from = 1; from <= longestLimit; from += ROUND_BAND) {
            text.append(' ').append(from).append('-').append(Math.min(from + ROUND_BAND - 1, longestLimit)).append(" |");
        }
        text.append('\n').append("|---|").append("---|".repeat((longestLimit + ROUND_BAND - 1) / ROUND_BAND)).append('\n');
        for (RulesStatistics rules : statistics) {
            if (rules.rules().roundLimit() != longestLimit) {
                continue;
            }
            text.append("| ").append(rules.rules().startingLifePoints()).append(" |");
            for (int from = 1; from <= longestLimit; from += ROUND_BAND) {
                long games = rules.gamesEndedBetween(from, Math.min(from + ROUND_BAND - 1, longestLimit));
                text.append(' ').append(percent((double) games / rules.games())).append(" |");
            }
            text.append('\n');
        }

        statistics.stream()
                .filter(rules -> rules.rules().equals(reference))
                .findFirst()
                .ifPresent(rules -> appendFactionTable(text, rules));
        return text.toString();
    }

    private static void appendFactionTable(StringBuilder text, RulesStatistics rules) {
        text.append("\n## Fazioni con le regole di oggi (")
                .append(rules.rules().startingLifePoints()).append(" punti vita, limite ")
                .append(rules.rules().roundLimit()).append(")\n\n")
                .append("Mazzi di una sola fazione. Quota di vittorie della fazione della riga contro quella"
                        + " della colonna, sulle partite con un vincitore.\n\n")
                .append("| Fazione |");
        for (Faction opponent : Faction.values()) {
            text.append(" contro ").append(name(opponent)).append(" |");
        }
        text.append('\n').append("|---|").append("---|".repeat(Faction.values().length)).append('\n');
        for (Faction faction : Faction.values()) {
            text.append("| ").append(name(faction)).append(" |");
            for (Faction opponent : Faction.values()) {
                text.append(' ').append(faction == opponent ? "-" : percent(rules.factionWinShare(faction, opponent)))
                        .append(" |");
            }
            text.append('\n');
        }
    }

    private static String name(Faction faction) {
        String code = faction.name();
        return code.charAt(0) + code.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String percent(double share) {
        return String.format(Locale.ITALY, "%.1f%%", share * 100);
    }
}
