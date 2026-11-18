package it.somnia.dreamscape.api.challenge.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Nuova sfida (POST /challenges): l'amico da sfidare e il mio mazzo, che deve essere valido
public record ChallengeRequest(
        @Schema(description = "L'id dell'amico da sfidare")
        @NotNull(message = "Scegli l'amico da sfidare.") UUID opponentId,
        @Schema(description = "Il mio mazzo per la sfida: deve essere valido")
        @NotNull(message = "Scegli il mazzo.") UUID deckId) {
}
