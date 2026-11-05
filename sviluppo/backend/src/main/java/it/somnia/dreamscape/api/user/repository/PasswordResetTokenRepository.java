package it.somnia.dreamscape.api.user.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.somnia.dreamscape.api.user.model.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // C'è già un link chiesto da poco? Evita di mandare troppe email alla stessa persona
    boolean existsByUserIdAndCreatedAtAfter(UUID userId, Instant since);

    // Un nuovo link annulla quelli vecchi non ancora usati
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    int invalidateAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
