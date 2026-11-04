package it.somnia.dreamscape.api.user.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.somnia.dreamscape.api.user.model.RefreshToken;

/**
 * Le revoche di gruppo (nuova password, account cancellato, token rubato) mettono anche la
 * scadenza ad adesso: così un token revocato in questo modo non sembra un token "sostituito
 * e poi riusato", che invece scollega tutti i dispositivi.
 * Svuotano la cache di JPA: vanno chiamate come ultima modifica della transazione.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Scollega tutti i dispositivi (token riusato, nuova password, account cancellato)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now, t.expiresAt = :now "
            + "WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    // Scollega gli altri dispositivi, tranne quello che ha fatto la richiesta
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now, t.expiresAt = :now "
            + "WHERE t.user.id = :userId AND t.revokedAt IS NULL AND t.tokenHash <> :keepHash")
    int revokeAllByUserIdExcept(@Param("userId") UUID userId, @Param("keepHash") String keepHash,
            @Param("now") Instant now);
}
