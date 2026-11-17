package it.somnia.dreamscape.engine.server.dto;

/**
 * Un errore di engine-service, nello stesso formato degli errori del backend (senza {@code fieldErrors}).
 *
 * @param status  il codice HTTP
 * @param code    il codice stabile dell'errore, per esempio {@code INVALID_ENGINE_KEY}
 * @param message la spiegazione, per i log del backend
 */
public record ErrorResponse(int status, String code, String message) {
}
