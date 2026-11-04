package it.somnia.dreamscape.api.common.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Cookie del refresh token: HttpOnly (il JavaScript non lo legge), SameSite=Strict e valido
 * solo per le API (/api/v1): serve a /auth/refresh e /auth/logout, e al cambio password
 * (/me/password) per lasciare collegato il dispositivo che lo chiede.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookies {

    public static final String NAME = "dreamscape_refresh";
    private static final String PATH = "/api/v1";

    private final SecurityProperties securityProperties;

    public ResponseCookie create(String token) {
        return build(token, securityProperties.refreshTokenTtl());
    }

    // Cookie vuoto e già scaduto: il browser cancella quello vecchio
    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(securityProperties.refreshCookieSecure())
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
