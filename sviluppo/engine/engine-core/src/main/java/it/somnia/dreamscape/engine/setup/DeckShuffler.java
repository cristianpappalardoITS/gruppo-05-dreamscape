package it.somnia.dreamscape.engine.setup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Mescola un mazzo con l'algoritmo di Fisher-Yates.
 *
 * <p>L'algoritmo è scritto qui invece di usare {@code Collections.shuffle}: così l'ordine
 * delle carte dipende solo dal {@link Random} e da questo codice, non da come una versione
 * di Java mescola le liste. Con lo stesso {@link Random} si ottiene sempre lo stesso ordine.
 */
public final class DeckShuffler {

    private DeckShuffler() {
    }

    /**
     * Restituisce una copia mescolata delle carte, senza modificare la lista ricevuta.
     *
     * @param cards  le carte da mescolare, non {@code null} e senza elementi {@code null}
     * @param random il generatore della partita, non {@code null}
     * @param <T>    il tipo delle carte
     * @return una nuova lista non modificabile con le stesse carte in ordine casuale
     * @throws NullPointerException se {@code cards}, {@code random} o una carta sono {@code null}
     */
    public static <T> List<T> shuffle(List<T> cards, Random random) {
        Objects.requireNonNull(cards, "cards");
        Objects.requireNonNull(random, "random");

        List<T> shuffled = new ArrayList<>(cards);
        // Dall'ultima posizione alla seconda: ogni posizione riceve una carta scelta tra quelle non ancora fissate
        for (int position = shuffled.size() - 1; position > 0; position--) {
            int chosen = random.nextInt(position + 1);
            Collections.swap(shuffled, position, chosen);
        }
        return List.copyOf(shuffled);
    }
}
