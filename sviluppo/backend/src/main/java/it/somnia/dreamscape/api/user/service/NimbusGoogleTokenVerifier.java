package it.somnia.dreamscape.api.user.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import it.somnia.dreamscape.api.common.config.GoogleProperties;
import it.somnia.dreamscape.api.user.service.UserExceptions.InvalidGoogleTokenException;

/**
 * Verifica gli id token di Google con le chiavi pubbliche che Google pubblica, senza librerie in più.
 *
 * <p>Il decoder non è un bean di Spring: il backend ha già un {@code JwtDecoder} per i propri token di
 * accesso, e due bean dello stesso tipo si confonderebbero. Le chiavi di Google si scaricano alla prima
 * verifica e restano in cache.
 */
@Component
public class NimbusGoogleTokenVerifier implements GoogleTokenVerifier {

    // Dove Google pubblica le chiavi con cui firma gli id token
    static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    // Google scrive l'emittente in tutti e due i modi
    static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final NimbusJwtDecoder decoder;

    // Il costruttore che usa Spring; l'altro serve ai test
    @Autowired
    public NimbusGoogleTokenVerifier(GoogleProperties googleProperties) {
        this(NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build(), googleProperties.clientId());
    }

    // Per i test: un decoder con una chiave di prova al posto di quelle di Google
    NimbusGoogleTokenVerifier(NimbusJwtDecoder decoder, String clientId) {
        this.decoder = decoder;
        this.decoder.setJwtValidator(validator(clientId));
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException invalid) {
            throw new InvalidGoogleTokenException();
        }
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new InvalidGoogleTokenException();
        }
        // Google lo manda come booleano, ma in alcuni token vecchi era il testo "true"
        boolean emailVerified = "true".equalsIgnoreCase(String.valueOf(jwt.getClaims().get("email_verified")));
        return new GoogleIdentity(subject, email, emailVerified);
    }

    // Scadenza, emittente di Google e destinatario uguale al nostro client id
    private static OAuth2TokenValidator<Jwt> validator(String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS, issuer -> GOOGLE_ISSUERS.contains(
                        String.valueOf(issuer))),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, audience -> clientId != null
                        && !clientId.isBlank() && audience != null && audience.contains(clientId)));
    }
}
