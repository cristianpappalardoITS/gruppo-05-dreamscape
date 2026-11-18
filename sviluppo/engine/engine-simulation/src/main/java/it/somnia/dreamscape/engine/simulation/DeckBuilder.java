package it.somnia.dreamscape.engine.simulation;

import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.Faction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Compone mazzi di prova validi con le carte del catalogo.
 *
 * <p>Le carte si scelgono a caso, rarità per rarità, rispettando le copie massime di ogni carta
 * (decisione 017). Un mazzo è di una sola fazione oppure misto, con carte di tutte e tre.
 */
final class DeckBuilder {

    private final List<CatalogCard> catalog;

    /**
     * Crea un compositore di mazzi sulle carte indicate.
     *
     * @param catalog le carte del catalogo, non {@code null}
     */
    DeckBuilder(List<CatalogCard> catalog) {
        this.catalog = List.copyOf(Objects.requireNonNull(catalog, "catalog"));
    }

    /**
     * Compone un mazzo a caso.
     *
     * @param profile quante carte di ogni rarità, non {@code null}
     * @param faction la fazione delle carte, oppure {@code null} per un mazzo misto
     * @param random  il generatore che sceglie le carte, non {@code null}
     * @return il mazzo, con le copie di ogni carta raccolte in una sola riga come fa il backend
     * @throws IllegalStateException se il catalogo non ha abbastanza carte per il profilo
     */
    DeckSnapshot build(DeckProfile profile, Faction faction, Random random) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(random, "random");

        // LinkedHashMap: lo stesso seed dà sempre lo stesso mazzo nello stesso ordine
        Map<CatalogCard, Integer> copies = new LinkedHashMap<>();
        for (Rarity rarity : Rarity.values()) {
            List<CatalogCard> pool = new ArrayList<>();
            for (CatalogCard card : catalog) {
                if (card.rarity() == rarity && (faction == null || card.faction() == faction)) {
                    pool.add(card);
                }
            }
            int needed = profile.count(rarity);
            if (pool.size() * rarity.maxCopies() < needed) {
                throw new IllegalStateException("Non ci sono abbastanza carte " + rarity + " per il mazzo "
                        + profile.label() + (faction == null ? " misto" : " " + faction));
            }
            while (needed > 0) {
                CatalogCard card = pool.get(random.nextInt(pool.size()));
                int taken = copies.getOrDefault(card, 0);
                if (taken < rarity.maxCopies()) {
                    copies.put(card, taken + 1);
                    needed--;
                }
            }
        }

        List<CardSnapshot> cards = new ArrayList<>();
        copies.forEach((card, quantity) -> cards.add(card.toSnapshot(quantity)));
        return new DeckSnapshot(cards);
    }
}
