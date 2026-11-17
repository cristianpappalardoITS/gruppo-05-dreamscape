package it.somnia.dreamscape.api.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.somnia.dreamscape.api.common.security.RefreshCookies;
import it.somnia.dreamscape.api.user.dto.request.GoogleLoginRequest;
import it.somnia.dreamscape.api.user.dto.request.LoginRequest;
import it.somnia.dreamscape.api.user.dto.request.PasswordResetRequest;
import it.somnia.dreamscape.api.user.dto.request.PasswordResetRequestRequest;
import it.somnia.dreamscape.api.user.dto.request.RegisterRequest;
import it.somnia.dreamscape.api.user.dto.response.AccessTokenResponse;
import it.somnia.dreamscape.api.user.dto.response.GoogleSignupResponse;
import it.somnia.dreamscape.api.user.dto.response.LoginResponse;
import it.somnia.dreamscape.api.user.dto.response.UserResponse;
import it.somnia.dreamscape.api.user.service.AuthService;
import it.somnia.dreamscape.api.user.service.GoogleAuthService;
import it.somnia.dreamscape.api.user.service.GoogleAuthService.GoogleOutcome;
import it.somnia.dreamscape.api.user.service.GoogleAuthService.LoggedIn;
import it.somnia.dreamscape.api.user.service.GoogleAuthService.SignupRequired;
import it.somnia.dreamscape.api.user.service.AuthService.RefreshedSession;
import it.somnia.dreamscape.api.user.service.AuthService.Session;
import it.somnia.dreamscape.api.user.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Registrazione, accesso, refresh, uscita e recupero password: aperti a tutti
@Tag(name = "Accesso", description = "Registrazione, accesso (anche con Google), rinnovo del token, uscita e "
        + "recupero della password. Non serve il token. Il refresh token viaggia nel cookie dreamscape_refresh "
        + "(HttpOnly, 7 giorni), che il browser manda da solo.")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final PasswordResetService passwordResetService;
    private final RefreshCookies refreshCookies;

    @Operation(summary = "Registrazione", description = "Crea l'account con 500 Dream Credits di benvenuto. "
            + "Non fa l'accesso: dopo si chiama /auth/login.")
    @ApiResponse(responseCode = "201", description = "Account creato")
    @ApiResponse(responseCode = "400",
            description = "VALIDATION_ERROR: campi mancanti o non validi, consensi non accettati")
    @ApiResponse(responseCode = "409", description = "USERNAME_TAKEN o EMAIL_TAKEN")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Accesso", description = "Con username o email e password. Restituisce il token di "
            + "accesso (15 minuti) e mette il refresh token nel cookie.")
    @ApiResponse(responseCode = "200", description = "Accesso fatto: token, utente e crediti")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: campi vuoti")
    @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS (utente o password sbagliati) o "
            + "ACCOUNT_DISABLED (account cancellato)")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Session session = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(session.refreshToken()).toString())
                .body(session.body());
    }

    // 200 accesso, 201 account appena creato, 202 serve la registrazione (decisione 032)
    @Operation(summary = "Accesso con Google", description = "Riceve l'ID token di Google Identity Services. "
            + "Se l'account Dreamscape non c'è ancora risponde 202: si rimanda la richiesta con username e consensi.")
    @ApiResponse(responseCode = "200", description = "Accesso fatto, come /auth/login",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "201", description = "Account appena creato e accesso fatto",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "202", description = "Serve la registrazione: email e username proposto",
            content = @Content(schema = @Schema(implementation = GoogleSignupResponse.class)))
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: username non valido o consensi mancanti")
    @ApiResponse(responseCode = "401", description = "INVALID_GOOGLE_TOKEN o ACCOUNT_DISABLED")
    @ApiResponse(responseCode = "403", description = "GOOGLE_EMAIL_NOT_VERIFIED")
    @ApiResponse(responseCode = "409", description = "GOOGLE_ACCOUNT_CONFLICT o USERNAME_TAKEN")
    @ApiResponse(responseCode = "503",
            description = "GOOGLE_LOGIN_DISABLED: manca il client id, si nasconde il pulsante")
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        GoogleOutcome outcome = googleAuthService.loginWithGoogle(request);
        if (outcome instanceof SignupRequired signupRequired) {
            return ResponseEntity.accepted().body(signupRequired.body());
        }
        LoggedIn loggedIn = (LoggedIn) outcome;
        Session session = loggedIn.session();
        return ResponseEntity.status(loggedIn.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(session.refreshToken()).toString())
                .body(session.body());
    }

    @Operation(summary = "Token nuovo", description = "Usa il cookie dreamscape_refresh: niente corpo. Restituisce "
            + "un token di accesso nuovo e cambia anche il cookie.")
    @ApiResponse(responseCode = "200", description = "Token nuovo")
    @ApiResponse(responseCode = "401", description = "INVALID_REFRESH_TOKEN: cookie mancante, scaduto o revocato")
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @Parameter(hidden = true)
            @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken) {
        RefreshedSession session = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(session.refreshToken()).toString())
                .body(session.body());
    }

    @Operation(summary = "Uscita", description = "Revoca il refresh token del cookie e cancella il cookie.")
    @ApiResponse(responseCode = "204", description = "Uscita fatta")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true)
            @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }

    // Sempre 202, anche se l'email non esiste: non si rivela chi è registrato
    @Operation(summary = "Password dimenticata", description = "Manda l'email con il link per la nuova password "
            + "(vale 1 ora). Risponde sempre 202, anche se l'email non è registrata.")
    @ApiResponse(responseCode = "202", description = "Richiesta ricevuta")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: email vuota o non valida")
    @PostMapping("/password-reset-requests")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Nuova password", description = "Con il token del link ricevuto per email. Scollega "
            + "tutti i dispositivi.")
    @ApiResponse(responseCode = "204", description = "Password cambiata")
    @ApiResponse(responseCode = "400", description = "INVALID_RESET_TOKEN (link scaduto o già usato) o "
            + "VALIDATION_ERROR (password troppo corta)")
    @PostMapping("/password-resets")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
