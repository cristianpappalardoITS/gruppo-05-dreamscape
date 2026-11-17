package it.somnia.dreamscape.engine.server.exception;

import it.somnia.dreamscape.engine.server.dto.ErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Trasforma gli errori in risposte {@code { status, code, message }}.
 *
 * <p>Il motore rifiuta solo i dati rotti (decisione 029): JSON illeggibile, mazzo vuoto, carta con valori
 * impossibili, effetto sconosciuto. Sono tutti {@code 400 INVALID_SIMULATION}, con il motivo nel messaggio
 * perché il backend lo scriva nei suoi log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // JSON illeggibile, oppure un record del nucleo che rifiuta i valori (es. "Il mazzo è vuoto")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
        return invalid(cause.getMessage());
    }

    // Dati letti ma rifiutati dal nucleo durante la partita, es. un effetto che il motore non conosce
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleRejected(IllegalArgumentException exception) {
        return invalid(exception.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException exception) {
        return invalid("Il corpo deve essere JSON (Content-Type: application/json).");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Non trovato.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException exception) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Operazione non permessa.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Errore inatteso del motore", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "ENGINE_ERROR", "Errore inatteso del motore.");
    }

    private static ResponseEntity<ErrorResponse> invalid(String reason) {
        String message = reason == null || reason.isBlank()
                ? "Richiesta della partita non valida."
                : "Richiesta della partita non valida: " + reason;
        return error(HttpStatus.BAD_REQUEST, "INVALID_SIMULATION", message);
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), code, message));
    }
}
