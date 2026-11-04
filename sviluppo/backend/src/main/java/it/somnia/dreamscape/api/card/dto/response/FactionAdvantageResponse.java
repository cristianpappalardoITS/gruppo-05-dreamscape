package it.somnia.dreamscape.api.card.dto.response;

import it.somnia.dreamscape.api.card.model.FactionAdvantage;

// Un vantaggio di fazione: { "faction": "DREAMS", "strongAgainst": "MEMORIES", "attackBonus": 1 }
public record FactionAdvantageResponse(String faction, String strongAgainst, int attackBonus) {

    public static FactionAdvantageResponse from(FactionAdvantage advantage) {
        return new FactionAdvantageResponse(advantage.getFaction().getCode(),
                advantage.getTargetFaction().getCode(), advantage.getAttackBonus());
    }
}
