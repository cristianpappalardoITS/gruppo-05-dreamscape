package it.somnia.dreamscape.api.match.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Cerca una partita (POST /matches/ranked, POST /matches/casual) con un proprio mazzo valido, anche diverso dal
// mazzo classificato
public record MatchmakingRequest(
        @Schema(description = "Un tuo mazzo valido", example = "019a2a10-1b2c-7d3e-8f40-a1b2c3d4e5f6")
        @NotNull(message = "Scegli il mazzo.") UUID deckId) {
}
