package it.somnia.dreamscape.engine.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class GameRulesTest {

    // Il ciclo del regolamento: Dreams → Memories → Nightmares → Dreams, +1 attacco (decisione 008)
    private static final List<FactionAdvantage> CYCLE = List.of(
            new FactionAdvantage(Faction.DREAMS, Faction.MEMORIES, 1, 0),
            new FactionAdvantage(Faction.MEMORIES, Faction.NIGHTMARES, 1, 0),
            new FactionAdvantage(Faction.NIGHTMARES, Faction.DREAMS, 1, 0));

    private final GameRules rules = new GameRules(20, 60, CYCLE);

    @Test
    void attackBonus_attackerHasAdvantage_returnsBonus() {
        // When / Then
        assertEquals(1, rules.attackBonus(Faction.NIGHTMARES, Faction.DREAMS));
    }

    @Test
    void attackBonus_attackerIsWeak_returnsZero() {
        // When / Then
        assertEquals(0, rules.attackBonus(Faction.DREAMS, Faction.NIGHTMARES));
    }

    @Test
    void attackBonus_sameFaction_returnsZero() {
        // When / Then
        assertEquals(0, rules.attackBonus(Faction.MEMORIES, Faction.MEMORIES));
    }

    @Test
    void create_repeatedPair_throwsIllegalArgumentException() {
        // Given
        List<FactionAdvantage> repeated = List.of(
                new FactionAdvantage(Faction.DREAMS, Faction.MEMORIES, 1, 0),
                new FactionAdvantage(Faction.DREAMS, Faction.MEMORIES, 2, 0));

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new GameRules(20, 60, repeated));
    }

    @Test
    void create_defenseBonus_throwsIllegalArgumentException() {
        // Given
        List<FactionAdvantage> withDefense = List.of(new FactionAdvantage(Faction.DREAMS, Faction.MEMORIES, 1, 1));

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new GameRules(20, 60, withDefense));
    }

    @Test
    void create_zeroLifePoints_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new GameRules(0, 60, CYCLE));
    }

    @Test
    void create_zeroRoundLimit_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new GameRules(20, 0, CYCLE));
    }

    @Test
    void create_advantageOnSameFaction_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new FactionAdvantage(Faction.DREAMS, Faction.DREAMS, 1, 0));
    }
}
