package it.somnia.dreamscape.api.pack.dto.response;

import java.util.List;
import java.util.UUID;

// Un pacchetto di benvenuto aperto: { "openingId", "packName", "cards" }
public record WelcomeOpeningResponse(UUID openingId, String packName, List<OpenedCardResponse> cards) {
}
