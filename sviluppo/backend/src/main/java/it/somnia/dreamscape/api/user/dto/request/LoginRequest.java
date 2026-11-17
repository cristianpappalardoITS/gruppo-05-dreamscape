package it.somnia.dreamscape.api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Accesso (POST /auth/login): login è lo username oppure l'email
public record LoginRequest(
        @Schema(description = "Username o email", example = "anna_test")
        @NotBlank
        @Size(max = 255)
        String login,

        @Schema(example = "prova-dreamscape")
        @NotBlank
        @Size(max = 100)
        String password) {
}
