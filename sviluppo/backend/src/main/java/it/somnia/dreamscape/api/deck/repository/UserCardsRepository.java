package it.somnia.dreamscape.api.deck.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import it.somnia.dreamscape.api.deck.model.UserCards;

// I filtri della collezione (GET /me/cards) sono in UserCardsSpecifications
public interface UserCardsRepository extends JpaRepository<UserCards, UUID>, JpaSpecificationExecutor<UserCards> {

    List<UserCards> findUserCardsByUser_Id(UUID userId);
}
