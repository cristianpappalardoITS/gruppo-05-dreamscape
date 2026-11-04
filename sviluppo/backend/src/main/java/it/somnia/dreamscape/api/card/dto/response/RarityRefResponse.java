package it.somnia.dreamscape.api.card.dto.response;

import it.somnia.dreamscape.api.card.model.Rarity;

// Rarità dentro un altro oggetto, es. la carta: { "code": "LEGENDARY", "name": "Leggendaria" }
public record RarityRefResponse(String code, String name) {

    public static RarityRefResponse from(Rarity rarity) {
        return new RarityRefResponse(rarity.getCode(), rarity.getName());
    }
}
