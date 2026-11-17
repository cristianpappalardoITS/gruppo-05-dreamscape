package it.somnia.dreamscape.api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Password dimenticata (POST /auth/password-reset-requests): risposta sempre 202
public record PasswordResetRequestRequest(
        @Schema(example = "lucida@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email) {
}
