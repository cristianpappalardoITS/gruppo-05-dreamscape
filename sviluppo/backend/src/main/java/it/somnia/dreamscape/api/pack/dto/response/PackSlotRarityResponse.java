package it.somnia.dreamscape.api.pack.dto.response;

// Una rarità possibile in uno slot, con la probabilità in percentuale: { "code": "EPIC", "chance": 25.0 }
public record PackSlotRarityResponse(String code, double chance) {
}
