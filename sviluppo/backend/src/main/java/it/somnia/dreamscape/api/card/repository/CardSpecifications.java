package it.somnia.dreamscape.api.card.repository;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import it.somnia.dreamscape.api.card.model.Card;

/**
 * I filtri delle carte del catalogo, da combinare con {@link Specification#allOf}.
 *
 * <p>Fazione e rarità si leggono dalla carta normale: le versioni (full art) non le hanno.
 * Per questo i filtri valgono solo insieme a {@link #isActiveBaseCard()}.
 */
public final class CardSpecifications {

    // Il carattere che rende letterali % e _ scritti dall'utente nella ricerca
    private static final char LIKE_ESCAPE = '\\';

    private CardSpecifications() {
    }

    // Carte normali attive: niente full art e niente carte disattivate (decisione 026)
    public static Specification<Card> isActiveBaseCard() {
        return (root, query, cb) -> cb.and(cb.isNull(root.get("baseCard")), cb.isTrue(root.get("active")));
    }

    // Codice della fazione, es. DREAMS
    public static Specification<Card> hasFaction(String factionCode) {
        return (root, query, cb) -> cb.equal(root.join("faction").get("code"), factionCode);
    }

    // Codice della rarità, es. RARE
    public static Specification<Card> hasRarity(String rarityCode) {
        return (root, query, cb) -> cb.equal(root.join("rarity").get("code"), rarityCode);
    }

    // Parte del nome, senza distinguere maiuscole e minuscole
    public static Specification<Card> nameContains(String search) {
        String pattern = "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE);
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
