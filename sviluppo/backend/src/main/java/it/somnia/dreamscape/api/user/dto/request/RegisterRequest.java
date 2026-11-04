package it.somnia.dreamscape.api.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Registrazione (POST /auth/register): regole dei campi della guida per il frontend.
// Che username ed email siano liberi lo controlla il service.
public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Solo lettere, numeri e trattino basso.")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @AssertTrue(message = "Bisogna accettare la Privacy Policy.")
        boolean privacyAccepted,

        @AssertTrue(message = "Bisogna accettare i Termini di servizio.")
        boolean termsAccepted) {
}
