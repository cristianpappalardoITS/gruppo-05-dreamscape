package it.somnia.dreamscape.engine.log;

import java.util.Objects;

/**
 * Lo stato dei due lati a fine round (campo {@code afterRound} del JSON).
 *
 * @param challenger lo stato di chi ha avviato la partita
 * @param opponent   lo stato dell'avversario
 */
public record AfterRound(SideState challenger, SideState opponent) {

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException se uno stato è {@code null}
     */
    public AfterRound {
        Objects.requireNonNull(challenger, "challenger");
        Objects.requireNonNull(opponent, "opponent");
    }
}
