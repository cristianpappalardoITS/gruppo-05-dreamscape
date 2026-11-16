package it.somnia.dreamscape.api.deck.repository;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.deck.model.UserCards;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

/**
 * I filtri della collezione di un utente, da combinare con {@link Specification#allOf}.
 *
 * <p>Fazione, rarità e nome di una full art sono quelli della sua carta base (decisione 025):
 * per questo ogni filtro guarda la carta base quando c'è, altrimenti la carta stessa.
 */
public final class UserCardsSpecifications {

    // Il carattere che rende letterali % e _ scritti dall'utente nella ricerca
    private static final char LIKE_ESCAPE = '\\';

    private UserCardsSpecifications() {
    }

    public static Specification<UserCards> ownedBy(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    // Codice della fazione, es. DREAMS
    public static Specification<UserCards> hasFaction(String factionCode) {
        return (root, query, cb) -> cb.equal(cb.coalesce(
                baseCard(root).join("faction", JoinType.LEFT).<String>get("code"),
                card(root).join("faction", JoinType.LEFT).<String>get("code")), factionCode);
    }

    // Codice della rarità, es. RARE
    public static Specification<UserCards> hasRarity(String rarityCode) {
        return (root, query, cb) -> cb.equal(cb.coalesce(
                baseCard(root).join("rarity", JoinType.LEFT).<String>get("code"),
                card(root).join("rarity", JoinType.LEFT).<String>get("code")), rarityCode);
    }

    // Parte del nome, senza distinguere maiuscole e minuscole
    public static Specification<UserCards> nameContains(String search) {
        String pattern = "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            Path<String> baseName = baseCard(root).get("name");
            Path<String> ownName = card(root).get("name");
            return cb.like(cb.lower(cb.coalesce(baseName, ownName)), pattern, LIKE_ESCAPE);
        };
    }

    // true = solo le full art, false = solo le carte normali
    public static Specification<UserCards> isFullArt(boolean fullArt) {
        return (root, query, cb) -> {
            Path<CardVariantType> variantType = card(root).get("variantType");
            return fullArt ? cb.equal(variantType, CardVariantType.FULL_ART) : cb.isNull(variantType);
        };
    }

    private static Join<UserCards, Card> card(Root<UserCards> root) {
        return root.join("card");
    }

    private static Join<Card, Card> baseCard(Root<UserCards> root) {
        return card(root).join("baseCard", JoinType.LEFT);
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
