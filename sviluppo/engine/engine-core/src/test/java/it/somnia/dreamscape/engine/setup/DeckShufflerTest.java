package it.somnia.dreamscape.engine.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class DeckShufflerTest {

    private static final List<Integer> TEN_CARDS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    @Test
    void shuffle_sameSeed_returnsSameOrder() {
        // Given
        Random first = new Random(42);
        Random second = new Random(42);

        // When
        List<Integer> firstOrder = DeckShuffler.shuffle(TEN_CARDS, first);
        List<Integer> secondOrder = DeckShuffler.shuffle(TEN_CARDS, second);

        // Then
        assertEquals(firstOrder, secondOrder);
    }

    // Blocca l'algoritmo: se questo ordine cambia, le partite salvate non si rigiocano più uguali
    @Test
    void shuffle_seed42_returnsKnownOrder() {
        // When
        List<Integer> shuffled = DeckShuffler.shuffle(TEN_CARDS, new Random(42));

        // Then
        assertEquals(List.of(5, 7, 3, 2, 8, 10, 9, 6, 4, 1), shuffled);
    }

    @Test
    void shuffle_anySeed_keepsSameCards() {
        // When
        List<Integer> shuffled = new ArrayList<>(DeckShuffler.shuffle(TEN_CARDS, new Random(7)));

        // Then
        shuffled.sort(null);
        assertEquals(TEN_CARDS, shuffled);
    }

    @Test
    void shuffle_anySeed_doesNotChangeInputList() {
        // Given
        List<Integer> cards = new ArrayList<>(TEN_CARDS);

        // When
        DeckShuffler.shuffle(cards, new Random(42));

        // Then
        assertEquals(TEN_CARDS, cards);
    }

    @Test
    void shuffle_anySeed_returnsUnmodifiableList() {
        // When
        List<Integer> shuffled = DeckShuffler.shuffle(TEN_CARDS, new Random(42));

        // Then
        assertThrows(UnsupportedOperationException.class, () -> shuffled.add(11));
    }

    @Test
    void shuffle_emptyList_returnsEmptyList() {
        // When
        List<Integer> shuffled = DeckShuffler.shuffle(List.of(), new Random(42));

        // Then
        assertTrue(shuffled.isEmpty());
    }
}
