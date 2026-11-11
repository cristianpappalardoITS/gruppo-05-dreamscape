package it.somnia.dreamscape.api.deck.repository;

import it.somnia.dreamscape.api.deck.model.DeckCards;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeckCardsRepository extends JpaRepository<DeckCards, UUID> {
    List<DeckCards> findAllByDeck_Id(UUID deckId);
}
