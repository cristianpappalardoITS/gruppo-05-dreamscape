package it.somnia.dreamscape.api.user.dto.response;

// Nuovo token di accesso (POST /auth/refresh); expiresIn in secondi
public record AccessTokenResponse(String accessToken, long expiresIn) {
}
