package it.somnia.dreamscape.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.somnia.dreamscape.engine.battle.BattleEngine;
import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.Faction;
import it.somnia.dreamscape.engine.input.GameRules;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SimulationRequestTest {

    private static final GameRules RULES = new GameRules(20, 60, List.of());

    @Test
    void constructor_withoutSeed_throws() {
        // When / Then
        assertThrows(NullPointerException.class,
                () -> new SimulationRequest(deck("A", 4, 5), deck("B", 4, 5), RULES, null, true));
    }

    @Test
    void constructor_withoutDeck_throws() {
        // When / Then
        assertThrows(NullPointerException.class,
                () -> new SimulationRequest(null, deck("B", 4, 5), RULES, 42L, true));
    }

    @Test
    void simulate_request_playsTheSameMatchAsTheSingleValues() {
        // Given
        BattleEngine engine = new BattleEngine();
        DeckSnapshot challenger = deck("A", 6, 5);
        DeckSnapshot opponent = deck("B", 4, 5);

        // When
        BattleResult fromRequest = engine.simulate(new SimulationRequest(challenger, opponent, RULES, 42L, true));
        BattleResult fromValues = engine.simulate(challenger, opponent, RULES, 42L, true);

        // Then
        assertEquals(fromValues, fromRequest);
    }

    @Test
    void simulate_requestWithoutLog_returnsNoLog() {
        // When
        BattleResult result = new BattleEngine()
                .simulate(new SimulationRequest(deck("A", 6, 5), deck("B", 4, 5), RULES, 42L, false));

        // Then
        assertNull(result.log());
    }

    // Un mazzo di 20 copie della stessa carta senza effetti
    private static DeckSnapshot deck(String name, int attack, int defense) {
        return new DeckSnapshot(List.of(
                new CardSnapshot(UUID.nameUUIDFromBytes(name.getBytes()), name, attack, defense, Faction.DREAMS, 20,
                        List.of())));
    }
}
