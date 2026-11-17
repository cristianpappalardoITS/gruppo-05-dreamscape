package it.somnia.dreamscape.api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Nuova password dal link dell'email (POST /auth/password-resets)
public record PasswordResetRequest(
        @Schema(description = "Il token del link ricevuto per email (parametro token)")
        @NotBlank
        @Size(max = 100)
        String token,

        @Schema(description = "Almeno 8 caratteri", example = "una-password-nuova")
        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword) {
}
