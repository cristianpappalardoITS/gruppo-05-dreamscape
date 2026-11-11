package it.somnia.dreamscape.api.deck.repository;

import it.somnia.dreamscape.api.deck.model.Deck;
import it.somnia.dreamscape.api.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeckRepository extends JpaRepository<Deck, UUID> {
    List<Deck> findAllByUser_Id(UUID userId);
}
