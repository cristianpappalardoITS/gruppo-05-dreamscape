package it.somnia.dreamscape.api.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Accesso (POST /auth/login): login è lo username oppure l'email
public record LoginRequest(
        @NotBlank
        @Size(max = 255)
        String login,

        @NotBlank
        @Size(max = 100)
        String password) {
}
