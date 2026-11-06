package it.somnia.dreamscape.engine.setup;

import it.somnia.dreamscape.engine.Side;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Inizio della partita: i due mazzi mescolati e il risultato del lancio della moneta.
 *
 * <p>Si crea con {@link #prepare(List, List, long)}, che dal seed della partita crea un solo
 * {@link Random} e lo usa sempre in quest'ordine (decisione 029):
 * <ol>
 *     <li>mescola il mazzo di {@link Side#CHALLENGER};</li>
 *     <li>mescola il mazzo di {@link Side#OPPONENT};</li>
 *     <li>tira la moneta.</li>
 * </ol>
 * Cambiare quest'ordine cambia tutte le partite rigiocate con lo stesso seed.
 *
 * @param challengerDeck il mazzo mescolato di chi ha avviato la partita; la prima carta è la cima
 * @param opponentDeck   il mazzo mescolato dell'avversario; la prima carta è la cima
 * @param coinTossWinner chi ha vinto la moneta: applica per primo i propri effetti
 *                       quando due effetti scattano nello stesso momento
 * @param <T>            il tipo delle carte
 */
public record MatchSetup<T>(List<T> challengerDeck, List<T> opponentDeck, Side coinTossWinner) {

    /**
     * Controlla i valori e rende le liste non modificabili.
     *
     * @throws NullPointerException se un valore o una carta sono {@code null}
     */
    public MatchSetup {
        challengerDeck = List.copyOf(Objects.requireNonNull(challengerDeck, "challengerDeck"));
        opponentDeck = List.copyOf(Objects.requireNonNull(opponentDeck, "opponentDeck"));
        Objects.requireNonNull(coinTossWinner, "coinTossWinner");
    }

    /**
     * Mescola i due mazzi e tira la moneta usando il seed della partita.
     *
     * <p>Con lo stesso seed e le stesse carte restituisce sempre gli stessi mazzi e la stessa moneta.
     *
     * @param challengerCards le carte del mazzo di chi ha avviato la partita, non vuoto
     * @param opponentCards   le carte del mazzo dell'avversario, non vuoto
     * @param seed            il seed della partita, salvato dal backend in {@code matches.random_seed}
     * @param <T>             il tipo delle carte
     * @return i mazzi mescolati e il vincitore della moneta
     * @throws NullPointerException     se un mazzo o una carta sono {@code null}
     * @throws IllegalArgumentException se un mazzo è vuoto
     */
    public static <T> MatchSetup<T> prepare(List<T> challengerCards, List<T> opponentCards, long seed) {
        requireNotEmpty(challengerCards, Side.CHALLENGER);
        requireNotEmpty(opponentCards, Side.OPPONENT);

        Random random = new Random(mixSeed(seed));
        List<T> challengerDeck = DeckShuffler.shuffle(challengerCards, random);
        List<T> opponentDeck = DeckShuffler.shuffle(opponentCards, random);
        Side coinTossWinner = random.nextBoolean() ? Side.CHALLENGER : Side.OPPONENT;

        return new MatchSetup<>(challengerDeck, opponentDeck, coinTossWinner);
    }

    /**
     * Rimescola i bit del seed con la funzione finale di SplitMix64.
     *
     * <p>{@link Random} con seed vicini (0, 1, 2...) produce sequenze molto simili: senza questo
     * passaggio, con i seed da 0 a 99 la moneta esce {@link Side#CHALLENGER} 96 volte su 100.
     * Le simulazioni usano seed in fila, quindi i risultati sarebbero falsati.
     *
     * @param seed il seed della partita
     * @return il seed da passare a {@link Random}; seed diversi danno sempre valori diversi
     */
    static long mixSeed(long seed) {
        long mixed = seed;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    private static void requireNotEmpty(List<?> cards, Side side) {
        Objects.requireNonNull(cards, side + " cards");
        if (cards.isEmpty()) {
            throw new IllegalArgumentException("Il mazzo di " + side + " è vuoto");
        }
    }
}
