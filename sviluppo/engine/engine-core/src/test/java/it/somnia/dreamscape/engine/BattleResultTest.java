package it.somnia.dreamscape.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.somnia.dreamscape.engine.log.MatchLog;

import java.util.List;

import org.junit.jupiter.api.Test;

class BattleResultTest {

    @Test
    void isDraw_noWinner_returnsTrue() {
        // When
        BattleResult result = new BattleResult(null, EndReason.ROUND_LIMIT, 60, 4, 6, Side.OPPONENT, null);

        // Then
        assertTrue(result.isDraw());
    }

    @Test
    void isDraw_withWinner_returnsFalse() {
        // When
        BattleResult result = new BattleResult(Side.OPPONENT, EndReason.LIFE_POINTS, 24, 0, 6, Side.OPPONENT, null);

        // Then
        assertFalse(result.isDraw());
    }

    @Test
    void create_roundLimitWithWinner_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new BattleResult(Side.CHALLENGER, EndReason.ROUND_LIMIT, 60, 4, 6, Side.OPPONENT, null));
    }

    @Test
    void create_negativeLifePoints_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new BattleResult(Side.OPPONENT, EndReason.LIFE_POINTS, 24, -1, 6, Side.OPPONENT, null));
    }

    @Test
    void create_logWithDifferentRoundCount_throwsIllegalArgumentException() {
        // Given
        MatchLog emptyLog = new MatchLog("0.1.0", Side.OPPONENT, List.of());

        // When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new BattleResult(Side.OPPONENT, EndReason.LIFE_POINTS, 24, 0, 6, Side.OPPONENT, emptyLog));
    }
}
