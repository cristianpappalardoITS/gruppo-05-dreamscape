package it.somnia.dreamscape.api.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cambio password (PUT /me/password): dopo, gli altri dispositivi vengono scollegati
public record ChangePasswordRequest(
        @NotBlank
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword) {
}
