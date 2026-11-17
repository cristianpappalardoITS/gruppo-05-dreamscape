package it.somnia.dreamscape.api.trade.controller;

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
import it.somnia.dreamscape.api.trade.dto.request.TradeRequest;
import it.somnia.dreamscape.api.trade.dto.response.TradeResponse;
import it.somnia.dreamscape.api.trade.dto.response.TradeWarningsResponse;
import it.somnia.dreamscape.api.trade.service.TradeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

// Gli scambi dell'utente collegato, solo con gli amici
@Tag(name = "Scambi", description = "Scambi di carte tra amici, con gli avvisi su mazzi e ultime copie. Quantità e "
        + "rarità sono libere: decide chi riceve.")
@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    // box: received = in attesa di una mia risposta, sent = proposti da me, history = chiusi
    @Operation(summary = "I miei scambi", description = "Dal più recente: received in attesa di una tua risposta, "
            + "sent proposti da te e in attesa, history chiusi.")
    @ApiResponse(responseCode = "200", description = "Una pagina di scambi")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: box, page o size non validi")
    @GetMapping
    public PageResponse<TradeResponse> listTrades(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "received, sent o history")
            @RequestParam @Pattern(regexp = "received|sent|history") String box,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return tradeService.listTrades(CurrentUser.id(jwt), box, page, size);
    }

    @Operation(summary = "Uno scambio")
    @ApiResponse(responseCode = "200", description = "Lo scambio")
    @ApiResponse(responseCode = "404", description = "TRADE_NOT_FOUND: non esiste o non ti riguarda")
    @GetMapping("/{tradeId}")
    public TradeResponse getTrade(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tradeId) {
        return tradeService.getTrade(CurrentUser.id(jwt), tradeId);
    }

    @Operation(summary = "Proponi uno scambio", description = "Solo a un amico, con almeno una carta per lato. Le "
            + "copie offerte restano bloccate finché lo scambio è in attesa.")
    @ApiResponse(responseCode = "201", description = "Scambio proposto")
    @ApiResponse(responseCode = "400", description = "INVALID_TRADE (con se stessi, stessa carta offerta e richiesta) "
            + "o VALIDATION_ERROR")
    @ApiResponse(responseCode = "403", description = "NOT_FRIENDS")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND o CARD_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CARDS_NOT_OWNED, CARDS_LOCKED_IN_TRADE o "
            + "REQUESTED_CARDS_NOT_AVAILABLE")
    @PostMapping
    public ResponseEntity<TradeResponse> proposeTrade(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.proposeTrade(CurrentUser.id(jwt), request));
    }

    // Avvisi prima di proporre: stesso corpo di POST /trades, non salva niente
    @Operation(summary = "Avvisi prima di proporre", description = "Stesso corpo e stessi errori di POST /trades, ma "
            + "non salva niente: mazzi che diventano non validi e ultime copie che escono.")
    @ApiResponse(responseCode = "200", description = "Gli avvisi; liste vuote = niente da dire")
    @ApiResponse(responseCode = "400", description = "INVALID_TRADE o VALIDATION_ERROR")
    @ApiResponse(responseCode = "403", description = "NOT_FRIENDS")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND o CARD_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CARDS_NOT_OWNED, CARDS_LOCKED_IN_TRADE o "
            + "REQUESTED_CARDS_NOT_AVAILABLE")
    @PostMapping("/preview")
    public TradeWarningsResponse previewTrade(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TradeRequest request) {
        return tradeService.previewTrade(CurrentUser.id(jwt), request);
    }

    // Avvisi di uno scambio in attesa, per chi lo guarda (di solito chi sta per accettare)
    @Operation(summary = "Avvisi di uno scambio in attesa", description = "Per chi lo guarda, di solito chi sta per "
            + "accettare: le carte che darebbe.")
    @ApiResponse(responseCode = "200", description = "Gli avvisi")
    @ApiResponse(responseCode = "404", description = "TRADE_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "TRADE_NOT_PENDING: scambio già chiuso")
    @GetMapping("/{tradeId}/deck-warnings")
    public TradeWarningsResponse getTradeWarnings(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tradeId) {
        return tradeService.getTradeWarnings(CurrentUser.id(jwt), tradeId);
    }

    // Solo chi riceve lo scambio: sposta le carte
    @Operation(summary = "Accetta uno scambio", description = "Solo chi lo riceve. Le carte si spostano subito e i "
            + "mazzi passano alla versione normale dove annunciato.")
    @ApiResponse(responseCode = "200", description = "Scambio accettato")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: non sei chi lo riceve")
    @ApiResponse(responseCode = "404", description = "TRADE_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "TRADE_NOT_PENDING, REQUESTED_CARDS_NOT_AVAILABLE (lo scambio "
            + "resta in attesa) o TRADE_CANCELLED (non siete più amici)")
    @PostMapping("/{tradeId}/accept")
    public TradeResponse acceptTrade(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tradeId) {
        return tradeService.acceptTrade(CurrentUser.id(jwt), tradeId);
    }

    // Solo chi riceve lo scambio
    @Operation(summary = "Rifiuta uno scambio", description = "Solo chi lo riceve.")
    @ApiResponse(responseCode = "200", description = "Scambio rifiutato")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: non sei chi lo riceve")
    @ApiResponse(responseCode = "404", description = "TRADE_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "TRADE_NOT_PENDING")
    @PostMapping("/{tradeId}/reject")
    public TradeResponse rejectTrade(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tradeId) {
        return tradeService.rejectTrade(CurrentUser.id(jwt), tradeId);
    }

    // Solo chi ha proposto lo scambio
    @Operation(summary = "Annulla uno scambio", description = "Solo chi l'ha proposto.")
    @ApiResponse(responseCode = "200", description = "Scambio annullato")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: non sei chi l'ha proposto")
    @ApiResponse(responseCode = "404", description = "TRADE_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "TRADE_NOT_PENDING")
    @PostMapping("/{tradeId}/cancel")
    public TradeResponse cancelTrade(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tradeId) {
        return tradeService.cancelTrade(CurrentUser.id(jwt), tradeId);
    }
}
