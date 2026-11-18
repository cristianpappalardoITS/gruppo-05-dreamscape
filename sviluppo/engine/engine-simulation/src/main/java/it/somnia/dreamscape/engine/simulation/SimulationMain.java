package it.somnia.dreamscape.engine.simulation;

import it.somnia.dreamscape.engine.BattleResult;
import it.somnia.dreamscape.engine.battle.BattleEngine;
import it.somnia.dreamscape.engine.input.Faction;
import it.somnia.dreamscape.engine.input.FactionAdvantage;
import it.somnia.dreamscape.engine.input.GameRules;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gioca migliaia di partite con il catalogo vero e confronta punti vita e limiti di round (#87).
 *
 * <p>Prepara una volta sola le partite (due mazzi a caso e un seed) e le gioca con ogni combinazione
 * di regole. Il resoconto in Markdown va in {@code build/simulazioni/risultati.md} e sullo schermo.
 * Si avvia con {@code ./gradlew :engine-simulation:run}; opzioni: {@code --partite N} e {@code --seed S}.
 */
public final class SimulationMain {

    /** I punti vita provati: 20 è il valore di oggi (decisione 008). */
    private static final int[] LIFE_POINTS = {10, 15, 20, 25, 30};

    /** I limiti di round provati: 60 è il valore di oggi (decisione 008). */
    private static final int[] ROUND_LIMITS = {30, 60, 90};

    /** Il giro dei vantaggi di V100 (decisione 008): Dreams > Memories > Nightmares > Dreams, +1 ATT. */
    private static final List<FactionAdvantage> ADVANTAGES = List.of(
            new FactionAdvantage(Faction.DREAMS, Faction.MEMORIES, 1, 0),
            new FactionAdvantage(Faction.MEMORIES, Faction.NIGHTMARES, 1, 0),
            new FactionAdvantage(Faction.NIGHTMARES, Faction.DREAMS, 1, 0));

    private static final GameRules CURRENT_RULES = new GameRules(20, 60, ADVANTAGES);

    private SimulationMain() {
    }

    /**
     * Avvia le simulazioni.
     *
     * <p>Con argomenti sbagliati scrive l'errore ed esce con il codice 2, senza giocare.
     *
     * @param args {@code --partite N} per il numero di partite, {@code --seed S} per il seed
     * @throws IOException se non si legge il catalogo o non si scrive il resoconto
     */
    public static void main(String[] args) throws IOException {
        SimulationOptions options;
        try {
            options = SimulationOptions.parse(args);
        } catch (IllegalArgumentException wrongArgs) {
            // Ci si ferma prima di giocare: il resoconto della volta prima resta com'era
            System.err.println(wrongArgs.getMessage());
            System.err.println(SimulationOptions.USAGE);
            System.exit(2);
            return;
        }
        int gameCount = options.games();
        long seed = options.seed();
        Path catalogDir = Path.of(requiredProperty("dreamscape.catalog"));
        Path outputFile = Path.of(requiredProperty("dreamscape.output"));

        List<CatalogCard> catalog = SeedCatalogReader.read(catalogDir);
        List<SimulatedGame> games = prepareGames(new DeckBuilder(catalog), gameCount, seed);

        BattleEngine engine = new BattleEngine();
        List<RulesStatistics> statistics = new ArrayList<>();
        long start = System.nanoTime();
        for (int lifePoints : LIFE_POINTS) {
            for (int roundLimit : ROUND_LIMITS) {
                RulesStatistics rules = new RulesStatistics(new GameRules(lifePoints, roundLimit, ADVANTAGES));
                for (SimulatedGame game : games) {
                    BattleResult result = engine.simulate(game.challengerDeck(), game.opponentDeck(),
                            rules.rules(), game.seed(), false);
                    rules.record(game, result);
                }
                statistics.add(rules);
                System.out.printf("Punti vita %d, limite %d: fatto%n", lifePoints, roundLimit);
            }
        }
        long seconds = (System.nanoTime() - start) / 1_000_000_000L;

        List<String> description = List.of(
                String.format("%d partite per combinazione, %d combinazioni: %d partite in %d secondi",
                        gameCount, statistics.size(), (long) gameCount * statistics.size(), seconds),
                "Le stesse partite (mazzi e seed) per tutte le combinazioni; seed iniziale " + seed,
                "Catalogo: " + catalog.size() + " carte dalle migration del backend (V102 e successive)"
                        + "; motore " + BattleEngine.ENGINE_VERSION + "; vantaggio di fazione +1 ATT",
                "Mazzi di 20 carte: Tipico (11 Comuni, 6 Rare, 2 Epiche, 1 Leggendaria) o Forte"
                        + " (6, 8, 4, 2), di una sola fazione o misti, scelti a caso con la stessa probabilità");
        String report = ReportWriter.write(statistics, CURRENT_RULES, description);

        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, report, StandardCharsets.UTF_8);
        System.out.println();
        System.out.println(report);
        System.out.println("Resoconto salvato in " + outputFile);
    }

    /**
     * Prepara le partite: per ogni lato un profilo e una fazione a caso (o un mazzo misto), poi il seed.
     *
     * @param builder   il compositore dei mazzi, non {@code null}
     * @param gameCount quante partite preparare, almeno 1
     * @param seed      il seed che decide mazzi e seed delle partite
     * @return le partite, sempre le stesse con lo stesso seed
     */
    static List<SimulatedGame> prepareGames(DeckBuilder builder, int gameCount, long seed) {
        if (gameCount < 1) {
            throw new IllegalArgumentException("Serve almeno una partita: " + gameCount);
        }
        Random random = new Random(seed);
        DeckProfile[] profiles = DeckProfile.values();
        Faction[] factions = Faction.values();
        List<SimulatedGame> games = new ArrayList<>(gameCount);
        for (int i = 0; i < gameCount; i++) {
            DeckProfile challengerProfile = profiles[random.nextInt(profiles.length)];
            // Un valore in più delle fazioni: l'ultimo vuol dire mazzo misto
            int challengerChoice = random.nextInt(factions.length + 1);
            Faction challengerFaction = challengerChoice < factions.length ? factions[challengerChoice] : null;
            DeckProfile opponentProfile = profiles[random.nextInt(profiles.length)];
            int opponentChoice = random.nextInt(factions.length + 1);
            Faction opponentFaction = opponentChoice < factions.length ? factions[opponentChoice] : null;

            games.add(new SimulatedGame(
                    builder.build(challengerProfile, challengerFaction, random), challengerProfile, challengerFaction,
                    builder.build(opponentProfile, opponentFaction, random), opponentProfile, opponentFaction,
                    random.nextLong()));
        }
        return games;
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Manca la proprietà " + name + ": avvia con ./gradlew :engine-simulation:run");
        }
        return value;
    }
}
