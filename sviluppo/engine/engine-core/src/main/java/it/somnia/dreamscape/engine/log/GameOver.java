package it.somnia.dreamscape.engine.log;

import it.somnia.dreamscape.engine.EndReason;
import it.somnia.dreamscape.engine.Side;

import java.util.Objects;

/**
 * Fine della partita: è l'ultimo evento dell'ultimo round. È l'unico evento senza lato.
 *
 * @param winner    il lato che ha vinto, {@code null} se pareggio
 * @param endReason perché è finita la partita
 */
public record GameOver(Side winner, EndReason endReason) implements GameEvent {

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException     se {@code endReason} è {@code null}
     * @throws IllegalArgumentException se la partita finisce per limite di round con un vincitore
     */
    public GameOver {
        Objects.requireNonNull(endReason, "endReason");
        if (endReason == EndReason.ROUND_LIMIT && winner != null) {
            throw new IllegalArgumentException("Al limite di round la partita è sempre pari");
        }
    }

    @Override
    public Phase phase() {
        return Phase.END;
    }

    @Override
    public EventType type() {
        return EventType.GAME_OVER;
    }
}
