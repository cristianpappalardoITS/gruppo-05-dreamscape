package it.somnia.dreamscape.engine.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DeckSnapshotTest {

    private static final CardSnapshot LIGHTHOUSE = new CardSnapshot(
            UUID.fromString("019a1f30-0000-7000-8000-000000000003"), "Il Faro nella Nebbia", 4, 7, Faction.DREAMS, 2,
            List.of());
    private static final CardSnapshot FACELESS_MAN = new CardSnapshot(
            UUID.fromString("019a1f30-0000-7000-8000-000000000008"), "L'Uomo Senza Volto", 8, 5, Faction.NIGHTMARES,
            1, List.of(new CardEffect("WEAKENING", 2)));

    @Test
    void expandCopies_cardsWithQuantities_returnsOneCardPerCopyInOrder() {
        // Given
        DeckSnapshot deck = new DeckSnapshot(List.of(LIGHTHOUSE, FACELESS_MAN));

        // When
        List<CardSnapshot> copies = deck.expandCopies();

        // Then
        assertEquals(List.of(LIGHTHOUSE, LIGHTHOUSE, FACELESS_MAN), copies);
    }

    @Test
    void expandCopies_anyDeck_returnsUnmodifiableList() {
        // Given
        List<CardSnapshot> copies = new DeckSnapshot(List.of(LIGHTHOUSE)).expandCopies();

        // When / Then
        assertThrows(UnsupportedOperationException.class, () -> copies.add(FACELESS_MAN));
    }

    @Test
    void create_emptyDeck_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new DeckSnapshot(List.of()));
    }
}
