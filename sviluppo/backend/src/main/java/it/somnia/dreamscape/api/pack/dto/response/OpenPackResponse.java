package it.somnia.dreamscape.api.pack.dto.response;

import java.util.List;
import java.util.UUID;

// Risposta dell'acquisto: l'id dell'apertura per la sua pagina, il saldo dopo l'acquisto e le carte uscite
public record OpenPackResponse(UUID openingId, int creditsAfter, List<OpenedCardResponse> cards) {
}
