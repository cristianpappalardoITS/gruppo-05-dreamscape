package it.somnia.dreamscape.api.deck.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.deck.model.UserCards;
import jakarta.persistence.LockModeType;

// I filtri della collezione (GET /me/cards) sono in UserCardsSpecifications
public interface UserCardsRepository extends JpaRepository<UserCards, UUID>, JpaSpecificationExecutor<UserCards> {

    List<UserCards> findUserCardsByUser_Id(UUID userId);

    // Blocca le righe della collezione di un giocatore per le carte indicate (SELECT ... FOR UPDATE), in ordine
    // di id così due transazioni le prendono nello stesso ordine. Lo usano gli scambi (#93): due proposte o
    // accettazioni insieme non possono usare le stesse copie
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCards uc WHERE uc.user.id = :userId AND uc.card.id IN :cardIds ORDER BY uc.id")
    List<UserCards> findForUpdate(UUID userId, Collection<UUID> cardIds);
}
