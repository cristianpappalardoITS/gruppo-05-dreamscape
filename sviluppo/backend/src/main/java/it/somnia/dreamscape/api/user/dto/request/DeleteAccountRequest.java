package it.somnia.dreamscape.api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Cancellazione dell'account (DELETE /me): si chiede la password; l'account viene anonimizzato
public record DeleteAccountRequest(
        @Schema(description = "La password attuale, per conferma")
        @NotBlank
        String password) {
}
