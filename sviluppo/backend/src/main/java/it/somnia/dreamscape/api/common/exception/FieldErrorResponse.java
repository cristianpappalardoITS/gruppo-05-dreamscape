package it.somnia.dreamscape.api.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

// Errore su un singolo campo del form: il frontend lo mostra accanto al campo
public record FieldErrorResponse(
        @Schema(example = "email") String field,
        @Schema(example = "Questa email è già usata.") String message) {
}
