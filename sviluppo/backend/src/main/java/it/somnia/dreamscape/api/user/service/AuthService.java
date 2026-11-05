package it.somnia.dreamscape.api.user.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.common.config.GameProperties;
import it.somnia.dreamscape.api.common.security.AccessTokenService;
import it.somnia.dreamscape.api.common.security.AccessTokenService.AccessToken;
import it.somnia.dreamscape.api.user.dto.request.LoginRequest;
import it.somnia.dreamscape.api.user.dto.request.RegisterRequest;
import it.somnia.dreamscape.api.user.dto.response.AccessTokenResponse;
import it.somnia.dreamscape.api.user.dto.response.LoginResponse;
import it.somnia.dreamscape.api.user.dto.response.UserResponse;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.model.UserRole;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.api.user.service.RefreshTokenService.Rotation;
import it.somnia.dreamscape.api.user.service.UserExceptions.EmailTakenException;
import it.somnia.dreamscape.api.user.service.UserExceptions.InvalidCredentialsException;
import it.somnia.dreamscape.api.user.service.UserExceptions.InvalidRefreshTokenException;
import it.somnia.dreamscape.api.user.service.UserExceptions.UsernameTakenException;
import lombok.RequiredArgsConstructor;

/**
 * Registrazione, accesso, rinnovo del token di accesso e uscita (guida per il frontend, capitolo 3).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CreditService creditService;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final PasswordEncoder passwordEncoder;
    private final GameProperties gameProperties;
    private final Clock clock;

    // Hash di una password qualsiasi: con un utente inesistente il controllo dura lo stesso tempo
    private String dummyPasswordHash;

    /**
     * Crea il giocatore con i consensi e il bonus di registrazione, nella stessa transazione.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameTakenException();
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailTakenException();
        }
        Instant now = clock.instant();
        User user = new User(username, email, passwordEncoder.encode(request.password()), UserRole.PLAYER);
        user.setPrivacyAcceptedAt(now);
        user.setTermsAcceptedAt(now);
        userRepository.saveAndFlush(user);
        creditService.apply(user, CreditTransactionType.SIGNUP_BONUS, gameProperties.signupBonus());
        return UserResponse.from(user);
    }

    /**
     * Accesso con username o email. Dà la ricompensa del primo accesso del giorno.
     */
    @Transactional
    public Session login(LoginRequest request) {
        String login = request.login().trim();
        Optional<User> found = login.contains("@")
                ? userRepository.findByEmailIgnoreCase(login)
                : userRepository.findByUsernameIgnoreCase(login);
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyPasswordHash());
            throw new InvalidCredentialsException();
        }
        User user = found.get();
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        user.setLastLoginAt(clock.instant());
        if (!user.isAdmin()) {
            userService.grantDailyRewardIfDue(user.getId());
        }
        String refreshToken = refreshTokenService.issue(user);
        AccessToken accessToken = accessTokenService.create(user.getId(), user.getRole());
        LoginResponse body = new LoginResponse(accessToken.value(), accessToken.expiresIn(), UserResponse.from(user));
        return new Session(body, refreshToken);
    }

    /**
     * Nuovo token di accesso dal refresh token del cookie, che viene sostituito.
     * All'apertura di ogni pagina il frontend passa da qui: vale come accesso del giorno.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshedSession refresh(String refreshToken) {
        Rotation rotation = refreshTokenService.rotate(refreshToken);
        User user = rotation.user();
        if (!user.isAdmin()) {
            userService.grantDailyRewardIfDue(user.getId());
        }
        AccessToken accessToken = accessTokenService.create(user.getId(), user.getRole());
        return new RefreshedSession(new AccessTokenResponse(accessToken.value(), accessToken.expiresIn()),
                rotation.token());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private String dummyPasswordHash() {
        if (dummyPasswordHash == null) {
            dummyPasswordHash = passwordEncoder.encode("password-che-non-esiste");
        }
        return dummyPasswordHash;
    }

    /**
     * Esito dell'accesso: il corpo della risposta e il refresh token da mettere nel cookie.
     */
    public record Session(LoginResponse body, String refreshToken) {
    }

    /**
     * Esito del rinnovo: il nuovo token di accesso e il nuovo refresh token per il cookie.
     */
    public record RefreshedSession(AccessTokenResponse body, String refreshToken) {
    }
}
