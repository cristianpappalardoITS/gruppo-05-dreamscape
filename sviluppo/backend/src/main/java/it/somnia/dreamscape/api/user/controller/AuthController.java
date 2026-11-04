package it.somnia.dreamscape.api.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.somnia.dreamscape.api.common.security.RefreshCookies;
import it.somnia.dreamscape.api.user.dto.request.LoginRequest;
import it.somnia.dreamscape.api.user.dto.request.PasswordResetRequest;
import it.somnia.dreamscape.api.user.dto.request.PasswordResetRequestRequest;
import it.somnia.dreamscape.api.user.dto.request.RegisterRequest;
import it.somnia.dreamscape.api.user.dto.response.AccessTokenResponse;
import it.somnia.dreamscape.api.user.dto.response.LoginResponse;
import it.somnia.dreamscape.api.user.dto.response.UserResponse;
import it.somnia.dreamscape.api.user.service.AuthService;
import it.somnia.dreamscape.api.user.service.AuthService.RefreshedSession;
import it.somnia.dreamscape.api.user.service.AuthService.Session;
import it.somnia.dreamscape.api.user.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Registrazione, accesso, refresh, uscita e recupero password: aperti a tutti
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RefreshCookies refreshCookies;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Session session = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(session.refreshToken()).toString())
                .body(session.body());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken) {
        RefreshedSession session = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(session.refreshToken()).toString())
                .body(session.body());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }

    // Sempre 202, anche se l'email non esiste: non si rivela chi è registrato
    @PostMapping("/password-reset-requests")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-resets")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
