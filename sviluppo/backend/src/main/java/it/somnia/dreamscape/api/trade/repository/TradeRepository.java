package it.somnia.dreamscape.api.trade.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.trade.model.Trade;
import it.somnia.dreamscape.api.trade.model.TradeStatus;
import jakarta.persistence.LockModeType;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

    // Ricevuti e mandati: solo quelli in attesa, dal più recente
    @EntityGraph(attributePaths = { "proposer", "recipient" })
    Page<Trade> findByRecipient_IdAndStatusOrderByCreatedAtDesc(UUID recipientId, TradeStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = { "proposer", "recipient" })
    Page<Trade> findByProposer_IdAndStatusOrderByCreatedAtDesc(UUID proposerId, TradeStatus status,
            Pageable pageable);

    // Storico: gli scambi chiusi dell'utente, da entrambi i lati, dal più recente
    @EntityGraph(attributePaths = { "proposer", "recipient" })
    @Query("""
            SELECT t FROM Trade t
            WHERE (t.proposer.id = :userId OR t.recipient.id = :userId)
              AND t.status <> it.somnia.dreamscape.api.trade.model.TradeStatus.PENDING
            ORDER BY t.resolvedAt DESC, t.createdAt DESC
            """)
    Page<Trade> findHistory(UUID userId, Pageable pageable);

    // Blocca la riga dello scambio: rifiuto, annullamento e accettazione insieme decidono uno alla volta
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trade t WHERE t.id = :tradeId")
    Optional<Trade> findByIdForUpdate(UUID tradeId);

    // Gli scambi in attesa tra due giocatori, in tutte e due le direzioni (amicizia tolta). Bloccati: se
    // un'accettazione è in corso si aspetta che finisca, e uno scambio appena accettato non torna CANCELLED
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t FROM Trade t
            WHERE t.status = it.somnia.dreamscape.api.trade.model.TradeStatus.PENDING
              AND ((t.proposer.id = :firstUserId AND t.recipient.id = :secondUserId)
                OR (t.proposer.id = :secondUserId AND t.recipient.id = :firstUserId))
            """)
    List<Trade> findPendingBetween(UUID firstUserId, UUID secondUserId);

    // Gli scambi in attesa di un giocatore, da entrambi i lati (account cancellato). Bloccati come sopra
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t FROM Trade t
            WHERE t.status = it.somnia.dreamscape.api.trade.model.TradeStatus.PENDING
              AND (t.proposer.id = :userId OR t.recipient.id = :userId)
            """)
    List<Trade> findPendingOf(UUID userId);

    // Per il contatore della Dashboard: gli scambi che aspettano una risposta dell'utente
    int countByRecipient_IdAndStatus(UUID recipientId, TradeStatus status);
}
