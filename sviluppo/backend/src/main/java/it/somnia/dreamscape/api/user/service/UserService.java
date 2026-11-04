package it.somnia.dreamscape.api.user.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.common.config.GameProperties;
import it.somnia.dreamscape.api.user.dto.request.ChangePasswordRequest;
import it.somnia.dreamscape.api.user.dto.request.DeleteAccountRequest;
import it.somnia.dreamscape.api.user.dto.request.UpdateProfileRequest;
import it.somnia.dreamscape.api.user.dto.response.ProfileResponse;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.api.user.service.UserExceptions.AccountDisabledException;
import it.somnia.dreamscape.api.user.service.UserExceptions.EmailTakenException;
import it.somnia.dreamscape.api.user.service.UserExceptions.WrongPasswordException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

/**
 * Profilo dell'utente collegato, ricompensa giornaliera e cancellazione dell'account (decisione 018).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    // Valore che BCrypt non accetta mai: dopo la cancellazione nessuna password funziona più
    private static final String DISABLED_PASSWORD_HASH = "!account-cancellato";
    private static final String DELETED_PREFIX = "deleted-";

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final CreditService creditService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final GameProperties gameProperties;
    private final Clock clock;
    private final EntityManager entityManager;

    /**
     * L'utente collegato, se è ancora attivo. Il token di accesso può sopravvivere 15 minuti
     * alla cancellazione dell'account: per questo ogni service passa da qui.
     */
    @Transactional(readOnly = true)
    public User getActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(AccountDisabledException::new);
    }

    /**
     * Come getActiveUser, ma blocca la riga fino alla fine della transazione (SELECT ... FOR UPDATE)
     * e la rilegge dal database: crediti e punteggio sono sempre quelli veri, anche se l'utente
     * era già stato caricato prima nella stessa transazione.
     */
    @Transactional
    public User getActiveUserForUpdate(UUID userId) {
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            throw new AccountDisabledException();
        }
        entityManager.flush();
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        if (!user.isEnabled()) {
            throw new AccountDisabledException();
        }
        return user;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        return ProfileResponse.from(getActiveUser(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getActiveUser(userId);
        String email = request.email().trim();
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailTakenException();
        }
        user.setEmail(email);
        return ProfileResponse.from(user);
    }

    /**
     * Cambia la password e scollega gli altri dispositivi; quello che ha fatto la richiesta resta collegato.
     *
     * @param currentRefreshToken refresh token del cookie, o null se manca
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request, String currentRefreshToken) {
        User user = getActiveUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new WrongPasswordException("currentPassword");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeOthers(user.getId(), currentRefreshToken);
    }

    /**
     * Rende anonimo l'account: la riga resta, così lo storico di partite e scambi degli altri
     * giocatori resta valido. Amicizie, scambi e sfide in attesa li sistemano le altre
     * funzionalità ascoltando {@link AccountDeletedEvent}, nella stessa transazione.
     */
    @Transactional
    public void deleteAccount(UUID userId, DeleteAccountRequest request) {
        User user = getActiveUserForUpdate(userId);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new WrongPasswordException("password");
        }
        String anonymous = DELETED_PREFIX + user.getId().toString().replace("-", "");
        user.setUsername(anonymous);
        user.setEmail(anonymous + "@deleted.invalid");
        user.setPasswordHash(DISABLED_PASSWORD_HASH);
        user.setEnabled(false);
        user.setRankedDeckId(null);
        eventPublisher.publishEvent(new AccountDeletedEvent(user.getId()));
        refreshTokenService.revokeAll(user.getId());
    }

    /**
     * Dà i Dream Credits del primo accesso del giorno (ora italiana), una volta sola anche con
     * due richieste nello stesso momento: la riga dell'utente è bloccata finché non si decide.
     *
     * @return true se la ricompensa è stata data adesso
     */
    @Transactional
    public boolean grantDailyRewardIfDue(UUID userId) {
        User user = getActiveUserForUpdate(userId);
        LocalDate today = LocalDate.now(clock.withZone(gameProperties.timeZone()));
        if (user.getLastDailyRewardOn() != null && !user.getLastDailyRewardOn().isBefore(today)) {
            return false;
        }
        user.setLastDailyRewardOn(today);
        creditService.apply(user, CreditTransactionType.DAILY_REWARD, gameProperties.dailyReward());
        return true;
    }
}
