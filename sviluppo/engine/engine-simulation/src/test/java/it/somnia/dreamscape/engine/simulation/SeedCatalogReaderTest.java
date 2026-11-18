package it.somnia.dreamscape.engine.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.somnia.dreamscape.engine.input.CardEffect;
import it.somnia.dreamscape.engine.input.Faction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeedCatalogReaderTest {

    // La cartella delle migration del backend la passa Gradle (engine-simulation/build.gradle)
    private static List<CatalogCard> readCatalog() throws IOException {
        return SeedCatalogReader.read(Path.of(System.getProperty("dreamscape.catalog")));
    }

    private static CatalogCard card(List<CatalogCard> catalog, String code) {
        return catalog.stream().filter(card -> card.code().equals(code)).findFirst().orElseThrow();
    }

    @Test
    void read_baseSet_finds99CardsAnd33PerFaction() throws IOException {
        List<CatalogCard> catalog = readCatalog();

        assertEquals(99, catalog.size());
        for (Faction faction : Faction.values()) {
            assertEquals(33, catalog.stream().filter(card -> card.faction() == faction).count());
        }
    }

    @Test
    void read_baseSet_finds84CardsWithEffect() throws IOException {
        assertEquals(84, readCatalog().stream().filter(card -> card.effect() != null).count());
    }

    @Test
    void read_cardWithValue_keepsCodeAndValue() throws IOException {
        CatalogCard card = card(readCatalog(), "BASE-003");

        assertEquals(new CardEffect("REST", 4), card.effect());
        assertEquals(Rarity.EPIC, card.rarity());
    }

    @Test
    void read_effectWithoutValue_hasNullValue() throws IOException {
        assertNull(card(readCatalog(), "BASE-001").effect().value());
    }

    @Test
    void read_campanile_usesValueOfV103() throws IOException {
        assertEquals(new CardEffect("PERMANENCE", 5), card(readCatalog(), "BASE-036").effect());
    }

    @Test
    void read_laterMigration_replacesEffectAndIgnoresOlderVersions(@TempDir Path migrations) throws IOException {
        // V10 prima di V100: con l'ordine per nome verrebbe dopo, e non è una migration del catalogo
        Files.writeString(migrations.resolve("V10__create_leagues.sql"), "    ('BASE-001', 'REST', 9),\n");
        Files.writeString(migrations.resolve("V100__seed.sql"), String.join("\n",
                "    ('BASE-001', 'Il Rifugio Sicuro', 'L''ultimo posto.', 5, 13, 'DREAMS', 'COMMON'),",
                "    ('BASE-001', 'PERMANENCE', 6),"));
        Files.writeString(migrations.resolve("V101__update.sql"), "    ('BASE-001', 'PERMANENCE', 5)\n");

        assertEquals(new CardEffect("PERMANENCE", 5), SeedCatalogReader.read(migrations).getFirst().effect());
    }

    @Test
    void read_twoEffectsInSameMigration_throws(@TempDir Path migrations) throws IOException {
        Files.writeString(migrations.resolve("V100__seed.sql"), String.join("\n",
                "    ('BASE-001', 'Il Rifugio Sicuro', 'Descrizione.', 5, 13, 'DREAMS', 'COMMON'),",
                "    ('BASE-001', 'BARRIER', NULL::INT),",
                "    ('BASE-001', 'REST', 2)"));

        assertThrows(IllegalArgumentException.class, () -> SeedCatalogReader.read(migrations));
    }

    @Test
    void read_nameWithApostrophe_removesSqlEscape() throws IOException {
        CatalogCard card = card(readCatalog(), "BASE-090");

        assertEquals("L'Autoritratto", card.name());
        assertEquals(9, card.attack());
        assertEquals(11, card.defense());
    }
}
