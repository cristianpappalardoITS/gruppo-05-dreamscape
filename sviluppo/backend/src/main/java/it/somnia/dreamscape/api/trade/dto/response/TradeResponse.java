package it.somnia.dreamscape.api.trade.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import it.somnia.dreamscape.api.trade.model.TradeStatus;
import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;

// Uno scambio: chi propone, chi riceve, le carte dei due lati e lo stato. resolvedAt è null finché è in attesa
public record TradeResponse(
        UUID id,
        TradeStatus status,
        UserSummaryResponse proposer,
        UserSummaryResponse recipient,
        String message,
        List<TradeCardResponse> offered,
        List<TradeCardResponse> requested,
        Instant createdAt,
        Instant resolvedAt) {
}
