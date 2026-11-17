package it.somnia.dreamscape.api.trade.dto.response;

import java.util.UUID;

// Un mazzo che cambia con lo scambio (decisione 024). cardName è il nome della carta che esce
public record DeckWarningResponse(UUID deckId, String deckName, boolean isRanked, String cardName,
        DeckWarningOutcome outcome) {
}
