package it.somnia.dreamscape.api.user.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Primo admin, creato all'avvio da variabili d'ambiente (prefisso dreamscape.admin).
 * Se uno dei tre valori manca, non si crea nessun admin.
 */
@ConfigurationProperties(prefix = "dreamscape.admin")
public record AdminProperties(String username, String email, String password) {

    public boolean isConfigured() {
        return hasText(username) && hasText(email) && hasText(password);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
