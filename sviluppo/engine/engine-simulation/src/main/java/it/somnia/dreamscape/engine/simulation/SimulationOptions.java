package it.somnia.dreamscape.engine.simulation;

import java.util.HashSet;
import java.util.Set;

/**
 * Le opzioni delle simulazioni: quante partite giocare e con quale seed.
 *
 * <p>Gli argomenti si controllano tutti prima di avviare le simulazioni: un'opzione senza valore o
 * sconosciuta ferma il programma invece di giocare con i valori di base e sovrascrivere il resoconto.
 *
 * @param games quante partite preparare, almeno 1
 * @param seed  il seed che decide mazzi e seed delle partite
 */
record SimulationOptions(int games, long seed) {

    /** Come si usano le opzioni, mostrato quando un argomento è sbagliato. */
    static final String USAGE = "Uso: ./gradlew :engine-simulation:run --args=\"--partite N --seed S\""
            + " (tutte e due facoltative, di base 5000 e 87)";

    private static final int DEFAULT_GAMES = 5_000;
    private static final long DEFAULT_SEED = 87;

    SimulationOptions {
        if (games < 1) {
            throw new IllegalArgumentException("Serve almeno una partita: " + games);
        }
    }

    /**
     * Legge le opzioni dagli argomenti del programma.
     *
     * @param args coppie {@code --partite N} e {@code --seed S}, in qualsiasi ordine, non {@code null}
     * @return le opzioni, con i valori di base per quelle che mancano
     * @throws IllegalArgumentException se un'opzione è sconosciuta, ripetuta o senza valore, se un
     *                                  valore non è un numero o se le partite sono meno di 1
     */
    static SimulationOptions parse(String[] args) {
        int games = DEFAULT_GAMES;
        long seed = DEFAULT_SEED;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < args.length; i += 2) {
            String name = args[i];
            if (!name.equals("--partite") && !name.equals("--seed")) {
                throw new IllegalArgumentException("Opzione sconosciuta: " + name);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Manca il valore di " + name);
            }
            if (!seen.add(name)) {
                throw new IllegalArgumentException("Opzione ripetuta: " + name);
            }
            String value = args[i + 1];
            try {
                if (name.equals("--partite")) {
                    games = Integer.parseInt(value);
                } else {
                    seed = Long.parseLong(value);
                }
            } catch (NumberFormatException notNumber) {
                throw new IllegalArgumentException("Il valore di " + name + " non è un numero intero: " + value,
                        notNumber);
            }
        }
        return new SimulationOptions(games, seed);
    }
}
