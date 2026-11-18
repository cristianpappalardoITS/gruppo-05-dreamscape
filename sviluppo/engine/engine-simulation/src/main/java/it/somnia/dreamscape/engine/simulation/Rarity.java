package it.somnia.dreamscape.engine.simulation;

/**
 * Le rarità delle carte, con le copie massime di una stessa carta in un mazzo.
 *
 * <p>Valori di {@code rarities} in V100 (decisione 017). I limiti per rarità del mazzo intero
 * stanno nei profili di {@link DeckProfile}, che li rispettano già.
 */
enum Rarity {
    COMMON(2),
    RARE(2),
    EPIC(2),
    LEGENDARY(1);

    private final int maxCopies;

    Rarity(int maxCopies) {
        this.maxCopies = maxCopies;
    }

    /**
     * Restituisce quante copie della stessa carta di questa rarità può avere un mazzo.
     *
     * @return le copie massime, almeno 1
     */
    int maxCopies() {
        return maxCopies;
    }
}
