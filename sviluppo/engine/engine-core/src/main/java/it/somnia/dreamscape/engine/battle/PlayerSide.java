package it.somnia.dreamscape.engine.battle;

import it.somnia.dreamscape.engine.Side;
import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.log.SideState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Lo stato di un lato durante la partita: mazzo, scarti, punti vita e creatura in campo.
 *
 * <p>È modificabile e vive solo dentro {@link BattleEngine}: fuori escono solo copie
 * immutabili ({@link SideState}).
 */
final class PlayerSide {

    private final Side side;
    private final Deque<CardSnapshot> deck;
    private int cardsInDiscard;
    private int lifePoints;
    private Creature creature;

    /**
     * Crea un lato con il mazzo già mescolato e i punti vita iniziali.
     *
     * @param side       il lato, non {@code null}
     * @param deck       il mazzo mescolato, la prima carta è la cima; non {@code null}
     * @param lifePoints i punti vita iniziali
     */
    PlayerSide(Side side, List<CardSnapshot> deck, int lifePoints) {
        this.side = Objects.requireNonNull(side, "side");
        this.deck = new ArrayDeque<>(Objects.requireNonNull(deck, "deck"));
        this.lifePoints = lifePoints;
    }

    Side side() {
        return side;
    }

    int lifePoints() {
        return lifePoints;
    }

    /**
     * Restituisce la creatura in campo.
     *
     * @return la creatura, {@code null} se il lato non ne ha
     */
    Creature creature() {
        return creature;
    }

    boolean hasCreature() {
        return creature != null;
    }

    /**
     * Dice se il lato dovrebbe giocare una carta ma non ne ha più.
     *
     * @return {@code true} se non ha una creatura in campo e il mazzo è vuoto
     */
    boolean isDeckExhausted() {
        return creature == null && deck.isEmpty();
    }

    /**
     * Mette in campo la prima carta del mazzo.
     *
     * @return la creatura appena giocata
     * @throws IllegalStateException se c'è già una creatura in campo o il mazzo è vuoto
     */
    Creature playTopCard() {
        if (creature != null) {
            throw new IllegalStateException(side + " ha già una creatura in campo");
        }
        if (deck.isEmpty()) {
            throw new IllegalStateException(side + " non ha carte da giocare");
        }
        creature = new Creature(deck.removeFirst());
        return creature;
    }

    /**
     * Toglie dal campo la creatura e la mette tra gli scarti.
     *
     * @throws IllegalStateException se non c'è una creatura in campo
     */
    void discardCreature() {
        if (creature == null) {
            throw new IllegalStateException(side + " non ha una creatura da scartare");
        }
        creature = null;
        cardsInDiscard++;
    }

    /**
     * Toglie punti vita al giocatore, senza scendere sotto 0.
     *
     * @param amount il danno, almeno 0
     */
    void loseLifePoints(int amount) {
        lifePoints = Math.max(0, lifePoints - amount);
    }

    /**
     * Restituisce lo stato del lato per il log di fine round.
     *
     * @return la copia immutabile dello stato
     */
    SideState toState() {
        if (creature == null) {
            return new SideState(null, null, null, lifePoints, deck.size(), cardsInDiscard);
        }
        return new SideState(creature.card().cardId(), creature.attack(), creature.defense(), lifePoints,
                deck.size(), cardsInDiscard);
    }
}
