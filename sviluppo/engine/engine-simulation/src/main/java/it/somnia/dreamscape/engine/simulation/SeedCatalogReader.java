package it.somnia.dreamscape.engine.simulation;

import it.somnia.dreamscape.engine.input.CardEffect;
import it.somnia.dreamscape.engine.input.Faction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Legge il catalogo vero dalle migration del backend: {@code V102__seed_base_set.sql} e le successive.
 *
 * <p>Così le simulazioni usano le stesse carte del database, senza una copia da tenere allineata.
 * Legge le migration del catalogo ({@code V100} e successive) in ordine di versione e capisce due tipi
 * di righe, quelle delle carte normali e quelle degli effetti, scritte come in V102. Una riga di un
 * effetto in una migration successiva sostituisce quella precedente della stessa carta (per esempio
 * il Campanile di V103). Le full art hanno le statistiche della carta base (decisione 025) e non
 * cambiano le partite.
 */
final class SeedCatalogReader {

    /** La prima versione delle migration con i dati del catalogo vero (regole-codice.md). */
    private static final int FIRST_CATALOG_VERSION = 100;

    private static final Pattern MIGRATION_FILE = Pattern.compile("^V(\\d+)__.+\\.sql$");

    // ('BASE-001', 'Il Rifugio Sicuro', 'Descrizione...', 5, 13, 'DREAMS', 'COMMON'),
    // Negli apostrofi SQL un ' dentro il testo è scritto ''
    private static final Pattern CARD_ROW = Pattern.compile(
            "^\\s*\\('(BASE-\\d{3})', '((?:[^']|'')*)', '(?:[^']|'')*', (\\d+), (\\d+), '([A-Z]+)', '([A-Z]+)'\\),?\\s*$");

    // ('BASE-003', 'REST', 4),   oppure   ('BASE-001', 'BARRIER', NULL::INT),
    private static final Pattern EFFECT_ROW = Pattern.compile(
            "^\\s*\\('(BASE-\\d{3})', '([A-Z_]+)', (NULL(?:::INT)?|\\d+)\\),?\\s*$");

    private SeedCatalogReader() {
    }

    /**
     * Legge le carte normali del catalogo con il loro effetto, come sono dopo tutte le migration.
     *
     * @param migrations la cartella {@code db/migration} del backend, non {@code null}
     * @return le carte nell'ordine in cui le migration le creano
     * @throws IOException              se la cartella o un file non si possono leggere
     * @throws IllegalArgumentException se una carta compare due volte, se una migration dà due effetti
     *                                  alla stessa carta o se un effetto riguarda una carta che non c'è
     */
    static List<CatalogCard> read(Path migrations) throws IOException {
        Map<String, String[]> cardRows = new LinkedHashMap<>();
        Map<String, CardEffect> effects = new LinkedHashMap<>();

        for (Path migration : catalogMigrations(migrations)) {
            Set<String> effectsInThisFile = new HashSet<>();
            for (String line : Files.readAllLines(migration, StandardCharsets.UTF_8)) {
                Matcher card = CARD_ROW.matcher(line);
                if (card.matches()) {
                    String code = card.group(1);
                    if (cardRows.put(code, new String[] {card.group(2), card.group(3), card.group(4),
                            card.group(5), card.group(6)}) != null) {
                        throw new IllegalArgumentException(code + " è creata due volte");
                    }
                    continue;
                }
                Matcher effect = EFFECT_ROW.matcher(line);
                if (effect.matches()) {
                    String code = effect.group(1);
                    if (!effectsInThisFile.add(code)) {
                        throw new IllegalArgumentException(code + " ha più di un effetto in " + migration.getFileName());
                    }
                    Integer value = effect.group(3).startsWith("NULL") ? null : Integer.valueOf(effect.group(3));
                    effects.put(code, new CardEffect(effect.group(2), value));
                }
            }
        }

        List<CatalogCard> cards = new ArrayList<>();
        cardRows.forEach((code, row) -> cards.add(new CatalogCard(code, row[0].replace("''", "'"),
                Integer.parseInt(row[1]), Integer.parseInt(row[2]), Faction.valueOf(row[3]), Rarity.valueOf(row[4]),
                effects.remove(code))));
        if (!effects.isEmpty()) {
            throw new IllegalArgumentException("Effetti di carte che non ci sono: " + effects.keySet());
        }
        return List.copyOf(cards);
    }

    /**
     * Restituisce le migration del catalogo in ordine di versione: V100, V101, V102, V103...
     *
     * @param migrations la cartella delle migration
     * @return i file dalla versione {@link #FIRST_CATALOG_VERSION} in poi
     * @throws IOException se la cartella non si può leggere
     */
    private static List<Path> catalogMigrations(Path migrations) throws IOException {
        try (Stream<Path> files = Files.list(migrations)) {
            // L'ordine per nome metterebbe V10 prima di V100 e V2 dopo V102: si ordina per numero
            return files.filter(file -> version(file) >= FIRST_CATALOG_VERSION)
                    .sorted(Comparator.comparingInt(SeedCatalogReader::version))
                    .toList();
        }
    }

    private static int version(Path file) {
        Matcher name = MIGRATION_FILE.matcher(file.getFileName().toString());
        return name.matches() ? Integer.parseInt(name.group(1)) : -1;
    }
}
