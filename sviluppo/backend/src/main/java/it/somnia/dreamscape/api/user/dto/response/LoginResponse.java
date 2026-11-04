package it.somnia.dreamscape.api.user.dto.response;

// Risposta dell'accesso: il token va tenuto solo in memoria; il refresh token arriva nel cookie
public record LoginResponse(String accessToken, long expiresIn, UserResponse user) {
}
