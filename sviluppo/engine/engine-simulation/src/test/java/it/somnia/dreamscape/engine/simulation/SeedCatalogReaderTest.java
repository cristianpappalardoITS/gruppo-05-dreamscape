package it.somnia.dreamscape.engine.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.somnia.dreamscape.engine.input.CardEffect;
import it.somnia.dreamscape.engine.input.Faction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class SeedCatalogReaderTest {

    // Il percorso della migration V102 lo passa Gradle (engine-simulation/build.gradle)
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
    void read_nameWithApostrophe_removesSqlEscape() throws IOException {
        CatalogCard card = card(readCatalog(), "BASE-090");

        assertEquals("L'Autoritratto", card.name());
        assertEquals(9, card.attack());
        assertEquals(11, card.defense());
    }
}
