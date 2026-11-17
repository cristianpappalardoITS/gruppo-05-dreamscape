package it.somnia.dreamscape.api.admin.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardEffect;
import it.somnia.dreamscape.api.card.model.CardVariantType;

// Una carta come la vede il backoffice: i dati salvati, non quelli presi dalla carta base.
// deckCount: in quanti mazzi è usata (anche con le sue versioni), da dire prima di disattivarla.
public record AdminCardResponse(
        UUID id,
        String code,
        UUID baseCardId,
        CardVariantType variantType,
        String name,
        String description,
        Integer attack,
        Integer defense,
        String factionCode,
        String rarityCode,
        String cardSetCode,
        String effectCode,
        Integer effectValue,
        String illustrationUrl,
        String illustrationThumbUrl,
        BigDecimal illustrationFocusY,
        boolean isActive,
        long deckCount) {

    public static AdminCardResponse from(Card card, CardEffect effect, long deckCount) {
        return new AdminCardResponse(
                card.getId(),
                card.getCode(),
                card.getBaseCard() != null ? card.getBaseCard().getId() : null,
                card.getVariantType(),
                card.getName(),
                card.getDescription(),
                card.getAttack(),
                card.getDefense(),
                card.getFaction() != null ? card.getFaction().getCode() : null,
                card.getRarity() != null ? card.getRarity().getCode() : null,
                card.getCardSet().getCode(),
                effect != null ? effect.getEffect().getCode() : null,
                effect != null ? effect.getEffectValue() : null,
                card.getIllustrationUrl(),
                card.getIllustrationThumbUrl(),
                card.getIllustrationFocusY(),
                card.isActive(),
                deckCount);
    }
}
