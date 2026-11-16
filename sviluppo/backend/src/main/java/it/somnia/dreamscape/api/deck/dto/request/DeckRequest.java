package it.somnia.dreamscape.api.deck.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Nuovo mazzo o modifica (POST /decks, PUT /decks/{deckId}): nome e carte
public record DeckRequest(
        @NotBlank(message = "Dai un nome al mazzo.")
        @Size(max = 40, message = "Il nome può avere al massimo 40 caratteri.") String name,
        @NotNull(message = "Scegli le carte del mazzo.")
        @Size(max = 40, message = "Troppe carte diverse.") List<@NotNull @Valid DeckCardRequest> cards) {
}
