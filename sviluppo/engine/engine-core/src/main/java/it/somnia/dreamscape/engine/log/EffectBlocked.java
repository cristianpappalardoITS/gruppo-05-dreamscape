package it.somnia.dreamscape.engine.log;

import it.somnia.dreamscape.engine.Side;

import java.util.Objects;
import java.util.UUID;

/**
 * Un effetto ne ferma un altro, oppure ferma una regola (bonus di fazione, colpo, danno al giocatore).
 *
 * @param phase   la fase in cui l'effetto viene fermato
 * @param side    il lato della carta che blocca
 * @param cardId  la carta che blocca
 * @param code    il codice dell'effetto che blocca (es. {@code IMMUNITY}, {@code BARRIER})
 * @param blocked cosa viene fermato: il codice di un effetto, oppure {@link #FACTION_BONUS},
 *                {@link #COMBAT_HIT} o {@link #PLAYER_DAMAGE}
 */
public record EffectBlocked(Phase phase, Side side, UUID cardId, String code, String blocked)
        implements GameEvent {

    /** Il bonus di fazione dell'avversario, fermato da Neutralità. */
    public static final String FACTION_BONUS = "FACTION_BONUS";

    /** Un colpo del combattimento, fermato da Barriera. */
    public static final String COMBAT_HIT = "COMBAT_HIT";

    /** Il danno del combattimento al giocatore, fermato da Culla. */
    public static final String PLAYER_DAMAGE = "PLAYER_DAMAGE";

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException se un valore è {@code null}
     */
    public EffectBlocked {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(cardId, "cardId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(blocked, "blocked");
    }

    @Override
    public EventType type() {
        return EventType.EFFECT_BLOCKED;
    }
}
