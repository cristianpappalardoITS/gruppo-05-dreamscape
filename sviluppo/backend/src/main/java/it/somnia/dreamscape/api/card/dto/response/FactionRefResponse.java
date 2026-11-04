package it.somnia.dreamscape.api.card.dto.response;

import it.somnia.dreamscape.api.card.model.Faction;

// Fazione dentro un altro oggetto, es. la carta: { "code": "DREAMS", "name": "Dreams" }
public record FactionRefResponse(String code, String name) {

    public static FactionRefResponse from(Faction faction) {
        return new FactionRefResponse(faction.getCode(), faction.getName());
    }
}
