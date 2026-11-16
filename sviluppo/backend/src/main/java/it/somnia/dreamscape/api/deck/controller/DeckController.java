package it.somnia.dreamscape.api.deck.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.deck.dto.request.DeckRequest;
import it.somnia.dreamscape.api.deck.dto.response.DeckResponse;
import it.somnia.dreamscape.api.deck.service.DeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// I mazzi dell'utente collegato
@RestController
@RequestMapping("/api/v1/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    @GetMapping
    public List<DeckResponse> listDecks(@AuthenticationPrincipal Jwt jwt) {
        return deckService.listDecks(CurrentUser.id(jwt));
    }

    @GetMapping("/{deckId}")
    public DeckResponse getDeck(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID deckId) {
        return deckService.getDeck(CurrentUser.id(jwt), deckId);
    }

    @PostMapping
    public ResponseEntity<DeckResponse> createDeck(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.createDeck(CurrentUser.id(jwt), request));
    }

    @PutMapping("/{deckId}")
    public DeckResponse updateDeck(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID deckId,
            @Valid @RequestBody DeckRequest request) {
        return deckService.updateDeck(CurrentUser.id(jwt), deckId, request);
    }

    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID deckId) {
        deckService.deleteDeck(CurrentUser.id(jwt), deckId);
        return ResponseEntity.noContent().build();
    }
}
