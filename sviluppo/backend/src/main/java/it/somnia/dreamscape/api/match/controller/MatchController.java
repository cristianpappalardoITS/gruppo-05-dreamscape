package it.somnia.dreamscape.api.match.controller;

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
import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.match.dto.request.MatchmakingRequest;
import it.somnia.dreamscape.api.match.dto.response.MatchResponse;
import it.somnia.dreamscape.api.match.dto.response.MatchSummaryResponse;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.match.service.MatchService;
import it.somnia.dreamscape.api.match.service.MatchmakingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

// Le partite: matchmaking (classificata e rapida), risultato con il replay e storico dell'utente collegato
@Tag(name = "Partite", description = "Partite giocate subito dal server con le regole della Base: classificate e "
        + "rapide con il matchmaking (l'avversario non deve essere collegato: gioca con il suo mazzo classificato), "
        + "sfide tra amici, risultato con il replay e storico.")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final MatchmakingService matchmakingService;

    @Operation(summary = "Gioca una classificata", description = "Con un tuo mazzo valido. Il matchmaking sceglie "
            + "chi ha il punteggio più vicino (entro 100, poi 200, poi 400 punti, poi il più vicino). Cambia il rank "
            + "di tutti e due (al massimo 3 partite al giorno contro lo stesso avversario); se vinci prendi 30 DC, "
            + "al massimo 3 vittorie premiate al giorno e niente DC se l'avversario è un amico.")
    @ApiResponse(responseCode = "201", description = "La partita giocata, come GET /matches/{matchId}")
    @ApiResponse(responseCode = "400", description = "INVALID_DECK (problems li elenca) o VALIDATION_ERROR")
    @ApiResponse(responseCode = "404", description = "DECK_NOT_FOUND: non esiste o è di un altro utente")
    @ApiResponse(responseCode = "409", description = "NO_OPPONENT_FOUND: nessuno con un mazzo classificato valido")
    @ApiResponse(responseCode = "503", description = "ENGINE_UNAVAILABLE: la partita non si gioca")
    @PostMapping("/matches/ranked")
    public ResponseEntity<MatchResponse> playRanked(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchmakingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchmakingService.playRanked(CurrentUser.id(jwt), request.deckId()));
    }

    @Operation(summary = "Gioca una rapida", description = "Con un tuo mazzo valido, contro un avversario a caso. "
            + "Niente rank né crediti.")
    @ApiResponse(responseCode = "201", description = "La partita giocata, come GET /matches/{matchId}")
    @ApiResponse(responseCode = "400", description = "INVALID_DECK (problems li elenca) o VALIDATION_ERROR")
    @ApiResponse(responseCode = "404", description = "DECK_NOT_FOUND: non esiste o è di un altro utente")
    @ApiResponse(responseCode = "409", description = "NO_OPPONENT_FOUND: nessuno con un mazzo classificato valido")
    @ApiResponse(responseCode = "503", description = "ENGINE_UNAVAILABLE: la partita non si gioca")
    @PostMapping("/matches/casual")
    public ResponseEntity<MatchResponse> playCasual(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MatchmakingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchmakingService.playCasual(CurrentUser.id(jwt), request.deckId()));
    }

    @Operation(summary = "Una partita", description = "Risultato, round con gli eventi in ordine per il replay e "
            + "copie dei due mazzi com'erano al momento della partita. Solo per i due giocatori.")
    @ApiResponse(responseCode = "200", description = "La partita")
    @ApiResponse(responseCode = "404", description = "MATCH_NOT_FOUND: non esiste o non l'hai giocata")
    @GetMapping("/matches/{matchId}")
    public MatchResponse getMatch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID matchId) {
        return matchService.getMatch(CurrentUser.id(jwt), matchId);
    }

    @Operation(summary = "Il mio storico", description = "Le partite giocate, dalla più recente: anche quelle in "
            + "cui sei stato scelto come avversario. Ognuna vista da te: avversario, risultato e variazione del tuo "
            + "punteggio.")
    @ApiResponse(responseCode = "200", description = "Una pagina di partite")
    @ApiResponse(responseCode = "400", description = "MALFORMED_REQUEST (type diverso da RANKED e CASUAL) o "
            + "VALIDATION_ERROR (page o size fuori dai limiti)")
    @GetMapping("/me/matches")
    public PageResponse<MatchSummaryResponse> listMatches(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Facoltativo: RANKED o CASUAL") @RequestParam(required = false) MatchType type,
            @Parameter(description = "Numero della pagina, da 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Partite per pagina, da 1 a 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return matchService.listMatches(CurrentUser.id(jwt), type, page, size);
    }
}
