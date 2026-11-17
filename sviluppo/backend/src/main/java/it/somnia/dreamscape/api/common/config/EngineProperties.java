package it.somnia.dreamscape.api.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Collegamento al motore di gioco (application.properties, prefisso dreamscape.engine; decisione 029).
 *
 * @param url            indirizzo di engine-service, da {@code ENGINE_URL}
 * @param apiKey         chiave condivisa mandata in {@code X-Engine-Key}, da {@code ENGINE_API_KEY}. Vuota = motore
 *                       non raggiungibile: le partite rispondono {@code 503 ENGINE_UNAVAILABLE}
 * @param connectTimeout tempo massimo per collegarsi al motore
 * @param readTimeout    tempo massimo per avere il risultato di una partita
 */
@ConfigurationProperties(prefix = "dreamscape.engine")
public record EngineProperties(String url, String apiKey, Duration connectTimeout, Duration readTimeout) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
