package it.somnia.dreamscape.api.card.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.card.dto.response.CardResponse;
import it.somnia.dreamscape.api.card.dto.response.FactionResponse;
import it.somnia.dreamscape.api.card.dto.response.RulesResponse;
import it.somnia.dreamscape.api.card.service.CatalogService;
import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

// Catalogo pubblico: carte, fazioni e regolamento, anche senza accesso (SecurityConfig)
@Tag(name = "Catalogo", description = "Le carte attive, le fazioni e il regolamento: pagine pubbliche, "
        + "non serve il token.")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @Operation(summary = "Carte del catalogo", description = "Solo le carte normali attive, in ordine di codice. "
            + "I filtri sono tutti facoltativi.")
    @ApiResponse(responseCode = "200", description = "Una pagina di carte")
    @ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR: search troppo lungo, page o size fuori dai limiti")
    @GetMapping("/cards")
    public PageResponse<CardResponse> listCards(
            @Parameter(description = "Codice della fazione, es. DREAMS") @RequestParam(required = false) String faction,
            @Parameter(description = "Codice della rarità, es. LEGENDARY")
            @RequestParam(required = false) String rarity,
            @Parameter(description = "Parte del nome della carta")
            @RequestParam(required = false) @Size(max = 80) String search,
            @Parameter(description = "true: solo le carte in vetrina della Home (le Leggendarie)")
            @RequestParam(defaultValue = "false") boolean featured,
            @Parameter(description = "Numero della pagina, da 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Carte per pagina, da 1 a 100")
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        return catalogService.listCards(faction, rarity, search, featured, page, size);
    }

    @Operation(summary = "Una carta", description = "Anche una full art, con nome, statistiche ed effetto della "
            + "carta normale.")
    @ApiResponse(responseCode = "200", description = "La carta")
    @ApiResponse(responseCode = "404", description = "CARD_NOT_FOUND")
    @GetMapping("/cards/{cardId}")
    public CardResponse getCard(@PathVariable UUID cardId) {
        return catalogService.getCard(cardId);
    }

    @Operation(summary = "Fazioni", description = "Le tre fazioni: descrizione, Lord e contro chi sono forti o deboli.")
    @ApiResponse(responseCode = "200", description = "Le fazioni")
    @GetMapping("/factions")
    public List<FactionResponse> listFactions() {
        return catalogService.listFactions();
    }

    @Operation(summary = "Regolamento", description = "Carte per mazzo, punti vita, limite di round, copie "
            + "per rarità e vantaggi tra fazioni.")
    @ApiResponse(responseCode = "200", description = "Il regolamento")
    @GetMapping("/rules")
    public RulesResponse getRules() {
        return catalogService.getRules();
    }
}
