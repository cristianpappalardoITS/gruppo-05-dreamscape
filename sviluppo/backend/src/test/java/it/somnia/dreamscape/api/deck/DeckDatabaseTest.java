package it.somnia.dreamscape.api.deck;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;

// Le regole dei mazzi che controlla PostgreSQL (V3): nomi unici, una riga per carta con le copie in
// quantity, mazzo classificato e cancellazioni a catena (#88). Le regole del regolamento (20 carte,
// copie massime per rarità, possesso) stanno nel service: le provano DeckApiTest e DeckValidatorTest (#80)
// e, con il commit di ogni richiesta, DeckIntegrationTest.
// Ogni test finisce con un rollback, quindi una sola violazione per test.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class DeckDatabaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertDeck_sameNameForSameUser_isRejected() {
        // Given
        UUID anna = userId("anna_test");
        insertDeck(anna, "Sogni lucidi");

        // When
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> insertDeck(anna, "Sogni lucidi"));

        // Then
        assertTrue(exception.getMessage().contains("uq_decks_user_name"), exception.getMessage());
    }

    @Test
    void insertDeck_sameNameForAnotherUser_isAllowed() {
        // Given
        insertDeck(userId("anna_test"), "Sogni lucidi");

        // When / Then
        assertDoesNotThrow(() -> insertDeck(userId("bruno_test"), "Sogni lucidi"));
    }

    @Test
    void insertDeckCard_zeroCopies_isRejected() {
        // Given
        UUID deckId = insertDeck(userId("anna_test"), "Sogni lucidi");

        // When
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> insertDeckCard(deckId, "TEST-001", 0));

        // Then
        assertTrue(exception.getMessage().contains("chk_deck_cards_quantity"), exception.getMessage());
    }

    @Test
    void insertDeckCard_twoCopiesInOneRow_isAllowed() {
        // Given: Comuni, Rare ed Epiche possono stare nel mazzo in 2 copie (decisione 017)
        UUID deckId = insertDeck(userId("anna_test"), "Sogni lucidi");

        // When
        insertDeckCard(deckId, "TEST-001", 2);

        // Then
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT quantity FROM deck_cards WHERE deck_id = ?", Integer.class, deckId));
    }

    @Test
    void insertDeckCard_sameCardInASecondRow_isRejected() {
        // Given: le copie della stessa carta stanno in una sola riga, in quantity;
        // una seconda riga per la stessa carta è un errore del service
        UUID deckId = insertDeck(userId("anna_test"), "Sogni lucidi");
        insertDeckCard(deckId, "TEST-001", 1);

        // When
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> insertDeckCard(deckId, "TEST-001", 1));

        // Then
        assertTrue(exception.getMessage().contains("uq_deck_cards_deck_card"), exception.getMessage());
    }

    @Test
    void deleteDeck_rankedDeck_leavesUserWithoutRankedDeck() {
        // Given
        UUID anna = userId("anna_test");
        UUID deckId = insertDeck(anna, "Classificato");
        jdbcTemplate.update("UPDATE users SET ranked_deck_id = ? WHERE id = ?", deckId, anna);

        // When
        jdbcTemplate.update("DELETE FROM decks WHERE id = ?", deckId);

        // Then: il giocatore resta, senza mazzo classificato (il matchmaking non lo sceglie più)
        assertNull(jdbcTemplate.queryForObject("SELECT ranked_deck_id FROM users WHERE id = ?", UUID.class, anna));
    }

    @Test
    void deleteDeck_withCards_deletesItsCards() {
        // Given
        UUID deckId = insertDeck(userId("anna_test"), "Sogni lucidi");
        insertDeckCard(deckId, "TEST-001", 2);
        insertDeckCard(deckId, "TEST-002", 1);

        // When
        jdbcTemplate.update("DELETE FROM decks WHERE id = ?", deckId);

        // Then
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM deck_cards WHERE deck_id = ?", Integer.class, deckId));
    }

    @Test
    void deleteUser_withDecks_deletesDecksAndTheirCards() {
        // Given: l'app non cancella gli utenti (li anonimizza), ma il database resta coerente se succede
        UUID anna = userId("anna_test");
        UUID deckId = insertDeck(anna, "Sogni lucidi");
        insertDeckCard(deckId, "TEST-001", 2);

        // When
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", anna);

        // Then
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM decks WHERE user_id = ?", Integer.class, anna));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM deck_cards WHERE deck_id = ?", Integer.class, deckId));
    }

    private UUID userId(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", UUID.class, username);
    }

    private UUID insertDeck(UUID userId, String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO decks (user_id, name) VALUES (?, ?) RETURNING id", UUID.class, userId, name);
    }

    private void insertDeckCard(UUID deckId, String cardCode, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO deck_cards (deck_id, card_id, quantity)
                SELECT ?, id, ? FROM cards WHERE code = ?""", deckId, quantity, cardCode);
    }
}
