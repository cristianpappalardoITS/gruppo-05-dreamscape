package it.somnia.dreamscape.api.match.service;

import java.util.List;
import java.util.UUID;

import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.model.EffectTrigger;

/**
 * La copia di un mazzo salvata in {@code matches.challenger_deck_snapshot} e {@code opponent_deck_snapshot}, nel
 * formato di schema.md ("Formato dei campi JSON di matches"). Serve al replay: le carte si mostrano com'erano al
 * momento della partita, anche se poi il mazzo cambia o viene cancellato.
 *
 * @param deckId   il mazzo usato
 * @param deckName il nome del mazzo al momento della partita
 * @param cards    le carte in ordine di codice
 */
public record StoredDeckSnapshot(UUID deckId, String deckName, List<Card> cards) {

    /**
     * Una carta del mazzo. {@code cardId} è la versione usata (es. la full art); nome, statistiche ed effetti
     * sono quelli della carta base al momento della partita.
     */
    public record Card(UUID cardId, String cardCode, UUID baseCardId, CardVariantType variantType, String name,
            int attack, int defense, String faction, String rarity, int quantity, List<Effect> effects) {
    }

    /**
     * L'effetto di una carta; {@code value} è il valore X, null se l'effetto non ce l'ha.
     */
    public record Effect(String code, String name, EffectTrigger triggerEvent, Integer value) {
    }
}
