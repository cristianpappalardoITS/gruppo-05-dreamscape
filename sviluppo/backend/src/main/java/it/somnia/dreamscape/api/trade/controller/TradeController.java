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
@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    // box: received = in attesa di una mia risposta, sent = proposti da me, history = chiusi
    @GetMapping
    public PageResponse<TradeResponse> listTrades(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @Pattern(regexp = "received|sent|history") String box,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return tradeService.listTrades(CurrentUser.id(jwt), box, page, size);
    }

    @GetMapping("/{tradeId}")
    public TradeResponse getTrade(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tradeId) {
        return tradeService.getTrade(CurrentUser.id(jwt), tradeId);
    }

    @PostMapping
    public ResponseEntity<TradeResponse> proposeTrade(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.proposeTrade(CurrentUser.id(jwt), request));
    }

    // Avvisi prima di proporre: stesso corpo di POST /trades, non salva niente
    @PostMapping("/preview")
    public TradeWarningsResponse previewTrade(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TradeRequest request) {
        return tradeService.previewTrade(CurrentUser.id(jwt), request);
    }

    // Avvisi di uno scambio in attesa, per chi lo guarda (di solito chi sta per accettare)
    @GetMapping("/{tradeId}/deck-warnings")
    public TradeWarningsResponse getTradeWarnings(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tradeId) {
        return tradeService.getTradeWarnings(CurrentUser.id(jwt), tradeId);
    }

    // Solo chi riceve lo scambio: sposta le carte
    @PostMapping("/{tradeId}/accept")
    public TradeResponse acceptTrade(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tradeId) {
        return tradeService.acceptTrade(CurrentUser.id(jwt), tradeId);
    }

    // Solo chi riceve lo scambio
    @PostMapping("/{tradeId}/reject")
    public TradeResponse rejectTrade(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tradeId) {
        return tradeService.rejectTrade(CurrentUser.id(jwt), tradeId);
    }

    // Solo chi ha proposto lo scambio
    @PostMapping("/{tradeId}/cancel")
    public TradeResponse cancelTrade(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tradeId) {
        return tradeService.cancelTrade(CurrentUser.id(jwt), tradeId);
    }
}
