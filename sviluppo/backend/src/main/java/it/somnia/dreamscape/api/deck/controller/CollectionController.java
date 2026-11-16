package it.somnia.dreamscape.api.deck.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.deck.dto.response.CollectionCardResponse;
import it.somnia.dreamscape.api.deck.service.CollectionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

// La collezione dell'utente collegato
@RestController
@RequestMapping("/api/v1/me/cards")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public PageResponse<CollectionCardResponse> listCollection(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String faction,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) @Size(max = 80) String search,
            @RequestParam(required = false) Boolean fullArt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        return collectionService.listCollection(CurrentUser.id(jwt), faction, rarity, search, fullArt, page, size);
    }
}
