package it.somnia.dreamscape.api.user.dto.request;

import jakarta.validation.constraints.NotBlank;

// Cancellazione dell'account (DELETE /me): si chiede la password; l'account viene anonimizzato
public record DeleteAccountRequest(
        @NotBlank
        String password) {
}
