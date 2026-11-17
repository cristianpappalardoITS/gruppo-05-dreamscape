package it.somnia.dreamscape.api.pack.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

// Acquisto di un pacchetto: la copertina scelta, null se il pacchetto non ne ha
public record OpenPackRequest(
        @Schema(description = "Id di una delle covers del pacchetto; si omette se il pacchetto non ne ha")
        UUID coverId) {
}
