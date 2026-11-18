package it.somnia.dreamscape.engine.simulation;

import it.somnia.dreamscape.engine.input.CardEffect;
import it.somnia.dreamscape.engine.input.Faction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Legge il catalogo vero dalla migration {@code V102__seed_base_set.sql} del backend.
 *
 * <p>Così le simulazioni usano le stesse carte del database, senza una copia da tenere allineata.
 * Legge solo le righe delle carte normali e degli effetti: le full art hanno le statistiche
 * della carta base (decisione 025) e non cambiano le partite.
 */
final class SeedCatalogReader {

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
     * Legge le carte normali del set base con il loro effetto.
     *
     * @param migration il percorso di {@code V102__seed_base_set.sql}, non {@code null}
     * @return le carte nell'ordine della migration
     * @throws IOException              se il file non si può leggere
     * @throws IllegalArgumentException se un effetto riguarda una carta che non c'è o se una carta
     *                                  ha due effetti
     */
    static List<CatalogCard> read(Path migration) throws IOException {
        List<String> lines = Files.readAllLines(migration, StandardCharsets.UTF_8);

        Map<String, CardEffect> effects = new HashMap<>();
        for (String line : lines) {
            Matcher row = EFFECT_ROW.matcher(line);
            if (row.matches()) {
                Integer value = row.group(3).startsWith("NULL") ? null : Integer.valueOf(row.group(3));
                if (effects.put(row.group(1), new CardEffect(row.group(2), value)) != null) {
                    throw new IllegalArgumentException(row.group(1) + " ha più di un effetto");
                }
            }
        }

        List<CatalogCard> cards = new ArrayList<>();
        for (String line : lines) {
            Matcher row = CARD_ROW.matcher(line);
            if (row.matches()) {
                String code = row.group(1);
                cards.add(new CatalogCard(code, row.group(2).replace("''", "'"),
                        Integer.parseInt(row.group(3)), Integer.parseInt(row.group(4)),
                        Faction.valueOf(row.group(5)), Rarity.valueOf(row.group(6)), effects.remove(code)));
            }
        }
        if (!effects.isEmpty()) {
            throw new IllegalArgumentException("Effetti di carte che non ci sono: " + effects.keySet());
        }
        return List.copyOf(cards);
    }
}
