package it.somnia.dreamscape.api.user.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.common.config.GameProperties;
import it.somnia.dreamscape.api.common.config.GoogleProperties;
import it.somnia.dreamscape.api.common.exception.FieldErrorResponse;
import it.somnia.dreamscape.api.user.dto.request.GoogleLoginRequest;
import it.somnia.dreamscape.api.user.dto.response.GoogleSignupResponse;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.model.UserRole;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.api.user.service.AuthService.Session;
import it.somnia.dreamscape.api.user.service.GoogleTokenVerifier.GoogleIdentity;
import it.somnia.dreamscape.api.user.service.UserExceptions.AccountDisabledException;
import it.somnia.dreamscape.api.user.service.UserExceptions.ConsentsRequiredException;
import it.somnia.dreamscape.api.user.service.UserExceptions.GoogleAccountConflictException;
import it.somnia.dreamscape.api.user.service.UserExceptions.GoogleEmailNotVerifiedException;
import it.somnia.dreamscape.api.user.service.UserExceptions.GoogleLoginDisabledException;
import it.somnia.dreamscape.api.user.service.UserExceptions.InvalidGoogleTokenException;
import it.somnia.dreamscape.api.user.service.UserExceptions.UsernameTakenException;
import lombok.RequiredArgsConstructor;

/**
 * Accesso con Google ({@code POST /auth/google}, decisione 032): accesso, registrazione e collegamento
 * di un account esistente, con lo stesso token di accesso e lo stesso cookie dell'accesso con la password.
 */
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    // Username proposto quando l'email non dà niente di usabile
    private static final String FALLBACK_USERNAME = "dreamweaver";
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 30;

    private final UserRepository userRepository;
    private final AuthService authService;
    private final CreditService creditService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final GoogleProperties googleProperties;
    private final GameProperties gameProperties;
    private final Clock clock;

    /**
     * Fa entrare con un id token di Google.
     *
     * <ol>
     *     <li>Account Google già collegato: accesso.</li>
     *     <li>Email verificata di un account Dreamscape esistente: lo collega a Google, poi accesso.</li>
     *     <li>Nessun account: senza username risponde che serve la registrazione, con un username proposto;
     *         con username e consensi crea il giocatore con il bonus di registrazione, poi accesso.</li>
     * </ol>
     *
     * @param request l'id token e, per la registrazione, username e consensi
     * @return l'accesso fatto (con {@code created} se l'account è nuovo) oppure la richiesta di registrazione
     * @throws GoogleLoginDisabledException    se manca {@code GOOGLE_CLIENT_ID}
     * @throws InvalidGoogleTokenException     se l'id token non è valido
     * @throws AccountDisabledException        se l'account trovato è stato cancellato o disattivato
     * @throws GoogleEmailNotVerifiedException se serve l'email e Google non l'ha verificata
     * @throws GoogleAccountConflictException  se l'account con quell'email è collegato a un altro account Google
     * @throws UsernameTakenException          se l'username scelto è già usato
     * @throws ConsentsRequiredException       se c'è l'username ma mancano i consensi
     */
    @Transactional
    public GoogleOutcome loginWithGoogle(GoogleLoginRequest request) {
        if (!googleProperties.isEnabled()) {
            throw new GoogleLoginDisabledException();
        }
        GoogleIdentity identity = googleTokenVerifier.verify(request.idToken());

        Optional<User> linked = userRepository.findByGoogleSubject(identity.subject());
        if (linked.isPresent()) {
            return new LoggedIn(authService.openSession(requireEnabled(linked.get())), false);
        }
        if (!identity.emailVerified()) {
            throw new GoogleEmailNotVerifiedException();
        }
        Optional<User> sameEmail = userRepository.findByEmailIgnoreCase(identity.email());
        if (sameEmail.isPresent()) {
            return new LoggedIn(authService.openSession(link(requireEnabled(sameEmail.get()), identity)), false);
        }
        if (request.username() == null || request.username().isBlank()) {
            return new SignupRequired(new GoogleSignupResponse(true, identity.email(),
                    suggestUsername(identity.email())));
        }
        User created = register(request, identity);
        return new LoggedIn(authService.openSession(created), true);
    }

    private static User requireEnabled(User user) {
        if (!user.isEnabled()) {
            throw new AccountDisabledException();
        }
        return user;
    }

    // Collega Google a un account esistente; un account già collegato a un altro Google resta com'è
    private static User link(User user, GoogleIdentity identity) {
        if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(identity.subject())) {
            throw new GoogleAccountConflictException();
        }
        user.setGoogleSubject(identity.subject());
        return user;
    }

    // Come POST /auth/register, ma senza password: consensi obbligatori e bonus di registrazione
    private User register(GoogleLoginRequest request, GoogleIdentity identity) {
        List<FieldErrorResponse> missing = new ArrayList<>();
        if (!Boolean.TRUE.equals(request.privacyAccepted())) {
            missing.add(new FieldErrorResponse("privacyAccepted", "Bisogna accettare la Privacy Policy."));
        }
        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            missing.add(new FieldErrorResponse("termsAccepted", "Bisogna accettare i Termini di servizio."));
        }
        if (!missing.isEmpty()) {
            throw new ConsentsRequiredException(missing);
        }
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameTakenException();
        }
        Instant now = clock.instant();
        User user = new User(username, identity.email(), null, UserRole.PLAYER);
        user.setGoogleSubject(identity.subject());
        user.setPrivacyAcceptedAt(now);
        user.setTermsAcceptedAt(now);
        userRepository.saveAndFlush(user);
        creditService.apply(user, CreditTransactionType.SIGNUP_BONUS, gameProperties.signupBonus());
        return user;
    }

    /**
     * Propone un username libero dalla parte dell'email prima della chiocciola.
     *
     * <p>Tiene solo lettere, numeri e trattino basso, come la registrazione; se è già usato aggiunge un numero
     * ({@code mario_rossi2}, {@code mario_rossi3}...).
     *
     * @param email l'email dell'account Google
     * @return un username valido e libero in questo momento
     */
    String suggestUsername(String email) {
        String localPart = email.substring(0, Math.max(0, email.indexOf('@')));
        String base = localPart.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
        if (base.length() < MIN_USERNAME_LENGTH) {
            base = FALLBACK_USERNAME;
        }
        base = base.substring(0, Math.min(base.length(), MAX_USERNAME_LENGTH));
        String candidate = base;
        for (int number = 2; userRepository.existsByUsernameIgnoreCase(candidate); number++) {
            String suffix = Integer.toString(number);
            candidate = base.substring(0, Math.min(base.length(), MAX_USERNAME_LENGTH - suffix.length())) + suffix;
        }
        return candidate;
    }

    /**
     * Esito dell'accesso con Google.
     */
    public sealed interface GoogleOutcome permits LoggedIn, SignupRequired {
    }

    /**
     * La persona è dentro.
     *
     * @param session il corpo della risposta e il refresh token per il cookie
     * @param created {@code true} se l'account è stato creato adesso (risposta {@code 201})
     */
    public record LoggedIn(Session session, boolean created) implements GoogleOutcome {
    }

    /**
     * Serve la registrazione: il frontend chiede username e consensi e rimanda la richiesta (risposta {@code 202}).
     *
     * @param body email e username proposto
     */
    public record SignupRequired(GoogleSignupResponse body) implements GoogleOutcome {
    }
}
