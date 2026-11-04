package it.somnia.dreamscape.engine.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.somnia.dreamscape.engine.Side;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RoundLogTest {

    private static final UUID LIGHTHOUSE = UUID.fromString("019a1f30-0000-7000-8000-000000000003");
    private static final UUID FACELESS_MAN = UUID.fromString("019a1f30-0000-7000-8000-000000000008");

    // Il round 3 dell'esempio di schema.md: l'Uomo Senza Volto (8 + 1) uccide il Faro nella Nebbia (difesa 7)
    private static RoundLog schemaExampleRound() {
        Attack facelessManHit = new Attack(Side.OPPONENT, FACELESS_MAN, 8, 1, 0, 9, 7, -2);
        Attack lighthouseHit = new Attack(Side.CHALLENGER, LIGHTHOUSE, 4, 0, -2, 2, 5, 3);
        List<GameEvent> events = List.of(
                new CardPlayed(Side.CHALLENGER, LIGHTHOUSE, 4, 7),
                new CardPlayed(Side.OPPONENT, FACELESS_MAN, 8, 5),
                new EffectApplied(Phase.COMBAT, Side.OPPONENT, FACELESS_MAN, "WEAKENING", 2, Side.CHALLENGER,
                        Target.CARD, Stat.ATTACK, 4, 2, true),
                facelessManHit,
                lighthouseHit,
                new CardDied(Side.CHALLENGER, LIGHTHOUSE),
                new PlayerDamage(Side.CHALLENGER, 2, 20, 18));
        AfterRound afterRound = new AfterRound(
                new SideState(null, null, null, 18, 18, 2),
                new SideState(FACELESS_MAN, 8, 3, 20, 18, 1));
        return new RoundLog(3, 2, events, RoundOutcome.CHALLENGER_CARD_DIED, afterRound);
    }

    @Test
    void schemaExample_playerDamage_isWinnerAttackMinusLoserDefenseBefore() {
        // Given
        RoundLog round = schemaExampleRound();
        Attack winnerHit = (Attack) round.events().get(3);
        PlayerDamage damage = (PlayerDamage) round.events().get(6);

        // When
        int expected = winnerHit.attack() + winnerHit.factionBonus() + winnerHit.attackModifier()
                - winnerHit.defenseBefore();

        // Then
        assertEquals(expected, damage.amount());
    }

    @Test
    void events_anyRound_returnsUnmodifiableList() {
        // Given
        RoundLog round = schemaExampleRound();

        // When / Then
        assertThrows(UnsupportedOperationException.class,
                () -> round.events().add(new CardDied(Side.OPPONENT, FACELESS_MAN)));
    }

    @Test
    void create_roundZero_throwsIllegalArgumentException() {
        // Given
        AfterRound afterRound = schemaExampleRound().afterRound();

        // When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new RoundLog(0, 1, List.of(), RoundOutcome.NONE_DIED, afterRound));
    }
}
