package it.somnia.dreamscape.api.card.repository;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

/**
 * I filtri su tutte le carte, comprese le versioni e quelle disattivate (backoffice, {@code GET /admin/cards}).
 *
 * <p>Fazione, rarità e nome di una full art sono quelli della sua carta base (decisione 025): ogni filtro guarda
 * la carta base quando c'è, altrimenti la carta stessa.
 */
public final class AllCardsSpecifications {

    // Il carattere che rende letterali % e _ scritti dall'utente nella ricerca
    private static final char LIKE_ESCAPE = '\\';

    private AllCardsSpecifications() {
    }

    // Codice della fazione, es. DREAMS
    public static Specification<Card> hasFaction(String factionCode) {
        return (root, query, cb) -> cb.equal(cb.coalesce(
                baseCard(root).join("faction", JoinType.LEFT).<String>get("code"),
                root.join("faction", JoinType.LEFT).<String>get("code")), factionCode);
    }

    // Codice della rarità, es. LEGENDARY
    public static Specification<Card> hasRarity(String rarityCode) {
        return (root, query, cb) -> cb.equal(cb.coalesce(
                baseCard(root).join("rarity", JoinType.LEFT).<String>get("code"),
                root.join("rarity", JoinType.LEFT).<String>get("code")), rarityCode);
    }

    // Parte del nome o del codice, senza distinguere maiuscole e minuscole
    public static Specification<Card> nameOrCodeContains(String search) {
        String pattern = "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(cb.coalesce(baseCard(root).<String>get("name"), root.<String>get("name"))),
                        pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("code")), pattern, LIKE_ESCAPE));
    }

    public static Specification<Card> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    // true = solo le full art, false = solo le carte normali
    public static Specification<Card> isFullArt(boolean fullArt) {
        return (root, query, cb) -> fullArt
                ? cb.equal(root.get("variantType"), CardVariantType.FULL_ART)
                : cb.isNull(root.get("variantType"));
    }

    private static Join<Card, Card> baseCard(Root<Card> root) {
        return root.join("baseCard", JoinType.LEFT);
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
