package it.somnia.dreamscape.api.deck.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.somnia.dreamscape.api.deck.model.Deck;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findAllByUser_Id(UUID userId);

    // I mazzi della pagina "I miei mazzi", in ordine di nome
    List<Deck> findAllByUser_IdOrderByNameAsc(UUID userId);

    // Un mazzo solo se è dell'utente: quello di un altro risponde come se non esistesse
    Optional<Deck> findByIdAndUser_Id(UUID deckId, UUID userId);

    // Nome già usato dall'utente (vincolo uq_decks_user_name)
    boolean existsByUser_IdAndName(UUID userId, String name);

    // Come sopra, senza contare il mazzo che si sta modificando
    boolean existsByUser_IdAndNameAndIdNot(UUID userId, String name, UUID deckId);

    // Elimina il mazzo con una query diretta: le carte si cancellano con ON DELETE CASCADE e il mazzo
    // classificato con ON DELETE SET NULL, senza che Hibernate debba caricarle prima
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Deck d WHERE d.id = :deckId")
    void deleteDeckById(@Param("deckId") UUID deckId);
}
