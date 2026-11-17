package it.somnia.dreamscape.api.common.exception;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

// Formato unico di ogni errore delle API (guida per il frontend, capitolo 2).
// problems c'è solo negli errori che hanno un elenco di problemi (mazzo non valido, INVALID_DECK)
public record ErrorResponse(
        @Schema(description = "Lo stato HTTP", example = "409")
        int status,

        @Schema(description = "Cosa è successo: il frontend decide con questo", example = "EMAIL_TAKEN")
        String code,

        @Schema(description = "Già in italiano: si può mostrare all'utente", example = "Questa email è già usata.")
        String message,

        @Schema(description = "Gli errori dei singoli campi, da mostrare accanto al campo; vuoto se non ce ne sono")
        List<FieldErrorResponse> fieldErrors,

        @Schema(description = "Solo per INVALID_DECK: tutti i problemi del mazzo")
        @JsonInclude(JsonInclude.Include.NON_NULL) List<String> problems) {

    public ErrorResponse(int status, String code, String message, List<FieldErrorResponse> fieldErrors) {
        this(status, code, message, fieldErrors, null);
    }

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, List.of());
    }
}
