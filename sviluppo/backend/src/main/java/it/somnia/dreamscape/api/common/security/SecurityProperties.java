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
        // Senza la variabile, Spring lascia il testo "${JWT_SECRET}" così com'è
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.startsWith("${")) {
            throw new IllegalStateException("Manca JWT_SECRET: impostala come variabile d'ambiente, "
                    + "oppure avvia con il profilo dev (./gradlew bootRun).");
        }
        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET è troppo corta: servono almeno " + MIN_SECRET_LENGTH
                    + " caratteri (es. openssl rand -base64 48).");
        }
    }
}
