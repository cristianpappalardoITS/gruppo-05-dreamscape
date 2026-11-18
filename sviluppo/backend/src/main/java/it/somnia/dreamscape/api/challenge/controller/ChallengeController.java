package it.somnia.dreamscape.api.challenge.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.challenge.dto.request.AcceptChallengeRequest;
import it.somnia.dreamscape.api.challenge.dto.request.ChallengeRequest;
import it.somnia.dreamscape.api.challenge.dto.response.AcceptChallengeResponse;
import it.somnia.dreamscape.api.challenge.dto.response.ChallengeResponse;
import it.somnia.dreamscape.api.challenge.service.ChallengeService;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

// Le sfide tra amici dell'utente collegato: sempre partite rapide, giocate subito all'accettazione
@Tag(name = "Sfide", description = "Sfide tra amici: partite rapide, senza rank né crediti. Al massimo una sfida "
        + "in attesa tra due amici; le sfide non scadono e chi sfida può annullarle.")
@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @Operation(summary = "Le mie sfide", description = "Solo quelle in attesa, dalla più recente. Ognuna dice se "
            + "il mazzo di chi ha sfidato si può giocare adesso (challengerDeckValid) e, se no, perché.")
    @ApiResponse(responseCode = "200", description = "Le sfide")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: box diverso da received o sent")
    @GetMapping
    public List<ChallengeResponse> listChallenges(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "received: da accettare o rifiutare; sent: lanciate da me")
            @RequestParam @Pattern(regexp = "received|sent") String box) {
        return challengeService.listChallenges(CurrentUser.id(jwt), box);
    }

    @Operation(summary = "Sfida un amico", description = "Con un mio mazzo valido. L'amico sceglie il suo mazzo "
            + "quando accetta.")
    @ApiResponse(responseCode = "201", description = "Sfida lanciata")
    @ApiResponse(responseCode = "400", description = "INVALID_DECK (il mazzo non è valido: problems li elenca), "
            + "INVALID_CHALLENGE (sfida a se stessi) o VALIDATION_ERROR")
    @ApiResponse(responseCode = "403", description = "NOT_FRIENDS: non siete amici")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND o DECK_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CHALLENGE_ALREADY_PENDING: c'è già una sfida in attesa tra "
            + "voi due")
    @PostMapping
    public ResponseEntity<ChallengeResponse> createChallenge(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChallengeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(challengeService.createChallenge(CurrentUser.id(jwt), request));
    }

    @Operation(summary = "Accetta e gioca", description = "Solo chi è stato sfidato, con un suo mazzo valido. La "
            + "partita si gioca subito: poi si va su /partite/{matchId}.")
    @ApiResponse(responseCode = "200", description = "Partita giocata: il suo id")
    @ApiResponse(responseCode = "400", description = "INVALID_DECK (il tuo mazzo non è valido) o VALIDATION_ERROR")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: è una sfida che hai lanciato tu")
    @ApiResponse(responseCode = "404", description = "CHALLENGE_NOT_FOUND o DECK_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CHALLENGER_DECK_INVALID (il mazzo di chi ha sfidato non è "
            + "più valido: la sfida resta in attesa), CHALLENGE_CANCELLED (non siete più amici: la sfida si annulla) "
            + "o CHALLENGE_NOT_PENDING")
    @ApiResponse(responseCode = "503", description = "ENGINE_UNAVAILABLE: il motore non risponde, la sfida resta in "
            + "attesa")
    @PostMapping("/{challengeId}/accept")
    public AcceptChallengeResponse acceptChallenge(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID challengeId,
            @Valid @RequestBody AcceptChallengeRequest request) {
        return challengeService.acceptChallenge(CurrentUser.id(jwt), challengeId, request);
    }

    @Operation(summary = "Rifiuta una sfida", description = "Solo chi è stato sfidato.")
    @ApiResponse(responseCode = "204", description = "Sfida rifiutata")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: è una sfida che hai lanciato tu")
    @ApiResponse(responseCode = "404", description = "CHALLENGE_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CHALLENGE_NOT_PENDING")
    @PostMapping("/{challengeId}/reject")
    public ResponseEntity<Void> rejectChallenge(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID challengeId) {
        challengeService.rejectChallenge(CurrentUser.id(jwt), challengeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Annulla una sfida", description = "Solo chi l'ha lanciata.")
    @ApiResponse(responseCode = "204", description = "Sfida annullata")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: è una sfida ricevuta")
    @ApiResponse(responseCode = "404", description = "CHALLENGE_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CHALLENGE_NOT_PENDING")
    @PostMapping("/{challengeId}/cancel")
    public ResponseEntity<Void> cancelChallenge(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID challengeId) {
        challengeService.cancelChallenge(CurrentUser.id(jwt), challengeId);
        return ResponseEntity.noContent().build();
    }
}
