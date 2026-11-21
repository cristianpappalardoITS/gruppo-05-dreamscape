package it.somnia.dreamscape.api.user.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.user.model.CreditTransaction;
import it.somnia.dreamscape.api.user.model.UserRole;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {

    // Pagina Crediti: dal movimento più recente (a parità di data, id v7 ordinato nel tempo)
    Page<CreditTransaction> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    // I movimenti dei giocatori da un momento in poi, sommati per tipo (Dashboard admin): un tipo può avere
    // movimenti in entrata e in uscita (ADMIN_ADJUSTMENT), quindi si sommano a parte
    @Query("""
            SELECT t.type AS transactionType, COUNT(t) AS transactions,
                   COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0) AS earned,
                   COALESCE(SUM(CASE WHEN t.amount < 0 THEN -t.amount ELSE 0 END), 0) AS spent
            FROM CreditTransaction t
            WHERE t.user.role = :role AND t.createdAt >= :from
            GROUP BY t.type
            ORDER BY t.type
            """)
    List<CreditTotalsView> sumByTypeSince(UserRole role, Instant from);
}
