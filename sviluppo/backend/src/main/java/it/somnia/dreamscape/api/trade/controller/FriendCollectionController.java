package it.somnia.dreamscape.api.trade.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.deck.dto.response.CollectionCardResponse;
import it.somnia.dreamscape.api.trade.service.TradeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

// La collezione di un amico, per scegliere le carte da chiedere in uno scambio. Stessi filtri di GET /me/cards
@Tag(name = "Scambi")
@RestController
@RequestMapping("/api/v1/users/{userId}/cards")
@RequiredArgsConstructor
public class FriendCollectionController {

    private final TradeService tradeService;

    @Operation(summary = "Collezione di un amico", description = "Per scegliere le carte da chiedere: "
            + "availableForTrade dice quante copie l'amico può dare. Stessi filtri e pagine di GET /me/cards.")
    @ApiResponse(responseCode = "200", description = "Una pagina della collezione dell'amico")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: filtri o pagine fuori dai limiti")
    @ApiResponse(responseCode = "403", description = "NOT_FRIENDS: non siete amici")
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
