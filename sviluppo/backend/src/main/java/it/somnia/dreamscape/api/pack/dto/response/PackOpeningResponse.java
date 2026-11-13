package it.somnia.dreamscape.api.pack.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Un'apertura di un pacchetto, letta dalla pagina /negozio/apertura/{openingId}: le carte sono nell'ordine
// in cui sono uscite
public record PackOpeningResponse(UUID openingId, String packName, Instant openedAt, List<OpenedCardResponse> cards) {
}
