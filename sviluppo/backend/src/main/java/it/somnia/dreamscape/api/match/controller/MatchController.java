package it.somnia.dreamscape.api.match.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.match.dto.response.MatchResponse;
import it.somnia.dreamscape.api.match.service.MatchService;
import lombok.RequiredArgsConstructor;

// Il risultato di una partita: la pagina /partite/{matchId} lo rilegge con l'id nell'URL
@Tag(name = "Partite", description = "Partite giocate dal server con le regole della Base: risultato e replay "
        + "round per round. Le partite nascono dalle sfide tra amici (e dal matchmaking, #127).")
@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "Una partita", description = "Risultato, round con gli eventi in ordine per il replay e "
            + "copie dei due mazzi com'erano al momento della partita. Solo per i due giocatori.")
    @ApiResponse(responseCode = "200", description = "La partita")
    @ApiResponse(responseCode = "404", description = "MATCH_NOT_FOUND: non esiste o non l'hai giocata")
    @GetMapping("/{matchId}")
    public MatchResponse getMatch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID matchId) {
        return matchService.getMatch(CurrentUser.id(jwt), matchId);
    }
}
