package it.somnia.dreamscape.engine.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.Faction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class DeckBuilderTest {

    private static List<CatalogCard> readCatalog() throws IOException {
        return SeedCatalogReader.read(Path.of(System.getProperty("dreamscape.catalog")));
    }

    @Test
    void build_anyProfile_respectsDeckRules() throws IOException {
        List<CatalogCard> catalog = readCatalog();
        Map<String, CatalogCard> byName = catalog.stream()
                .collect(Collectors.toMap(CatalogCard::name, Function.identity()));
        DeckBuilder builder = new DeckBuilder(catalog);
        Random random = new Random(1);

        for (DeckProfile profile : DeckProfile.values()) {
            for (int i = 0; i < 200; i++) {
                DeckSnapshot deck = builder.build(profile, null, random);

                assertEquals(DeckProfile.DECK_SIZE, deck.expandCopies().size());
                for (Rarity rarity : Rarity.values()) {
                    int count = 0;
                    for (CardSnapshot card : deck.cards()) {
                        if (byName.get(card.name()).rarity() == rarity) {
                            assertTrue(card.quantity() <= rarity.maxCopies(), card.name());
                            count += card.quantity();
                        }
                    }
                    assertEquals(profile.count(rarity), count);
                }
            }
        }
    }

    @Test
    void build_withFaction_usesOnlyThatFaction() throws IOException {
        DeckSnapshot deck = new DeckBuilder(readCatalog()).build(DeckProfile.STRONG, Faction.NIGHTMARES, new Random(2));

        assertTrue(deck.cards().stream().allMatch(card -> card.faction() == Faction.NIGHTMARES));
    }

    @Test
    void build_sameSeed_givesSameDeck() throws IOException {
        DeckBuilder builder = new DeckBuilder(readCatalog());

        assertEquals(builder.build(DeckProfile.TYPICAL, null, new Random(3)),
                builder.build(DeckProfile.TYPICAL, null, new Random(3)));
    }
}
