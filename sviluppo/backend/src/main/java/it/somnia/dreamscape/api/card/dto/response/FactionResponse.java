package it.somnia.dreamscape.api.card.dto.response;

import java.util.List;
import java.util.UUID;

import it.somnia.dreamscape.api.card.model.Faction;

// Una fazione per la pagina "Il gioco" (GET /factions): contro chi è forte, contro chi è debole
// e il suo Lord (lordCardId è null finché non c'è il catalogo vero)
public record FactionResponse(
        String code,
        String name,
        String description,
        String lordTitle,
        UUID lordCardId,
        List<String> strongAgainst,
        List<String> weakAgainst) {

    public static FactionResponse from(Faction faction, List<String> strongAgainst, List<String> weakAgainst) {
        UUID lordCardId = faction.getLordCard() != null ? faction.getLordCard().getId() : null;
        return new FactionResponse(faction.getCode(), faction.getName(), faction.getDescription(),
                faction.getLordTitle(), lordCardId, strongAgainst, weakAgainst);
    }
}
