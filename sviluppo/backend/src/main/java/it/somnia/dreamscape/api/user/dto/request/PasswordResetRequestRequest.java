package it.somnia.dreamscape.api.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Password dimenticata (POST /auth/password-reset-requests): risposta sempre 202
public record PasswordResetRequestRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email) {
}
