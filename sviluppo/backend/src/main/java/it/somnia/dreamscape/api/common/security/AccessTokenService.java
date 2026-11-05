package it.somnia.dreamscape.api.common.security;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import it.somnia.dreamscape.api.user.model.UserRole;
import lombok.RequiredArgsConstructor;

/**
 * Crea il token di accesso JWT (15 minuti). Il token contiene solo l'id dell'utente
 * ({@code sub}) e il ruolo: il resto si legge dal database a ogni richiesta.
 */
@Service
@RequiredArgsConstructor
public class AccessTokenService {

    static final String ISSUER = "dreamscape";
    static final String ROLE_CLAIM = "role";

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    public AccessToken create(UUID userId, UserRole role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(securityProperties.accessTokenTtl()))
                .claim(ROLE_CLAIM, role.name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, securityProperties.accessTokenTtl().toSeconds());
    }

    /**
     * Token di accesso appena creato.
     *
     * @param value     il JWT da mandare nell'header Authorization
     * @param expiresIn secondi di validità
     */
    public record AccessToken(String value, long expiresIn) {
    }
}
