package it.somnia.dreamscape.api.deck.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.somnia.dreamscape.api.deck.model.Deck;
import it.somnia.dreamscape.api.deck.model.DeckCards;

public interface DeckCardsRepository extends JpaRepository<DeckCards, UUID> {

    List<DeckCards> findAllByDeck_Id(UUID deckId);

    // In quanti mazzi compare almeno una di queste carte (backoffice: avviso prima di disattivare una carta)
    @Query("SELECT COUNT(DISTINCT dc.deck.id) FROM DeckCards dc WHERE dc.card.id IN :cardIds")
    long countDecksUsing(@Param("cardIds") Collection<UUID> cardIds);

    // Le carte di più mazzi con una sola query (lista dei mazzi), con la carta e la sua carta base
    @EntityGraph(attributePaths = { "card", "card.baseCard" })
    List<DeckCards> findAllByDeck_IdIn(Collection<UUID> deckIds);

    // Svuota il mazzo prima di salvare le carte nuove. Query diretta e subito eseguita: con delete() e
    // save() Hibernate inserirebbe prima di cancellare e violerebbe uq_deck_cards_deck_card
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("DELETE FROM DeckCards dc WHERE dc.deck = :deck")
    void deleteAllCardsOf(@Param("deck") Deck deck);
}
