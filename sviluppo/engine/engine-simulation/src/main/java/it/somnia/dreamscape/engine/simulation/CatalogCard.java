package it.somnia.dreamscape.engine.simulation;

import it.somnia.dreamscape.engine.input.CardEffect;
import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.Faction;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Una carta del catalogo vero, letta dalla migration V102.
 *
 * @param code    il codice della carta, per esempio {@code BASE-001}
 * @param name    il nome della carta
 * @param attack  l'attacco
 * @param defense la difesa
 * @param faction la fazione
 * @param rarity  la rarità
 * @param effect  l'effetto, oppure {@code null} per le carte senza effetto
 */
record CatalogCard(String code, String name, int attack, int defense, Faction faction, Rarity rarity,
                   CardEffect effect) {

    CatalogCard {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(rarity, "rarity");
    }

    /**
     * Restituisce la copia della carta nel formato che il motore riceve dal backend.
     *
     * <p>L'id è ricavato dal codice, così la stessa carta ha sempre lo stesso id.
     *
     * @param quantity le copie della carta nel mazzo, almeno 1
     * @return la carta per un {@code DeckSnapshot}
     */
    CardSnapshot toSnapshot(int quantity) {
        UUID cardId = UUID.nameUUIDFromBytes(code.getBytes(StandardCharsets.UTF_8));
        List<CardEffect> effects = effect == null ? List.of() : List.of(effect);
        return new CardSnapshot(cardId, name, attack, defense, faction, quantity, effects);
    }
}
