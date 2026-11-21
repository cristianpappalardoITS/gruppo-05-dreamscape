package it.somnia.dreamscape.api.deck.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.deck.dto.request.RankedDeckRequest;
import it.somnia.dreamscape.api.deck.service.DeckService;
import lombok.RequiredArgsConstructor;

// Il mazzo classificato dell'utente collegato: sta in /me perché è una scelta del giocatore, non del mazzo
@Tag(name = "Mazzi")
@RestController
@RequestMapping("/api/v1/me/ranked-deck")
@RequiredArgsConstructor
public class RankedDeckController {

    private final DeckService deckService;

    @Operation(summary = "Scelta del mazzo classificato", description = "Il mazzo con cui giochi quando il "
            + "matchmaking ti sceglie come avversario, anche se non sei collegato. Solo un tuo mazzo valido; con "
            + "deckId null lo togli e nessuno ti sceglie più. Nella lista dei mazzi è quello con isRanked true.")
    @ApiResponse(responseCode = "204", description = "Mazzo classificato scelto o tolto")
    @ApiResponse(responseCode = "400", description = "INVALID_DECK: il mazzo non è valido (problems li elenca)")
    @ApiResponse(responseCode = "404", description = "DECK_NOT_FOUND: non esiste o è di un altro utente")
    @PutMapping
    public ResponseEntity<Void> setRankedDeck(@AuthenticationPrincipal Jwt jwt,
            @RequestBody RankedDeckRequest request) {
        deckService.setRankedDeck(CurrentUser.id(jwt), request.deckId());
        return ResponseEntity.noContent().build();
    }
}
