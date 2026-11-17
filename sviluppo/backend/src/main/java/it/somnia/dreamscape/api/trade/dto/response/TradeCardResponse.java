package it.somnia.dreamscape.api.trade.dto.response;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;

// Una carta dello scambio con le sue copie: { "card": { ... }, "quantity": 2 }
public record TradeCardResponse(CardResponse card, int quantity) {
}
