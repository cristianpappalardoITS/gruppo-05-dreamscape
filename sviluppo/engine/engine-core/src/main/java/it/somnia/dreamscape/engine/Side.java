package it.somnia.dreamscape.engine;

/**
 * Indica uno dei due lati della partita.
 *
 * <p>Il motore non conosce gli utenti: parla solo di lati, e il backend li traduce
 * negli utenti della partita (decisione 029).
 */
public enum Side {

    /** Chi ha avviato la partita: ha lanciato la sfida o premuto "Cerca partita". */
    CHALLENGER,

    /** Chi ha accettato la sfida o è stato scelto dal matchmaking. */
    OPPONENT
}
