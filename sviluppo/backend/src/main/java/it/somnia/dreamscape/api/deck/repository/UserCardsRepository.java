package it.somnia.dreamscape.api.deck.repository;

import it.somnia.dreamscape.api.deck.model.UserCards;
import it.somnia.dreamscape.api.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserCardsRepository extends JpaRepository<UserCards, UUID> {

    List<UserCards> findUserCardsByUser_Id(UUID userId);
}
