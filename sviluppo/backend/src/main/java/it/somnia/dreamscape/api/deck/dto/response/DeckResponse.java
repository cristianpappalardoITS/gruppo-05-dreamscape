package it.somnia.dreamscape.api.deck.dto.response;

import java.util.List;
import java.util.UUID;

// Un mazzo come lo riceve il frontend (guida per il frontend, "Mazzi"). isValid e problems si calcolano
// a ogni lettura: un mazzo salvato valido può diventarlo meno dopo uno scambio o una carta disattivata
public record DeckResponse(
        UUID id,
        String name,
        int totalCards,
        boolean isValid,
        boolean isRanked,
        List<String> problems,
        List<DeckCardResponse> cards) {
}
