package it.somnia.dreamscape.api.deck.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.deck.dto.response.CollectionCardResponse;
import it.somnia.dreamscape.api.deck.service.CollectionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

// La collezione dell'utente collegato
@Tag(name = "Collezione", description = "Le carte possedute dall'utente collegato, con il numero di copie.")
@RestController
@RequestMapping("/api/v1/me/cards")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @Operation(summary = "La mia collezione", description = "In ordine di codice: la full art viene subito dopo la "
            + "sua carta normale. Ci sono anche le carte disattivate. I filtri sono tutti facoltativi.")
    @ApiResponse(responseCode = "200", description = "Una pagina della collezione")
    @ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR: search troppo lungo, page o size fuori dai limiti")
    @GetMapping
    public PageResponse<CollectionCardResponse> listCollection(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Codice della fazione, es. DREAMS") @RequestParam(required = false) String faction,
            @Parameter(description = "Codice della rarità, es. RARE") @RequestParam(required = false) String rarity,
            @Parameter(description = "Parte del nome della carta")
            @RequestParam(required = false) @Size(max = 80) String search,
            @Parameter(description = "true solo le full art, false solo le carte normali, senza il parametro tutte")
            @RequestParam(required = false) Boolean fullArt,
            @Parameter(description = "Numero della pagina, da 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Carte per pagina, da 1 a 100")
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        return collectionService.listCollection(CurrentUser.id(jwt), faction, rarity, search, fullArt, page, size);
    }
}
