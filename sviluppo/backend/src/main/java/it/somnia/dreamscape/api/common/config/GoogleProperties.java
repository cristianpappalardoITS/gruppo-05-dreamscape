package it.somnia.dreamscape.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Accesso con Google (application.properties, prefisso dreamscape.google; decisione 032).
 *
 * @param clientId il client id OAuth del progetto su Google Cloud, da {@code GOOGLE_CLIENT_ID}. Vuoto = accesso
 *                 con Google spento: {@code POST /auth/google} risponde {@code 503 GOOGLE_LOGIN_DISABLED}
 */
@ConfigurationProperties(prefix = "dreamscape.google")
public record GoogleProperties(String clientId) {

    public boolean isEnabled() {
        return clientId != null && !clientId.isBlank();
    }
}
