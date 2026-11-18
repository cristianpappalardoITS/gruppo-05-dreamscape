package it.somnia.dreamscape.api.challenge.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Accettazione di una sfida (POST /challenges/{challengeId}/accept): il mazzo con cui gioca chi accetta
public record AcceptChallengeRequest(
        @Schema(description = "Il mio mazzo per la partita: deve essere valido")
        @NotNull(message = "Scegli il mazzo.") UUID deckId) {
}
