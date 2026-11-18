package it.somnia.dreamscape.engine.simulation;

/**
 * Quante carte di ogni rarità ha un mazzo di prova di 20 carte.
 *
 * <p>Entrambi rispettano i limiti della decisione 017: al massimo 2 Leggendarie, 4 Epiche e 8 Rare.
 */
enum DeckProfile {

    /** Il mazzo di un giocatore a metà strada, lo stesso dello script di bilanciamento delle carte. */
    TYPICAL("Tipico", 11, 6, 2, 1),

    /** Il mazzo più forte che le regole permettono: tutti i limiti di rarità al massimo. */
    STRONG("Forte", 6, 8, 4, 2);

    /** Le carte di un mazzo (decisione 017). */
    static final int DECK_SIZE = 20;

    private final String label;
    private final int commons;
    private final int rares;
    private final int epics;
    private final int legendaries;

    DeckProfile(String label, int commons, int rares, int epics, int legendaries) {
        if (commons + rares + epics + legendaries != DECK_SIZE) {
            throw new IllegalStateException("Il profilo " + label + " non ha " + DECK_SIZE + " carte");
        }
        this.label = label;
        this.commons = commons;
        this.rares = rares;
        this.epics = epics;
        this.legendaries = legendaries;
    }

    /**
     * Restituisce il nome del profilo nelle tabelle.
     *
     * @return il nome in italiano
     */
    String label() {
        return label;
    }

    /**
     * Restituisce quante carte della rarità indicata ha il mazzo.
     *
     * @param rarity la rarità, non {@code null}
     * @return il numero di carte, anche 0
     */
    int count(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> commons;
            case RARE -> rares;
            case EPIC -> epics;
            case LEGENDARY -> legendaries;
        };
    }
}
