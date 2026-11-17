package it.somnia.dreamscape.api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Registrazione (POST /auth/register): regole dei campi della guida per il frontend.
// Che username ed email siano liberi lo controlla il service.
public record RegisterRequest(
        @Schema(description = "Da 3 a 30 caratteri: lettere, numeri e trattino basso. Non si cambia più",
                example = "lucida")
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Solo lettere, numeri e trattino basso.")
        String username,

        @Schema(example = "lucida@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Almeno 8 caratteri", example = "una-password-lunga")
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Schema(description = "Deve essere true", example = "true")
        @AssertTrue(message = "Bisogna accettare la Privacy Policy.")
        boolean privacyAccepted,

        @Schema(description = "Deve essere true", example = "true")
        @AssertTrue(message = "Bisogna accettare i Termini di servizio.")
        boolean termsAccepted) {
}
