package it.somnia.dreamscape.api.match.service;

import java.net.http.HttpClient;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import it.somnia.dreamscape.api.common.config.EngineProperties;
import it.somnia.dreamscape.api.match.service.MatchExceptions.EngineUnavailableException;
import it.somnia.dreamscape.engine.SimulationRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Chiede la partita a engine-service con {@code POST /api/v1/simulations} e la chiave in {@code X-Engine-Key}.
 *
 * <p>Qualsiasi problema del motore (spento, oltre il tempo massimo, chiave mancante o rifiutata, errore) diventa
 * {@code 503 ENGINE_UNAVAILABLE} per il frontend; il motivo vero finisce nel log del backend.
 */
@Slf4j
@Component
public class HttpBattleSimulator implements BattleSimulator {

    static final String SIMULATIONS_PATH = "/api/v1/simulations";
    static final String KEY_HEADER = "X-Engine-Key";

    private final EngineProperties properties;
    private final RestClient restClient;

    public HttpBattleSimulator(EngineProperties properties) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public SimulationResult simulate(SimulationRequest request) {
        Objects.requireNonNull(request, "request");
        if (!properties.hasApiKey()) {
            log.error("Manca ENGINE_API_KEY: il backend non può chiamare il motore di gioco");
            throw new EngineUnavailableException();
        }
        SimulationResult result;
        try {
            result = restClient.post()
                    .uri(SIMULATIONS_PATH)
                    .header(KEY_HEADER, properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SimulationResult.class);
        } catch (RestClientResponseException exception) {
            // Il motore ha risposto con un errore: 401 = chiavi diverse, 400 = dati rotti mandati dal backend
            log.error("Il motore di gioco ha risposto {}: {}", exception.getStatusCode().value(),
                    exception.getResponseBodyAsString());
            throw new EngineUnavailableException();
        } catch (RestClientException exception) {
            // Motore spento, irraggiungibile o oltre il tempo massimo
            log.error("Motore di gioco non disponibile su {}: {}", properties.url(), exception.getMessage());
            throw new EngineUnavailableException();
        }
        if (result == null || result.endReason() == null) {
            log.error("Il motore di gioco ha risposto senza il risultato della partita");
            throw new EngineUnavailableException();
        }
        return result;
    }
}
