package it.somnia.dreamscape.api.common.exception;

// Errore su un singolo campo del form: il frontend lo mostra accanto al campo
public record FieldErrorResponse(String field, String message) {
}
