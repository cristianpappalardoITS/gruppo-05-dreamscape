package it.somnia.dreamscape.engine.server.config;

import it.somnia.dreamscape.engine.server.dto.ErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;

/**
 * Lascia passare verso le API solo le richieste con la chiave del backend nell'intestazione
 * {@code X-Engine-Key} (decisione 029): nessuno può giocare partite saltando il backend.
 */
@Component
public class EngineKeyFilter extends OncePerRequestFilter {

    /** L'intestazione con la chiave condivisa. */
    public static final String KEY_HEADER = "X-Engine-Key";

    private final byte[] expectedKey;
    private final ObjectMapper objectMapper;

    public EngineKeyFilter(EngineProperties properties, ObjectMapper objectMapper) {
        this.expectedKey = properties.apiKey().getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(KEY_HEADER);
        // Confronto a tempo costante: dal tempo di risposta non si capisce quanta parte della chiave è giusta
        if (key == null || !MessageDigest.isEqual(expectedKey, key.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                    "INVALID_ENGINE_KEY", "Chiave del motore mancante o sbagliata (" + KEY_HEADER + ")."));
            return;
        }
        chain.doFilter(request, response);
    }
}
