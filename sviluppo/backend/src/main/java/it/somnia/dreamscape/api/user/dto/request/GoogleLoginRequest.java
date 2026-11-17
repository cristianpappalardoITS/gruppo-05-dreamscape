package it.somnia.dreamscape.api.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Accesso con Google (POST /auth/google). La prima volta basta idToken; se serve la registrazione
// (risposta 202) si rimanda con username e consensi, con le stesse regole di POST /auth/register.
public record GoogleLoginRequest(
        @NotBlank
        @Size(max = 4096)
        String idToken,

        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Solo lettere, numeri e trattino basso.")
        String username,

        Boolean privacyAccepted,

        Boolean termsAccepted) {
}
