package it.somnia.dreamscape.api.user.dto.response;

// Risposta 202 di POST /auth/google: l'account Google non ha ancora un account Dreamscape.
// Il frontend mostra il piccolo form con l'username proposto e i consensi, poi rimanda la richiesta.
public record GoogleSignupResponse(boolean signupRequired, String email, String suggestedUsername) {
}
