package it.somnia.dreamscape.engine.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurazione di engine-service (application.properties, prefisso {@code dreamscape.engine}).
 *
 * @param apiKey la chiave condivisa con il backend, dalla variabile d'ambiente {@code ENGINE_API_KEY}
 */
@ConfigurationProperties(prefix = "dreamscape.engine")
public record EngineProperties(String apiKey) {

    /**
     * Ferma l'avvio se manca la chiave: un motore senza chiave giocherebbe partite per chiunque.
     *
     * @throws IllegalStateException se la chiave è vuota
     */
    public EngineProperties {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Manca la variabile d'ambiente ENGINE_API_KEY: è la chiave che il backend manda in X-Engine-Key");
        }
    }
}
