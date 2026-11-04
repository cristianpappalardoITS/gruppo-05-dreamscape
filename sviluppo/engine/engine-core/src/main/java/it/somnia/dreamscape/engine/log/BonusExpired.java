package it.somnia.dreamscape.engine.log;

import it.somnia.dreamscape.engine.Side;

import java.util.Objects;
import java.util.UUID;

/**
 * Finisce un bonus che valeva solo per un round, su una creatura ancora in campo.
 * Si scrive solo se cambia qualcosa.
 *
 * @param side   il lato della creatura
 * @param cardId la creatura
 * @param code   il codice dell'effetto che aveva dato il bonus
 * @param stat   il valore che torna com'era
 * @param before il valore prima della fine del bonus
 * @param after  il valore dopo la fine del bonus
 */
public record BonusExpired(Side side, UUID cardId, String code, Stat stat, int before, int after)
        implements GameEvent {

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException se un valore è {@code null}
     */
    public BonusExpired {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(cardId, "cardId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(stat, "stat");
    }

    @Override
    public Phase phase() {
        return Phase.END;
    }

    @Override
    public EventType type() {
        return EventType.BONUS_EXPIRED;
    }
}
