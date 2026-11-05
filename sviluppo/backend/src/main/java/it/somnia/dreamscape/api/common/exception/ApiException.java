package it.somnia.dreamscape.api.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Errore previsto dalle regole del gioco o delle API. Ogni caso ha la sua sottoclasse
 * (es. {@code UsernameTakenException}) con lo stato HTTP e il codice della guida per il frontend.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final transient List<FieldErrorResponse> fieldErrors;

    protected ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    protected ApiException(HttpStatus status, String code, String message, List<FieldErrorResponse> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<FieldErrorResponse> getFieldErrors() {
        return fieldErrors;
    }

    public ErrorResponse toResponse() {
        return new ErrorResponse(status.value(), code, getMessage(), fieldErrors);
    }
}
