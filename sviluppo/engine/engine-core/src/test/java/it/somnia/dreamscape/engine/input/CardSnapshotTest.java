package it.somnia.dreamscape.engine.input;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CardSnapshotTest {

    private static final UUID CARD_ID = UUID.fromString("019a1f30-0000-7000-8000-000000000003");
    private static final CardEffect PERMANENCE = new CardEffect("PERMANENCE", 1);

    @Test
    void create_validCardWithOneEffect_succeeds() {
        // When / Then
        assertDoesNotThrow(() -> new CardSnapshot(CARD_ID, "Il Faro nella Nebbia", 4, 7, Faction.DREAMS, 2,
                List.of(PERMANENCE)));
    }

    @Test
    void create_twoEffects_throwsIllegalArgumentException() {
        // Given
        List<CardEffect> twoEffects = List.of(PERMANENCE, new CardEffect("NEUTRALITY", null));

        // When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new CardSnapshot(CARD_ID, "Il Faro nella Nebbia", 4, 7, Faction.DREAMS, 1, twoEffects));
    }

    @Test
    void create_negativeAttack_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new CardSnapshot(CARD_ID, "Il Faro nella Nebbia", -1, 7, Faction.DREAMS, 1, List.of()));
    }

    @Test
    void create_zeroDefense_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new CardSnapshot(CARD_ID, "Il Faro nella Nebbia", 4, 0, Faction.DREAMS, 1, List.of()));
    }

    @Test
    void create_zeroQuantity_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new CardSnapshot(CARD_ID, "Il Faro nella Nebbia", 4, 7, Faction.DREAMS, 0, List.of()));
    }

    @Test
    void create_blankEffectCode_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new CardEffect(" ", 1));
    }

    @Test
    void create_negativeEffectValue_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new CardEffect("PUNISHMENT", -2));
    }
}
