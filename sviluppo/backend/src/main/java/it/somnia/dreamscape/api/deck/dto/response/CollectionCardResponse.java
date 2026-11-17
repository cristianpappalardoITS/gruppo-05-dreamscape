package it.somnia.dreamscape.api.deck.dto.response;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;

// Una voce della collezione: { "card": { ... }, "quantity": 2, "availableForTrade": 1 }. La full art è una voce
// a parte. availableForTrade = copie che si possono offrire in uno scambio: quantity meno quelle già offerte negli
// scambi in attesa (decisione 024); nei mazzi si usano tutte
public record CollectionCardResponse(CardResponse card, int quantity, int availableForTrade) {
}
