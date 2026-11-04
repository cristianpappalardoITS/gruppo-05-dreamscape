package it.somnia.dreamscape.engine.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.somnia.dreamscape.engine.EndReason;
import it.somnia.dreamscape.engine.Side;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class GameEventTest {

    private static final UUID CARD_ID = UUID.fromString("019a1f30-0000-7000-8000-000000000003");

    @Test
    void phase_eventsWithFixedPhase_returnPhaseOfRegulation() {
        // When / Then
        assertEquals(Phase.START, new CardPlayed(Side.CHALLENGER, CARD_ID, 4, 7).phase());
        assertEquals(Phase.COMBAT, new Attack(Side.CHALLENGER, CARD_ID, 4, 0, 0, 4, 5, 1).phase());
        assertEquals(Phase.END, new CardDied(Side.CHALLENGER, CARD_ID).phase());
        assertEquals(Phase.END, new PlayerDamage(Side.CHALLENGER, 2, 20, 18).phase());
        assertEquals(Phase.END, new BonusExpired(Side.CHALLENGER, CARD_ID, "HOPE", Stat.DEFENSE, 6, 5).phase());
        assertEquals(Phase.END, new GameOver(Side.OPPONENT, EndReason.LIFE_POINTS).phase());
    }

    @Test
    void type_eachEvent_returnsTypeOfJson() {
        // When / Then
        assertEquals(EventType.CARD_PLAYED, new CardPlayed(Side.CHALLENGER, CARD_ID, 4, 7).type());
        assertEquals(EventType.EFFECT, new EffectApplied(Phase.COMBAT, Side.OPPONENT, CARD_ID, "WEAKENING", 2,
                Side.CHALLENGER, Target.CARD, Stat.ATTACK, 4, 2, true).type());
        assertEquals(EventType.EFFECT_BLOCKED, new EffectBlocked(Phase.COMBAT, Side.OPPONENT, CARD_ID, "BARRIER",
                EffectBlocked.COMBAT_HIT).type());
        assertEquals(EventType.ATTACK, new Attack(Side.CHALLENGER, CARD_ID, 4, 0, 0, 4, 5, 1).type());
        assertEquals(EventType.CARD_DIED, new CardDied(Side.CHALLENGER, CARD_ID).type());
        assertEquals(EventType.PLAYER_DAMAGE, new PlayerDamage(Side.CHALLENGER, 2, 20, 18).type());
        assertEquals(EventType.BONUS_EXPIRED,
                new BonusExpired(Side.CHALLENGER, CARD_ID, "HOPE", Stat.DEFENSE, 6, 5).type());
        assertEquals(EventType.GAME_OVER, new GameOver(null, EndReason.ROUND_LIMIT).type());
    }

    @Test
    void create_attackWithNegativeDamage_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new Attack(Side.CHALLENGER, CARD_ID, 0, 0, -2, -2, 5, 5));
    }

    @Test
    void create_playerDamageBelowZero_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new PlayerDamage(Side.CHALLENGER, 5, 3, -2));
    }

    @Test
    void create_gameOverAtRoundLimitWithWinner_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new GameOver(Side.CHALLENGER, EndReason.ROUND_LIMIT));
    }

    @Test
    void create_sideStateWithoutCardButWithAttack_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new SideState(null, 4, null, 20, 18, 2));
    }

    @Test
    void create_sideStateWithCardButWithoutDefense_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new SideState(CARD_ID, 4, null, 20, 18, 2));
    }
}
