package it.somnia.dreamscape.api.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Nuova password dal link dell'email (POST /auth/password-resets)
public record PasswordResetRequest(
        @NotBlank
        @Size(max = 100)
        String token,

        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword) {
}
