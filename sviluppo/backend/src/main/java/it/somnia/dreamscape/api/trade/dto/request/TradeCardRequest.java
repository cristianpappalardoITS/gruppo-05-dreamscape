package it.somnia.dreamscape.api.trade.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Una carta dello scambio: { "cardId": "…", "quantity": 2 }. La full art si indica con il suo cardId
public record TradeCardRequest(
        @NotNull(message = "Scegli una carta.") UUID cardId,
        @Min(value = 1, message = "Metti almeno una copia.")
        @Max(value = 99, message = "Troppe copie per una carta.") int quantity) {
}
