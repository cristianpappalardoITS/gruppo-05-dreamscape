package it.somnia.dreamscape.engine.log;

import it.somnia.dreamscape.engine.Side;

import java.util.Objects;

/**
 * Il danno del combattimento al giocatore. I danni causati dagli effetti sono invece
 * {@link EffectApplied} con {@link Target#PLAYER}.
 *
 * @param side             il lato che subisce il danno
 * @param amount           il danno, almeno 0
 * @param lifePointsBefore i punti vita prima del danno
 * @param lifePointsAfter  i punti vita dopo il danno, mai sotto 0
 */
public record PlayerDamage(Side side, int amount, int lifePointsBefore, int lifePointsAfter) implements GameEvent {

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException     se {@code side} è {@code null}
     * @throws IllegalArgumentException se il danno o i punti vita finali sono negativi
     */
    public PlayerDamage {
        Objects.requireNonNull(side, "side");
        if (amount < 0) {
            throw new IllegalArgumentException("Il danno al giocatore è negativo: " + amount);
        }
        if (lifePointsAfter < 0) {
            throw new IllegalArgumentException("I punti vita scendono sotto 0: " + lifePointsAfter);
        }
    }

    @Override
    public Phase phase() {
        return Phase.END;
    }

    @Override
    public EventType type() {
        return EventType.PLAYER_DAMAGE;
    }
}
