package it.somnia.dreamscape.api.trade.dto.response;

import java.util.List;

// Gli avvisi prima di proporre (POST /trades/preview) o di accettare (GET /trades/{tradeId}/deck-warnings).
// Liste vuote = niente da dire. Lo scambio si può fare lo stesso
public record TradeWarningsResponse(List<DeckWarningResponse> deckWarnings,
        List<LastCopyWarningResponse> lastCopyWarnings) {
}
