package it.somnia.dreamscape.api.friend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.friend.dto.response.FriendResponse;
import it.somnia.dreamscape.api.friend.service.FriendService;
import lombok.RequiredArgsConstructor;

// Gli amici dell'utente collegato
@Tag(name = "Amici", description = "Amici, richieste di amicizia e ricerca dei Dreamweaver per username. Si scambia "
        + "solo con gli amici.")
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "I miei amici")
    @ApiResponse(responseCode = "200", description = "Gli amici, con la data dell'amicizia")
    @GetMapping
    public List<FriendResponse> listFriends(@AuthenticationPrincipal Jwt jwt) {
        return friendService.listFriends(CurrentUser.id(jwt));
    }

    // Annulla anche scambi e sfide in attesa tra i due (decisione 026)
    @Operation(summary = "Togli un amico", description = "Annulla anche gli scambi e le sfide in attesa tra voi due.")
    @ApiResponse(responseCode = "204", description = "Amico tolto")
    @ApiResponse(responseCode = "404", description = "FRIEND_NOT_FOUND: non è tra i tuoi amici")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeFriend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        friendService.removeFriend(CurrentUser.id(jwt), userId);
        return ResponseEntity.noContent().build();
    }
}
