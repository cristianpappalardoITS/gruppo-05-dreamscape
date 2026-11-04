package it.somnia.dreamscape.api.common.security;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

// Legge l'id dell'utente collegato dal token di accesso (claim sub)
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
