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

import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.friend.dto.response.FriendResponse;
import it.somnia.dreamscape.api.friend.service.FriendService;
import lombok.RequiredArgsConstructor;

// Gli amici dell'utente collegato
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public List<FriendResponse> listFriends(@AuthenticationPrincipal Jwt jwt) {
        return friendService.listFriends(CurrentUser.id(jwt));
    }

    // Annulla anche scambi e sfide in attesa tra i due (decisione 026)
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeFriend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        friendService.removeFriend(CurrentUser.id(jwt), userId);
        return ResponseEntity.noContent().build();
    }
}
