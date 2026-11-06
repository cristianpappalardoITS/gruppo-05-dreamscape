package it.somnia.dreamscape.engine.log;

/**
 * Le tre fasi di un round, come nel regolamento.
 */
public enum Phase {

    /** Fase iniziale: chi non ha una creatura in campo gioca la prima carta del mazzo. */
    START,

    /** Combattimento: le due creature si colpiscono contemporaneamente. */
    COMBAT,

    /** Fase finale: morti, danni ai giocatori, effetti di fine round, controllo della fine partita. */
    END
}
