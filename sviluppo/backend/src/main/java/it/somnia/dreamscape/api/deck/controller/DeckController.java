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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.deck.dto.request.DeckRequest;
import it.somnia.dreamscape.api.deck.dto.response.DeckResponse;
import it.somnia.dreamscape.api.deck.service.DeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// I mazzi dell'utente collegato
@Tag(name = "Mazzi", description = "I mazzi dell'utente collegato. Ogni mazzo dice se è valido e, se no, perché "
        + "(problems).")
@RestController
@RequestMapping("/api/v1/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    @Operation(summary = "I miei mazzi", description = "In ordine di nome, ognuno con le sue carte.")
    @ApiResponse(responseCode = "200", description = "I mazzi")
    @GetMapping
    public List<DeckResponse> listDecks(@AuthenticationPrincipal Jwt jwt) {
        return deckService.listDecks(CurrentUser.id(jwt));
    }

    @Operation(summary = "Un mazzo")
    @ApiResponse(responseCode = "200", description = "Il mazzo")
    @ApiResponse(responseCode = "404", description = "DECK_NOT_FOUND: non esiste o è di un altro utente")
    @GetMapping("/{deckId}")
    public DeckResponse getDeck(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID deckId) {
        return deckService.getDeck(CurrentUser.id(jwt), deckId);
    }

    @Operation(summary = "Nuovo mazzo", description = "Nome e carte con le copie. Se la stessa carta compare due "
            + "volte, le copie si sommano.")
    @ApiResponse(responseCode = "201", description = "Mazzo creato")
    @ApiResponse(responseCode = "400", description = "INVALID_DECK (il mazzo non rispetta le regole: problems li "
            + "elenca tutti) o VALIDATION_ERROR (nome vuoto o troppo lungo, carte mancanti)")
    @ApiResponse(responseCode = "409", description = "DECK_NAME_TAKEN")
    @PostMapping
    public ResponseEntity<DeckResponse> createDeck(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.createDeck(CurrentUser.id(jwt), request));
    }

    @Operation(summary = "Modifica di un mazzo", description = "Le carte mandate sostituiscono quelle di prima.")
    @ApiResponse(responseCode = "200", description = "Mazzo aggiornato")
    @ApiResponse(responseCode = "400", description = "INVALID_DECK o VALIDATION_ERROR")
    @ApiResponse(responseCode = "404", description = "DECK_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "DECK_NAME_TAKEN")
    @PutMapping("/{deckId}")
    public DeckResponse updateDeck(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID deckId,
            @Valid @RequestBody DeckRequest request) {
        return deckService.updateDeck(CurrentUser.id(jwt), deckId, request);
    }

    @Operation(summary = "Eliminazione di un mazzo")
    @ApiResponse(responseCode = "204", description = "Mazzo eliminato")
    @ApiResponse(responseCode = "404", description = "DECK_NOT_FOUND")
    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID deckId) {
        deckService.deleteDeck(CurrentUser.id(jwt), deckId);
        return ResponseEntity.noContent().build();
    }
}
