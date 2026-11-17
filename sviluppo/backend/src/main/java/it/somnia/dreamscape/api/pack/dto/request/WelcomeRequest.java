package it.somnia.dreamscape.api.pack.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Scelta della fazione nella pagina Benvenuto: { "starterFaction": "DREAMS" }
public record WelcomeRequest(
        @Schema(description = "DREAMS, NIGHTMARES o MEMORIES", example = "DREAMS")
        @NotBlank(message = "Scegli una fazione.") String starterFaction) {
}
