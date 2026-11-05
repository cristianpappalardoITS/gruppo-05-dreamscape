package it.somnia.dreamscape.api.user.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.user.model.CreditTransaction;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {

    // Pagina Crediti: dal movimento più recente (a parità di data, id v7 ordinato nel tempo)
    Page<CreditTransaction> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);
}
