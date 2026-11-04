package it.somnia.dreamscape.api.card.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardEffect;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.model.Faction;

// La carta come la riceve il frontend (guida ai dati, "Carta"). Per una versione (full art)
// id, codice e illustrazione sono i suoi; nome, testi, statistiche ed effetto sono della base.
public record CardResponse(
        UUID id,
        String code,
        UUID baseCardId,
        CardVariantType variantType,
        String name,
        FactionRefResponse faction,
        RarityRefResponse rarity,
        int attack,
        int defense,
        String description,
        CardEffectResponse effect,
        String illustrationUrl,
        String illustrationThumbUrl,
        BigDecimal illustrationFocusY,
        String lordTitle) {

    // effect è l'effetto della carta base, oppure null se non ne ha
    public static CardResponse from(Card card, CardEffect effect) {
        Card base = card.getBaseCard() != null ? card.getBaseCard() : card;
        Faction faction = base.getFaction();
        boolean isLord = faction.getLordCard() != null && faction.getLordCard().getId().equals(base.getId());
        return new CardResponse(
                card.getId(),
                card.getCode(),
                card.getBaseCard() != null ? base.getId() : null,
                card.getVariantType(),
                base.getName(),
                FactionRefResponse.from(faction),
                RarityRefResponse.from(base.getRarity()),
                base.getAttack(),
                base.getDefense(),
                base.getDescription(),
                effect != null ? CardEffectResponse.from(effect) : null,
                card.getIllustrationUrl(),
                card.getIllustrationThumbUrl(),
                card.getIllustrationFocusY(),
                isLord ? faction.getLordTitle() : null);
    }
}
