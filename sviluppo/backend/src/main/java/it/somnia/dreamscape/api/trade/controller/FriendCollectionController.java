package it.somnia.dreamscape.api.trade.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.deck.dto.response.CollectionCardResponse;
import it.somnia.dreamscape.api.trade.service.TradeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

// La collezione di un amico, per scegliere le carte da chiedere in uno scambio. Stessi filtri di GET /me/cards
@RestController
@RequestMapping("/api/v1/users/{userId}/cards")
@RequiredArgsConstructor
public class FriendCollectionController {

    private final TradeService tradeService;

    @GetMapping
    public PageResponse<CollectionCardResponse> listFriendCollection(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @RequestParam(required = false) String faction,
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) @Size(max = 80) String search,
            @RequestParam(required = false) Boolean fullArt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        return tradeService.listFriendCollection(CurrentUser.id(jwt), userId, faction, rarity, search, fullArt, page,
                size);
    }
}
