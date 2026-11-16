package it.somnia.dreamscape.api.common.exception;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// Formato unico di ogni errore delle API (guida per il frontend, capitolo 2).
// problems c'è solo negli errori che hanno un elenco di problemi (mazzo non valido, INVALID_DECK)
public record ErrorResponse(int status, String code, String message, List<FieldErrorResponse> fieldErrors,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<String> problems) {

    public ErrorResponse(int status, String code, String message, List<FieldErrorResponse> fieldErrors) {
        this(status, code, message, fieldErrors, null);
    }

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, List.of());
    }
}
