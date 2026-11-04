package it.somnia.dreamscape.engine.log;

import it.somnia.dreamscape.engine.Side;

import java.util.Objects;
import java.util.UUID;

/**
 * Una creatura muore e va nella pila degli scarti del suo lato.
 *
 * @param side   il lato della creatura morta
 * @param cardId la creatura morta
 */
public record CardDied(Side side, UUID cardId) implements GameEvent {

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException se un valore è {@code null}
     */
    public CardDied {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(cardId, "cardId");
    }

    @Override
    public Phase phase() {
        return Phase.END;
    }

    @Override
    public EventType type() {
        return EventType.CARD_DIED;
    }
}
