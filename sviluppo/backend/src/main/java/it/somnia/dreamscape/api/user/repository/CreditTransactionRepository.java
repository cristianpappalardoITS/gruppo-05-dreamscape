package it.somnia.dreamscape.api.user.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.user.model.CreditTransaction;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {

    // Pagina Crediti: dal movimento più recente (a parità di data, id v7 ordinato nel tempo)
    Page<CreditTransaction> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    // Movimenti di un tipo in [from, to): es. le vittorie classificate già premiate oggi
    @Query("""
            SELECT COUNT(t) FROM CreditTransaction t
            WHERE t.user.id = :userId AND t.type = :type AND t.createdAt >= :from AND t.createdAt < :to
            """)
    long countOfTypeBetween(UUID userId, CreditTransactionType type, Instant from, Instant to);

    // I crediti di un tipo presi da un giocatore con una partita (creditsEarned nel risultato); 0 se nessuno
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM CreditTransaction t
            WHERE t.user.id = :userId AND t.matchId = :matchId AND t.type = :type
            """)
    long sumAmountOfMatch(UUID userId, UUID matchId, CreditTransactionType type);
}
