package it.somnia.dreamscape.api.card.dto.response;

import java.util.List;

// I numeri del regolamento (GET /rules). Carte per mazzo dalla configurazione, punti vita e
// limite di round dalla modalità BASE (game_modes), rarità e vantaggi dal database.
public record RulesResponse(
        int deckSize,
        int startingLifePoints,
        int roundLimit,
        List<RarityRuleResponse> rarities,
        List<FactionAdvantageResponse> factionAdvantages) {
}
