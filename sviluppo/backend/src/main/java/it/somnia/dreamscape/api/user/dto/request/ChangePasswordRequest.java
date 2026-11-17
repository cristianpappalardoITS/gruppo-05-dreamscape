package it.somnia.dreamscape.api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cambio password (PUT /me/password): dopo, gli altri dispositivi vengono scollegati
public record ChangePasswordRequest(
        @Schema(example = "prova-dreamscape")
        @NotBlank
        String currentPassword,

        @Schema(description = "Almeno 8 caratteri", example = "una-password-nuova")
        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword) {
}
