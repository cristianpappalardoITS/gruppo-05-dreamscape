package it.somnia.dreamscape.api.deck.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Nuovo mazzo o modifica (POST /decks, PUT /decks/{deckId}): nome e carte
public record DeckRequest(
        @Schema(description = "Unico tra i mazzi dell'utente, al massimo 40 caratteri", example = "Incubi veloci")
        @NotBlank(message = "Dai un nome al mazzo.")
        @Size(max = 40, message = "Il nome può avere al massimo 40 caratteri.") String name,
        @Schema(description = "Le carte con le copie: la stessa carta due volte somma le copie")
        @NotNull(message = "Scegli le carte del mazzo.")
        @Size(max = 40, message = "Troppe carte diverse.") List<@NotNull @Valid DeckCardRequest> cards) {
}
