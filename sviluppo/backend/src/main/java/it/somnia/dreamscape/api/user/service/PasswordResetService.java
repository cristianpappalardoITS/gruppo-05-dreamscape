package it.somnia.dreamscape.api.user.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.common.security.OpaqueTokens;
import it.somnia.dreamscape.api.common.security.SecurityProperties;
import it.somnia.dreamscape.api.user.dto.request.PasswordResetRequest;
import it.somnia.dreamscape.api.user.model.PasswordResetToken;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.PasswordResetTokenRepository;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.api.user.service.UserExceptions.InvalidResetTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Recupero della password (#107): link monouso valido 1 ora, mandato per email.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    // Un solo link al minuto per utente: chi preme più volte il pulsante non riceve dieci email
    private static final Duration MIN_INTERVAL = Duration.ofMinutes(1);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;
    private final MailProperties mailProperties;
    private final Clock clock;

    /**
     * Manda il link se l'email appartiene a un utente attivo. Non dice mai se l'email esiste:
     * il controller risponde sempre 202, e l'email parte dopo la risposta
     * ({@link PasswordResetRequestedEvent}), così i tempi non cambiano.
     */
    @Transactional
    public void requestReset(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim()).filter(User::isEnabled).orElse(null);
        if (user == null) {
            return;
        }
        Instant now = clock.instant();
        if (passwordResetTokenRepository.existsByUserIdAndCreatedAtAfter(user.getId(), now.minus(MIN_INTERVAL))) {
            log.info("Link di recupero già chiesto da meno di un minuto per l'utente {}", user.getId());
            return;
        }
        passwordResetTokenRepository.invalidateAllByUserId(user.getId(), now);
        // L'aggiornamento di gruppo svuota la cache di JPA: il nuovo token punta all'utente ricaricato
        User managedUser = userRepository.getReferenceById(user.getId());
        String token = OpaqueTokens.generate();
        passwordResetTokenRepository.save(new PasswordResetToken(managedUser, OpaqueTokens.hash(token),
                now.plus(securityProperties.passwordResetTokenTtl())));
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), user.getUsername(),
                mailProperties.passwordResetUrl() + token));
    }

    /**
     * Imposta la nuova password e scollega tutti i dispositivi.
     *
     * @throws InvalidResetTokenException se il token non esiste, è già stato usato o è scaduto
     */
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        Instant now = clock.instant();
        PasswordResetToken stored = passwordResetTokenRepository.findByTokenHash(OpaqueTokens.hash(request.token()))
                .filter(token -> token.isUsable(now))
                .orElseThrow(InvalidResetTokenException::new);
        User user = stored.getUser();
        if (!user.isEnabled()) {
            throw new InvalidResetTokenException();
        }
        stored.setUsedAt(now);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(user.getId());
    }
}
