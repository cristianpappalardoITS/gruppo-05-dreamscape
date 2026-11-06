package it.somnia.dreamscape.engine.log;

/**
 * Il tipo di un evento del log, cioè il campo {@code type} del JSON in {@code matches.round_log}.
 */
public enum EventType {
    CARD_PLAYED,
    EFFECT,
    EFFECT_BLOCKED,
    ATTACK,
    CARD_DIED,
    PLAYER_DAMAGE,
    BONUS_EXPIRED,
    GAME_OVER
}
