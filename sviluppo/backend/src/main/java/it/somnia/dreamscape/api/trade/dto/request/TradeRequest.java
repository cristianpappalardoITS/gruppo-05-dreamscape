package it.somnia.dreamscape.api.trade.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Proposta di scambio (POST /trades e POST /trades/preview): offered = le carte che do, requested = quelle
// che chiedo all'amico. Almeno una carta per lato (schema.md, sezione Scambi)
public record TradeRequest(
        @NotNull(message = "Scegli l'amico.") UUID recipientId,
        @Size(max = 255, message = "Il messaggio può avere al massimo 255 caratteri.") String message,
        @NotEmpty(message = "Offri almeno una carta.")
        @Size(max = 40, message = "Troppe carte diverse.") List<@NotNull @Valid TradeCardRequest> offered,
        @NotEmpty(message = "Chiedi almeno una carta.")
        @Size(max = 40, message = "Troppe carte diverse.") List<@NotNull @Valid TradeCardRequest> requested) {
}
