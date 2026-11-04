package it.somnia.dreamscape.api.user.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.common.security.OpaqueTokens;
import it.somnia.dreamscape.api.common.security.SecurityProperties;
import it.somnia.dreamscape.api.user.model.RefreshToken;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.RefreshTokenRepository;
import it.somnia.dreamscape.api.user.service.UserExceptions.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refresh token: uno per dispositivo, 7 giorni, nel database solo l'hash.
 * A ogni uso si sostituisce con uno nuovo (rotazione). Se arriva un token già sostituito,
 * qualcuno potrebbe averlo rubato: si scollegano tutti i dispositivi dell'utente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    // Crea un token per un nuovo accesso e restituisce il valore in chiaro, da mettere nel cookie
    @Transactional
    public String issue(User user) {
        String token = OpaqueTokens.generate();
        Instant expiresAt = clock.instant().plus(securityProperties.refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(user, OpaqueTokens.hash(token), expiresAt));
        return token;
    }

    /**
     * Usa il token e lo sostituisce con uno nuovo.
     *
     * @throws InvalidRefreshTokenException se manca, è scaduto, revocato o l'utente non è più attivo
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken stored = refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(token))
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = clock.instant();
        User user = stored.getUser();
        // Scaduto, oppure revocato da logout o nuova password (che mettono anche la scadenza)
        if (stored.isExpired(now) || !user.isEnabled()) {
            throw new InvalidRefreshTokenException();
        }
        // Revocato ma non scaduto: è stato sostituito da una rotazione e qualcuno lo sta riusando
        if (stored.isRevoked()) {
            log.warn("Refresh token già usato per l'utente {}: scollego tutti i dispositivi", user.getId());
            refreshTokenRepository.revokeAllByUserId(user.getId(), now);
            throw new InvalidRefreshTokenException();
        }
        stored.setRevokedAt(now);
        return new Rotation(user, issue(user));
    }

    // Logout: revoca il token del cookie, se c'è e non è già revocato
    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        Instant now = clock.instant();
        refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(token))
                .filter(stored -> !stored.isRevoked())
                .ifPresent(stored -> {
                    stored.setRevokedAt(now);
                    stored.setExpiresAt(now);
                });
    }

    @Transactional
    public void revokeAll(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, clock.instant());
    }

    // Scollega tutti tranne il dispositivo del token indicato (null = tutti)
    @Transactional
    public void revokeOthers(UUID userId, String keepToken) {
        if (keepToken == null || keepToken.isBlank()) {
            revokeAll(userId);
            return;
        }
        refreshTokenRepository.revokeAllByUserIdExcept(userId, OpaqueTokens.hash(keepToken), clock.instant());
    }

    /**
     * Esito della rotazione.
     *
     * @param user  proprietario del token
     * @param token nuovo refresh token in chiaro, da mettere nel cookie
     */
    public record Rotation(User user, String token) {
    }
}
