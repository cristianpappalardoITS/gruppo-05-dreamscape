package it.somnia.dreamscape.engine.log;

import it.somnia.dreamscape.engine.Side;

import java.util.Objects;
import java.util.UUID;

/**
 * Un effetto cambia un valore: attacco o difesa di una creatura, oppure i punti vita di un giocatore.
 *
 * <p>Se un effetto cambia più valori, ci sono più eventi, uno per valore.
 *
 * @param phase           la fase in cui scatta l'effetto
 * @param side            il lato della carta che ha l'effetto
 * @param cardId          la carta che ha l'effetto
 * @param code            il codice dell'effetto (es. {@code WEAKENING})
 * @param value           il valore X, {@code null} se l'effetto non ha valore
 * @param targetSide      il lato su cui agisce l'effetto
 * @param target          se agisce sulla creatura in campo o sul giocatore
 * @param stat            il valore che cambia
 * @param before          il valore prima dell'effetto
 * @param after           il valore dopo l'effetto
 * @param untilEndOfRound {@code true} se vale solo per questo round (es. Speranza, Vendetta, Indebolimento)
 */
public record EffectApplied(Phase phase, Side side, UUID cardId, String code, Integer value, Side targetSide,
                            Target target, Stat stat, int before, int after, boolean untilEndOfRound)
        implements GameEvent {

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException se un valore obbligatorio è {@code null}
     */
    public EffectApplied {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(cardId, "cardId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(targetSide, "targetSide");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(stat, "stat");
    }

    @Override
    public EventType type() {
        return EventType.EFFECT;
    }
}
