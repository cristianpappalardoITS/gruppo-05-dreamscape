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

import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.common.security.CurrentUser;
import it.somnia.dreamscape.api.common.security.RefreshCookies;
import it.somnia.dreamscape.api.user.dto.request.ChangePasswordRequest;
import it.somnia.dreamscape.api.user.dto.request.DeleteAccountRequest;
import it.somnia.dreamscape.api.user.dto.request.UpdateProfileRequest;
import it.somnia.dreamscape.api.user.dto.response.CreditTransactionResponse;
import it.somnia.dreamscape.api.user.dto.response.ProfileResponse;
import it.somnia.dreamscape.api.user.service.CreditService;
import it.somnia.dreamscape.api.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

// Profilo, password, cancellazione dell'account e movimenti di crediti dell'utente collegato
@Validated
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final CreditService creditService;
    private final RefreshCookies refreshCookies;

    @GetMapping
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(CurrentUser.id(jwt));
    }

    @PutMapping
    public ProfileResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(CurrentUser.id(jwt), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request,
            @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken) {
        userService.changePassword(CurrentUser.id(jwt), request, refreshToken);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(CurrentUser.id(jwt), request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }

    @GetMapping("/credit-transactions")
    public PageResponse<CreditTransactionResponse> listCreditTransactions(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return creditService.listTransactions(CurrentUser.id(jwt), page, size);
    }
}
