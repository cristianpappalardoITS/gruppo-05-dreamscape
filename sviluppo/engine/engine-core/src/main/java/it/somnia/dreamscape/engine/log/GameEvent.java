package it.somnia.dreamscape.engine.log;

/**
 * Un evento del log di un round, nell'ordine in cui è successo.
 *
 * <p>Ogni tipo di evento è un record con i campi descritti in {@code docs/database/schema.md}
 * (formato di {@code round_log}). L'interfaccia è {@code sealed}: i tipi possibili sono solo questi,
 * così chi trasforma il log in JSON li gestisce tutti.
 */
public sealed interface GameEvent
        permits CardPlayed, EffectApplied, EffectBlocked, Attack, CardDied, PlayerDamage, BonusExpired, GameOver {

    /**
     * Restituisce la fase del round in cui è successo l'evento.
     *
     * @return la fase, mai {@code null}
     */
    Phase phase();

    /**
     * Restituisce il tipo dell'evento, cioè il campo {@code type} del JSON.
     *
     * @return il tipo, mai {@code null}
     */
    EventType type();
}
