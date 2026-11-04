package it.somnia.dreamscape.api.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurazione dell'accesso (prefisso dreamscape.security).
 *
 * @param jwtSecret             chiave HMAC del token di accesso, almeno 32 caratteri, da JWT_SECRET
 * @param accessTokenTtl        durata del token di accesso (15 minuti)
 * @param refreshTokenTtl       durata del refresh token (7 giorni)
 * @param passwordResetTokenTtl durata del link di recupero password (1 ora)
 * @param refreshCookieSecure   cookie del refresh token solo su HTTPS
 */
@ConfigurationProperties(prefix = "dreamscape.security")
public record SecurityProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration passwordResetTokenTtl,
        boolean refreshCookieSecure) {

    private static final int MIN_SECRET_LENGTH = 32;

    public SecurityProperties {
        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET deve avere almeno " + MIN_SECRET_LENGTH + " caratteri.");
        }
    }
}
