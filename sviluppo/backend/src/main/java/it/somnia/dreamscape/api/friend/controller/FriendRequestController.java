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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.friend.dto.request.SendFriendRequestRequest;
import it.somnia.dreamscape.api.friend.dto.response.FriendRequestResponse;
import it.somnia.dreamscape.api.friend.dto.response.FriendResponse;
import it.somnia.dreamscape.api.friend.service.FriendService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

// Le richieste di amicizia ricevute e mandate
@Tag(name = "Amici")
@RestController
@RequestMapping("/api/v1/friend-requests")
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendService friendService;

    @Operation(summary = "Richieste di amicizia", description = "Le richieste in attesa, dalla più recente: user è "
            + "l'altro giocatore.")
    @ApiResponse(responseCode = "200", description = "Le richieste")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: direction diverso da received o sent")
    @GetMapping
    public List<FriendRequestResponse> listRequests(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "received: ricevute, sent: mandate")
            @RequestParam @Pattern(regexp = "received|sent") String direction) {
        return friendService.listRequests(CurrentUser.id(jwt), "received".equals(direction));
    }

    // 201 con la richiesta; 200 con l'amico se l'altro aveva già chiesto l'amicizia (accettata subito)
    @Operation(summary = "Manda una richiesta di amicizia", description = "Per username. Se l'altro ti aveva già "
            + "chiesto l'amicizia, siete subito amici.")
    @ApiResponse(responseCode = "201", description = "Richiesta mandata",
            content = @Content(schema = @Schema(implementation = FriendRequestResponse.class)))
    @ApiResponse(responseCode = "200", description = "Eravate già in attesa l'uno dell'altro: siete amici",
            content = @Content(schema = @Schema(implementation = FriendResponse.class)))
    @ApiResponse(responseCode = "400", description = "CANNOT_FRIEND_YOURSELF o VALIDATION_ERROR")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "ALREADY_FRIENDS o FRIEND_REQUEST_ALREADY_SENT")
    @PostMapping
    public ResponseEntity<Object> sendRequest(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SendFriendRequestRequest request) {
        FriendService.SendResult result = friendService.sendRequest(CurrentUser.id(jwt), request.username());
        if (result.isAccepted()) {
            return ResponseEntity.ok(result.friend());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.request());
    }

    @Operation(summary = "Accetta una richiesta", description = "Solo chi l'ha ricevuta.")
    @ApiResponse(responseCode = "200", description = "Il nuovo amico")
    @ApiResponse(responseCode = "404", description = "FRIEND_REQUEST_NOT_FOUND: non esiste o non ti riguarda")
    @PostMapping("/{requestId}/accept")
    public FriendResponse acceptRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        return friendService.acceptRequest(CurrentUser.id(jwt), requestId);
    }

    // Rifiuta una richiesta ricevuta o annulla una richiesta mandata
    @Operation(summary = "Rifiuta o annulla una richiesta", description = "Rifiuta una richiesta ricevuta, annulla una "
            + "richiesta mandata.")
    @ApiResponse(responseCode = "204", description = "Richiesta tolta")
    @ApiResponse(responseCode = "404", description = "FRIEND_REQUEST_NOT_FOUND")
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deleteRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        friendService.deleteRequest(CurrentUser.id(jwt), requestId);
        return ResponseEntity.noContent().build();
    }
}
