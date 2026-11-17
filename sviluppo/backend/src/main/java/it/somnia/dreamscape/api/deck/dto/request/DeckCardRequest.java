package it.somnia.dreamscape.api.deck.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Una carta del mazzo da salvare: { "cardId": "…", "quantity": 2 }. Le regole del mazzo le controlla
// DeckValidator; qui solo che i dati siano leggibili
public record DeckCardRequest(
        @Schema(description = "Id della carta, normale o full art")
        @NotNull(message = "Scegli una carta.") UUID cardId,
        @Schema(description = "Copie nel mazzo, almeno 1: i limiti per rarità sono in GET /rules", example = "2")
        @Min(value = 1, message = "Metti almeno una copia.")
        @Max(value = 20, message = "Troppe copie per una carta.") int quantity) {
}
