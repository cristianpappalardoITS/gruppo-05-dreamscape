package it.somnia.dreamscape.api.pack.dto.response;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;

// Una carta uscita da un pacchetto: { "card": { ... } }, come nella guida ai dati
public record OpenedCardResponse(CardResponse card) {
}
