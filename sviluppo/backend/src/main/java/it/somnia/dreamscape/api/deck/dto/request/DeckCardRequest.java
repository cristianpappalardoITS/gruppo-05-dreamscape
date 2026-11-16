package it.somnia.dreamscape.api.deck.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Una carta del mazzo da salvare: { "cardId": "…", "quantity": 2 }. Le regole del mazzo le controlla
// DeckValidator; qui solo che i dati siano leggibili
public record DeckCardRequest(
        @NotNull(message = "Scegli una carta.") UUID cardId,
        @Min(value = 1, message = "Metti almeno una copia.")
        @Max(value = 20, message = "Troppe copie per una carta.") int quantity) {
}
