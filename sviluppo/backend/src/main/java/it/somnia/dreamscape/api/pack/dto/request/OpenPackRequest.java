package it.somnia.dreamscape.api.pack.dto.request;

import java.util.UUID;

// Acquisto di un pacchetto: la copertina scelta, null se il pacchetto non ne ha
public record OpenPackRequest(UUID coverId) {
}
