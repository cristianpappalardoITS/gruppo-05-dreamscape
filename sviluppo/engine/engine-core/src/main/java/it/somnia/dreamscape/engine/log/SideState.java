package it.somnia.dreamscape.engine.log;

import java.util.UUID;

/**
 * Lo stato di un lato a fine round: serve al replay per saltare a un round senza rileggere gli eventi.
 *
 * @param cardId         la creatura in campo, {@code null} se il lato non ne ha
 * @param attack         l'attacco con le modifiche permanenti, senza bonus di fazione; {@code null} senza creatura
 * @param defense        la difesa attuale; {@code null} senza creatura
 * @param lifePoints     i punti vita, almeno 0
 * @param cardsInDeck    le carte rimaste nel mazzo, almeno 0
 * @param cardsInDiscard le carte nella pila degli scarti, almeno 0
 */
public record SideState(UUID cardId, Integer attack, Integer defense, int lifePoints, int cardsInDeck,
                        int cardsInDiscard) {

    /**
     * Controlla i valori.
     *
     * @throws IllegalArgumentException se un contatore è negativo, o se attacco e difesa
     *                                  non sono presenti esattamente quando c'è una creatura
     */
    public SideState {
        boolean hasCard = cardId != null;
        if ((attack != null) != hasCard || (defense != null) != hasCard) {
            throw new IllegalArgumentException("Attacco e difesa ci sono solo se c'è una creatura in campo");
        }
        if (lifePoints < 0 || cardsInDeck < 0 || cardsInDiscard < 0) {
            throw new IllegalArgumentException("Punti vita e carte non possono essere negativi");
        }
    }
}
