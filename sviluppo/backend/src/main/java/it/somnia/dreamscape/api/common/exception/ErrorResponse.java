package it.somnia.dreamscape.api.common.exception;

import java.util.List;

// Formato unico di ogni errore delle API (guida per il frontend, capitolo 2)
public record ErrorResponse(int status, String code, String message, List<FieldErrorResponse> fieldErrors) {

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, List.of());
    }
}
