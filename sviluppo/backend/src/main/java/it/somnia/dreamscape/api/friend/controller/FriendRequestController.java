package it.somnia.dreamscape.api.friend.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.friend.dto.request.SendFriendRequestRequest;
import it.somnia.dreamscape.api.friend.dto.response.FriendRequestResponse;
import it.somnia.dreamscape.api.friend.dto.response.FriendResponse;
import it.somnia.dreamscape.api.friend.service.FriendService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

// Le richieste di amicizia ricevute e mandate
@RestController
@RequestMapping("/api/v1/friend-requests")
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendService friendService;

    @GetMapping
    public List<FriendRequestResponse> listRequests(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @Pattern(regexp = "received|sent") String direction) {
        return friendService.listRequests(CurrentUser.id(jwt), "received".equals(direction));
    }

    // 201 con la richiesta; 200 con l'amico se l'altro aveva già chiesto l'amicizia (accettata subito)
    @PostMapping
    public ResponseEntity<Object> sendRequest(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SendFriendRequestRequest request) {
        FriendService.SendResult result = friendService.sendRequest(CurrentUser.id(jwt), request.username());
        if (result.isAccepted()) {
            return ResponseEntity.ok(result.friend());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.request());
    }

    @PostMapping("/{requestId}/accept")
    public FriendResponse acceptRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        return friendService.acceptRequest(CurrentUser.id(jwt), requestId);
    }

    // Rifiuta una richiesta ricevuta o annulla una richiesta mandata
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deleteRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        friendService.deleteRequest(CurrentUser.id(jwt), requestId);
        return ResponseEntity.noContent().build();
    }
}
