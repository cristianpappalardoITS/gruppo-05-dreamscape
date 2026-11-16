package it.somnia.dreamscape.api.deck.dto.response;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;

// Una carta del mazzo con le sue copie: { "card": { ... }, "quantity": 2 }
public record DeckCardResponse(CardResponse card, int quantity) {
}
