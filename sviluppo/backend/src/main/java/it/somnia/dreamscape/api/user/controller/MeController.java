package it.somnia.dreamscape.api.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import it.somnia.dreamscape.api.common.security.RefreshCookies;
import it.somnia.dreamscape.api.user.dto.request.ChangePasswordRequest;
import it.somnia.dreamscape.api.user.dto.request.DeleteAccountRequest;
import it.somnia.dreamscape.api.user.dto.request.UpdateProfileRequest;
import it.somnia.dreamscape.api.user.dto.response.CreditTransactionResponse;
import it.somnia.dreamscape.api.user.dto.response.DashboardResponse;
import it.somnia.dreamscape.api.user.dto.response.ProfileResponse;
import it.somnia.dreamscape.api.user.service.CreditService;
import it.somnia.dreamscape.api.user.service.DashboardService;
import it.somnia.dreamscape.api.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

// Profilo, riepilogo della Dashboard, password, cancellazione dell'account e movimenti di crediti
// dell'utente collegato
@Tag(name = "Profilo", description = "I dati dell'utente collegato: profilo, riepilogo della Dashboard, "
        + "password, cancellazione dell'account e movimenti di Dream Credits.")
@Validated
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final CreditService creditService;
    private final DashboardService dashboardService;
    private final RefreshCookies refreshCookies;

    @Operation(summary = "Profilo", description = "Username, email, ruolo e data di iscrizione. I crediti non ci "
            + "sono: arrivano con l'accesso e con /me/summary.")
    @ApiResponse(responseCode = "200", description = "Il profilo")
    @GetMapping
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(CurrentUser.id(jwt));
    }

    @Operation(summary = "Riepilogo della Dashboard", description = "Crediti, punteggio e lega, missioni, contatori "
            + "delle richieste in attesa e carte dell'ultima apertura.")
    @ApiResponse(responseCode = "200", description = "Il riepilogo")
    @GetMapping("/summary")
    public DashboardResponse getSummary(@AuthenticationPrincipal Jwt jwt) {
        return dashboardService.getSummary(CurrentUser.id(jwt));
    }

    @Operation(summary = "Cambio email", description = "Lo username non si cambia.")
    @ApiResponse(responseCode = "200", description = "Il profilo aggiornato")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: email vuota o non valida")
    @ApiResponse(responseCode = "409", description = "EMAIL_TAKEN")
    @PutMapping
    public ProfileResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(CurrentUser.id(jwt), request);
    }

    @Operation(summary = "Cambio password", description = "Scollega gli altri dispositivi; questo resta collegato.")
    @ApiResponse(responseCode = "204", description = "Password cambiata")
    @ApiResponse(responseCode = "400", description = "WRONG_PASSWORD (password attuale sbagliata), PASSWORD_NOT_SET "
            + "(account creato con Google, senza password) o VALIDATION_ERROR")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request,
            @Parameter(hidden = true)
            @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken) {
        userService.changePassword(CurrentUser.id(jwt), request, refreshToken);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancellazione dell'account", description = "Chiede la password. L'account diventa anonimo "
            + "e non si recupera; il cookie viene cancellato.")
    @ApiResponse(responseCode = "204", description = "Account cancellato")
    @ApiResponse(responseCode = "400", description = "WRONG_PASSWORD, PASSWORD_NOT_SET o VALIDATION_ERROR")
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(CurrentUser.id(jwt), request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }

    @Operation(summary = "Movimenti di crediti", description = "Dal più recente. amount è negativo per le spese.")
    @ApiResponse(responseCode = "200", description = "Una pagina di movimenti")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: page o size fuori dai limiti")
    @GetMapping("/credit-transactions")
    public PageResponse<CreditTransactionResponse> listCreditTransactions(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Numero della pagina, da 0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Movimenti per pagina, da 1 a 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return creditService.listTransactions(CurrentUser.id(jwt), page, size);
    }
}
