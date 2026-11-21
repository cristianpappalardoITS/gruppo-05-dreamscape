package it.somnia.dreamscape.api.deck.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

// Il mazzo classificato (PUT /me/ranked-deck): quello con cui si gioca quando il matchmaking ti sceglie come
// avversario. null lo toglie
public record RankedDeckRequest(
        @Schema(description = "Un tuo mazzo valido; null per togliere il mazzo classificato",
                example = "019a2a10-1b2c-7d3e-8f40-a1b2c3d4e5f6", nullable = true) UUID deckId) {
}
