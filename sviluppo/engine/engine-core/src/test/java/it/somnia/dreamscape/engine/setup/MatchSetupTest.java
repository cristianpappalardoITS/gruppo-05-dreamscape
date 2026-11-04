package it.somnia.dreamscape.engine.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.somnia.dreamscape.engine.Side;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class MatchSetupTest {

    private static final List<Integer> TEN_CARDS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    @Test
    void prepare_sameSeed_returnsSameDecksAndCoinToss() {
        // When
        MatchSetup<Integer> first = MatchSetup.prepare(TEN_CARDS, TEN_CARDS, 42L);
        MatchSetup<Integer> second = MatchSetup.prepare(TEN_CARDS, TEN_CARDS, 42L);

        // Then
        assertEquals(first, second);
    }

    // Blocca seed, ordine dei mescolamenti e moneta: se cambia, le partite salvate non si rigiocano più uguali
    @Test
    void prepare_seed42_returnsKnownDecksAndCoinToss() {
        // When
        MatchSetup<Integer> setup = MatchSetup.prepare(TEN_CARDS, TEN_CARDS, 42L);

        // Then
        assertEquals(List.of(5, 6, 9, 1, 3, 10, 4, 2, 7, 8), setup.challengerDeck());
        assertEquals(List.of(3, 4, 2, 10, 9, 1, 5, 8, 7, 6), setup.opponentDeck());
        assertEquals(Side.CHALLENGER, setup.coinTossWinner());
    }

    @Test
    void prepare_anySeed_usesRandomInFixedOrder() {
        // Given: lo stesso Random usato a mano nell'ordine della decisione 029
        long seed = 1234L;
        Random random = new Random(MatchSetup.mixSeed(seed));
        List<Integer> expectedChallenger = DeckShuffler.shuffle(TEN_CARDS, random);
        List<Integer> expectedOpponent = DeckShuffler.shuffle(TEN_CARDS, random);
        Side expectedCoin = random.nextBoolean() ? Side.CHALLENGER : Side.OPPONENT;

        // When
        MatchSetup<Integer> setup = MatchSetup.prepare(TEN_CARDS, TEN_CARDS, seed);

        // Then
        assertEquals(expectedChallenger, setup.challengerDeck());
        assertEquals(expectedOpponent, setup.opponentDeck());
        assertEquals(expectedCoin, setup.coinTossWinner());
    }

    @Test
    void prepare_sameCardsOnBothSides_shufflesDecksIndependently() {
        // When
        MatchSetup<Integer> setup = MatchSetup.prepare(TEN_CARDS, TEN_CARDS, 42L);

        // Then
        assertNotEquals(setup.challengerDeck(), setup.opponentDeck());
    }

    // Con java.util.Random senza rimescolare il seed, i seed da 0 a 99 davano CHALLENGER 96 volte su 100
    @Test
    void prepare_consecutiveSeeds_givesBalancedCoinToss() {
        // Given
        int games = 1000;
        int challengerWins = 0;

        // When
        for (long seed = 0; seed < games; seed++) {
            if (MatchSetup.prepare(TEN_CARDS, TEN_CARDS, seed).coinTossWinner() == Side.CHALLENGER) {
                challengerWins++;
            }
        }

        // Then: valore fisso per questi seed, lontano dagli sbilanciamenti visti senza rimescolare
        assertTrue(challengerWins > 450 && challengerWins < 550, "CHALLENGER ha vinto " + challengerWins + " volte");
    }

    @Test
    void prepare_emptyChallengerDeck_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> MatchSetup.prepare(List.of(), TEN_CARDS, 42L));
    }

    @Test
    void prepare_emptyOpponentDeck_throwsIllegalArgumentException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> MatchSetup.prepare(TEN_CARDS, List.of(), 42L));
    }

    @Test
    void prepare_nullDeck_throwsNullPointerException() {
        // When / Then
        assertThrows(NullPointerException.class, () -> MatchSetup.prepare(null, TEN_CARDS, 42L));
    }

    @Test
    void mixSeed_differentSeeds_returnDifferentValues() {
        // When / Then
        assertNotEquals(MatchSetup.mixSeed(0L), MatchSetup.mixSeed(1L));
    }
}
