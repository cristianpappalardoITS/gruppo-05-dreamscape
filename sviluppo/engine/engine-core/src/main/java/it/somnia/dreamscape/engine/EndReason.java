package it.somnia.dreamscape.engine;

/**
 * Perché è finita la partita, con gli stessi codici di {@code matches.end_reason}.
 */
public enum EndReason {

    /** Almeno un giocatore è arrivato a 0 punti vita. */
    LIFE_POINTS,

    /** Almeno un giocatore doveva giocare una carta e non ne aveva più. */
    DECK_EXHAUSTED,

    /** È finito l'ultimo round ammesso dalle regole: pareggio. */
    ROUND_LIMIT
}
