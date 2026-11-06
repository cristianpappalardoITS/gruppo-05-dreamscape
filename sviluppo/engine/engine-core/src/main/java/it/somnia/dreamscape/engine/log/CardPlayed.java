package it.somnia.dreamscape.engine.log;

import it.somnia.dreamscape.engine.Side;

import java.util.Objects;
import java.util.UUID;

/**
 * Una carta entra in campo: il lato non aveva una creatura e gioca la prima carta del mazzo.
 *
 * @param side    il lato che gioca la carta
 * @param cardId  la carta giocata
 * @param attack  l'attacco della carta quando entra in campo
 * @param defense la difesa della carta quando entra in campo
 */
public record CardPlayed(Side side, UUID cardId, int attack, int defense) implements GameEvent {

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException se {@code side} o {@code cardId} sono {@code null}
     */
    public CardPlayed {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(cardId, "cardId");
    }

    @Override
    public Phase phase() {
        return Phase.START;
    }

    @Override
    public EventType type() {
        return EventType.CARD_PLAYED;
    }
}
