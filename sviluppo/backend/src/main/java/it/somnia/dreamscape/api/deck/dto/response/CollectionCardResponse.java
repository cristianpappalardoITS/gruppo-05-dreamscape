package it.somnia.dreamscape.api.deck.dto.response;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;

// Una voce della collezione: { "card": { ... }, "quantity": 2 }. La full art è una voce a parte
public record CollectionCardResponse(CardResponse card, int quantity) {
}
