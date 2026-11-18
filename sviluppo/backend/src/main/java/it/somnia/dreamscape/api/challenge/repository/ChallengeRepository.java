package it.somnia.dreamscape.api.challenge.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.challenge.model.Challenge;
import it.somnia.dreamscape.api.challenge.model.ChallengeStatus;
import jakarta.persistence.LockModeType;

public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {

    // Ricevute e mandate: solo quelle in attesa, dalla più recente
    @EntityGraph(attributePaths = { "challenger", "opponent", "challengerDeck" })
    List<Challenge> findByOpponent_IdAndStatusOrderByCreatedAtDesc(UUID opponentId, ChallengeStatus status);

    @EntityGraph(attributePaths = { "challenger", "opponent", "challengerDeck" })
    List<Challenge> findByChallenger_IdAndStatusOrderByCreatedAtDesc(UUID challengerId, ChallengeStatus status);

    // Blocca la riga della sfida: accettazione, rifiuto e annullamento insieme decidono uno alla volta
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Challenge c WHERE c.id = :challengeId")
    Optional<Challenge> findByIdForUpdate(UUID challengeId);

    // C'è già una sfida in attesa tra i due, in una delle due direzioni (uq_challenges_pending_pair)?
    @Query("""
            SELECT COUNT(c) > 0 FROM Challenge c
            WHERE c.status = it.somnia.dreamscape.api.challenge.model.ChallengeStatus.PENDING
              AND ((c.challenger.id = :firstUserId AND c.opponent.id = :secondUserId)
                OR (c.challenger.id = :secondUserId AND c.opponent.id = :firstUserId))
            """)
    boolean existsPendingBetween(UUID firstUserId, UUID secondUserId);

    // Le sfide in attesa tra due giocatori (amicizia tolta). Bloccate: se un'accettazione è in corso si aspetta
    // che finisca, così una sfida appena giocata non torna CANCELLED
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM Challenge c
            WHERE c.status = it.somnia.dreamscape.api.challenge.model.ChallengeStatus.PENDING
              AND ((c.challenger.id = :firstUserId AND c.opponent.id = :secondUserId)
                OR (c.challenger.id = :secondUserId AND c.opponent.id = :firstUserId))
            """)
    List<Challenge> findPendingBetween(UUID firstUserId, UUID secondUserId);

    // Le sfide in attesa di un giocatore, da entrambi i lati (account cancellato). Bloccate come sopra
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM Challenge c
            WHERE c.status = it.somnia.dreamscape.api.challenge.model.ChallengeStatus.PENDING
              AND (c.challenger.id = :userId OR c.opponent.id = :userId)
            """)
    List<Challenge> findPendingOf(UUID userId);

    // Per il contatore della Dashboard: le sfide che aspettano una risposta dell'utente
    int countByOpponent_IdAndStatus(UUID opponentId, ChallengeStatus status);
}
