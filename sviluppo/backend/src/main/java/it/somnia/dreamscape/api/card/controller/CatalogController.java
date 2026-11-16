package it.somnia.dreamscape.api.card.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/cards")
    public PageResponse<CardResponse> listCards(
            @RequestParam(required = false) String faction,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) @Size(max = 80) String search,
            @RequestParam(defaultValue = "false") boolean featured,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        return catalogService.listCards(faction, rarity, search, featured, page, size);
    }

    @GetMapping("/cards/{cardId}")
    public CardResponse getCard(@PathVariable UUID cardId) {
        return catalogService.getCard(cardId);
    }

    @GetMapping("/factions")
    public List<FactionResponse> listFactions() {
        return catalogService.listFactions();
    }

    @GetMapping("/rules")
    public RulesResponse getRules() {
        return catalogService.getRules();
    }
}
