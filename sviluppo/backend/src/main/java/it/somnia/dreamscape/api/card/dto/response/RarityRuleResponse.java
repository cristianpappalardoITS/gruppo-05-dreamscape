package it.somnia.dreamscape.api.card.dto.response;

import it.somnia.dreamscape.api.card.model.Rarity;

// Una rarità con i limiti del mazzo; maxPerDeck null = nessun limite
public record RarityRuleResponse(String code, String name, Integer maxPerDeck, int maxCopies) {

    public static RarityRuleResponse from(Rarity rarity) {
        return new RarityRuleResponse(rarity.getCode(), rarity.getName(), rarity.getMaxPerDeck(),
                rarity.getMaxCopies());
    }
}
