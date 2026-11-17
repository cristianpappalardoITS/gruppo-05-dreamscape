package it.somnia.dreamscape.api.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import it.somnia.dreamscape.api.user.service.GoogleTokenVerifier.GoogleIdentity;
import it.somnia.dreamscape.api.user.service.UserExceptions.InvalidGoogleTokenException;

// La verifica dell'id token di Google senza rete: una chiave RSA di prova fa la parte di quella di Google
class NimbusGoogleTokenVerifierTest {

    private static final String CLIENT_ID = "client-di-prova.apps.googleusercontent.com";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private final RSAKey googleKey = newKey();
    private final NimbusGoogleTokenVerifier verifier = verifierTrusting(googleKey);

    @Test
    void verify_validToken_returnsSubjectAndEmail() throws Exception {
        // Given
        String token = sign(googleKey, claims().build());

        // When
        GoogleIdentity identity = verifier.verify(token);

        // Then
        assertEquals("1234567890", identity.subject());
        assertEquals("mario.rossi@gmail.com", identity.email());
        assertTrue(identity.emailVerified());
    }

    @Test
    void verify_issuerWithoutHttps_isAccepted() throws Exception {
        // Given: Google scrive l'emittente anche senza https://
        String token = sign(googleKey, claims().issuer("accounts.google.com").build());

        // When / Then
        assertEquals("1234567890", verifier.verify(token).subject());
    }

    @Test
    void verify_emailNotVerified_saysSo() throws Exception {
        // Given
        String token = sign(googleKey, claims().claim("email_verified", false).build());

        // When / Then
        assertFalse(verifier.verify(token).emailVerified());
    }

    @Test
    void verify_tokenForAnotherSite_isRefused() throws Exception {
        // Given: un id token valido, ma chiesto da un altro client id
        String token = sign(googleKey, claims().audience("altro-sito.apps.googleusercontent.com").build());

        // When / Then
        assertThrows(InvalidGoogleTokenException.class, () -> verifier.verify(token));
    }

    @Test
    void verify_otherIssuer_isRefused() throws Exception {
        // Given
        String token = sign(googleKey, claims().issuer("https://falso-google.example").build());

        // When / Then
        assertThrows(InvalidGoogleTokenException.class, () -> verifier.verify(token));
    }

    @Test
    void verify_expiredToken_isRefused() throws Exception {
        // Given: scaduto da un'ora (la tolleranza di Spring è di 60 secondi)
        Instant anHourAgo = Instant.now().minusSeconds(3600);
        String token = sign(googleKey, claims().issueTime(Date.from(anHourAgo.minusSeconds(60)))
                .expirationTime(Date.from(anHourAgo)).build());

        // When / Then
        assertThrows(InvalidGoogleTokenException.class, () -> verifier.verify(token));
    }

    @Test
    void verify_signedWithAnotherKey_isRefused() throws Exception {
        // Given: qualcuno si costruisce un token con la sua chiave
        String token = sign(newKey(), claims().build());

        // When / Then
        assertThrows(InvalidGoogleTokenException.class, () -> verifier.verify(token));
    }

    @Test
    void verify_withoutEmail_isRefused() throws Exception {
        // Given
        String token = sign(googleKey, claims().claim("email", null).build());

        // When / Then
        assertThrows(InvalidGoogleTokenException.class, () -> verifier.verify(token));
    }

    @Test
    void verify_notAToken_isRefused() {
        assertThrows(InvalidGoogleTokenException.class, () -> verifier.verify("non-un-token"));
    }

    @Test
    void verify_withoutClientId_refusesEveryToken() throws Exception {
        // Given: GOOGLE_CLIENT_ID vuota
        NimbusGoogleTokenVerifier withoutClientId = new NimbusGoogleTokenVerifier(
                NimbusJwtDecoder.withPublicKey(googleKey.toRSAPublicKey()).build(), "");
        String token = sign(googleKey, claims().build());

        // When / Then
        assertThrows(InvalidGoogleTokenException.class, () -> withoutClientId.verify(token));
    }

    // I claim di un id token di Google valido per il nostro client id, che scade tra 5 minuti
    private static JWTClaimsSet.Builder claims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(GOOGLE_ISSUER)
                .audience(List.of(CLIENT_ID))
                .subject("1234567890")
                .claim("email", "mario.rossi@gmail.com")
                .claim("email_verified", true)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)));
    }

    private static String sign(RSAKey key, JWTClaimsSet claims) throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static NimbusGoogleTokenVerifier verifierTrusting(RSAKey key) {
        try {
            return new NimbusGoogleTokenVerifier(NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build(),
                    CLIENT_ID);
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RSAKey newKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("chiave-di-prova").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
