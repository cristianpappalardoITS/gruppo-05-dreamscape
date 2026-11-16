package it.somnia.dreamscape.api.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.databind.ObjectMapper;

// Mazzi dell'utente collegato (GET, POST, PUT, DELETE /decks), con le carte di prova di V900
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeckApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";

    // Un mazzo valido di Dreams: 7 Comuni e 3 Rare, 2 copie ciascuna = 20 carte (6 Rare, il massimo è 8)
    private static final List<String> DREAMS_DECK = List.of("TEST-001", "TEST-002", "TEST-003", "TEST-004",
            "TEST-005", "TEST-006", "TEST-007", "TEST-008", "TEST-009", "TEST-010");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Il test è una sola transazione: dopo un UPDATE diretto Hibernate ricorderebbe i dati di prima
    @PersistenceContext
    private EntityManager entityManager;

    private String bearer;

    @BeforeEach
    void logInWithCards() throws Exception {
        String body = "{ \"login\": \"anna_test\", \"password\": \"" + TEST_PASSWORD + "\" }";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        bearer = "Bearer " + objectMapper.readTree(response).path("accessToken").asString();
        DREAMS_DECK.forEach(code -> giveCards("anna_test", code, 2));
    }

    @Test
    void createDeck_validDeck_returnsCreatedDeckWithItsCards() throws Exception {
        createDeck(deckJson("Sogni tranquilli", twoOfEach(DREAMS_DECK)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Sogni tranquilli"))
                .andExpect(jsonPath("$.totalCards").value(20))
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.isRanked").value(false))
                .andExpect(jsonPath("$.problems.length()").value(0))
                .andExpect(jsonPath("$.cards.length()").value(10))
                .andExpect(jsonPath("$.cards[0].card.code").value("TEST-001"))
                .andExpect(jsonPath("$.cards[0].card.faction.code").value("DREAMS"))
                .andExpect(jsonPath("$.cards[0].quantity").value(2));
    }

    @Test
    void createDeck_nameWithSpaces_savesItTrimmed() throws Exception {
        createDeck(deckJson("  Sogni tranquilli  ", twoOfEach(DREAMS_DECK)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sogni tranquilli"));
    }

    @Test
    void createDeck_eighteenCards_returnsInvalidDeckWithProblems() throws Exception {
        // Given: 9 carte diverse da 2 copie
        Map<String, Integer> cards = twoOfEach(DREAMS_DECK.subList(0, 9));

        // When / Then
        createDeck(deckJson("Incompleto", cards))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_DECK"))
                .andExpect(jsonPath("$.message").value("Il mazzo ha 18 carte: ne servono 20."))
                .andExpect(jsonPath("$.problems.length()").value(1))
                .andExpect(jsonPath("$.problems[0]").value("Il mazzo ha 18 carte: ne servono 20."))
                .andExpect(jsonPath("$.fieldErrors.length()").value(0));
        assertThat(countDecks("anna_test")).isZero();
    }

    @Test
    void createDeck_cardNotOwned_returnsTheProblem() throws Exception {
        // Given: TEST-011 (Epica) non è nella collezione al posto di una Rara
        Map<String, Integer> cards = twoOfEach(DREAMS_DECK.subList(0, 9));
        cards.put("TEST-011", 2);

        // When / Then
        createDeck(deckJson("Con una carta in più", cards))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problems[0]").value("Non possiedi la carta Arcobaleno Notturno (prova)."));
    }

    @Test
    void createDeck_unknownCardId_returnsInvalidDeck() throws Exception {
        // Given
        String body = """
                { "name": "Fantasma", "cards": [ { "cardId": "%s", "quantity": 2 } ] }""".formatted(UUID.randomUUID());

        // When / Then
        createDeck(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DECK"))
                .andExpect(jsonPath("$.problems[0]").value("Una delle carte scelte non esiste."));
    }

    @Test
    void createDeck_sameCardTwiceInRequest_sumsTheCopies() throws Exception {
        // Given: TEST-001 mandata due volte con 1 copia, come 2 copie
        UUID firstCardId = cardId("TEST-001");
        String extra = ", { \"cardId\": \"%s\", \"quantity\": 1 }".formatted(firstCardId);
        Map<String, Integer> cards = twoOfEach(DREAMS_DECK);
        cards.put("TEST-001", 1);
        String body = deckJson("Doppione", cards).replace(" ] }", extra + " ] }");

        // When / Then
        createDeck(body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cards[0].card.code").value("TEST-001"))
                .andExpect(jsonPath("$.cards[0].quantity").value(2));
    }

    @Test
    void createDeck_blankName_returnsValidationError() throws Exception {
        createDeck(deckJson(" ", twoOfEach(DREAMS_DECK)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.problems").doesNotExist());
    }

    @Test
    void createDeck_nameAlreadyUsed_returnsDeckNameTaken() throws Exception {
        // Given
        createDeck(deckJson("Sogni tranquilli", twoOfEach(DREAMS_DECK))).andExpect(status().isCreated());

        // When / Then
        createDeck(deckJson("Sogni tranquilli", twoOfEach(DREAMS_DECK)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DECK_NAME_TAKEN"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void createDeck_withoutLogin_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/decks").contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson("Senza accesso", twoOfEach(DREAMS_DECK))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listDecks_twoDecks_returnsThemByNameWithRankedFlag() throws Exception {
        // Given
        UUID zeta = createdDeckId("Zeta");
        createdDeckId("Alfa");
        jdbcTemplate.update("UPDATE users SET ranked_deck_id = ? WHERE username = 'anna_test'", zeta);
        forgetLoadedData();

        // When / Then
        mockMvc.perform(get("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alfa"))
                .andExpect(jsonPath("$[0].isRanked").value(false))
                .andExpect(jsonPath("$[0].cards.length()").value(10))
                .andExpect(jsonPath("$[1].name").value("Zeta"))
                .andExpect(jsonPath("$[1].isRanked").value(true))
                .andExpect(jsonPath("$[1].isValid").value(true));
    }

    @Test
    void listDecks_noDecks_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getDeck_afterLosingACopy_isStillSavedButInvalid() throws Exception {
        // Given: dopo uno scambio resta 1 copia di TEST-001 (decisione 024)
        UUID deckId = createdDeckId("Sogni tranquilli");
        jdbcTemplate.update("""
                UPDATE user_cards SET quantity = 1
                WHERE user_id = (SELECT id FROM users WHERE username = 'anna_test')
                  AND card_id = (SELECT id FROM cards WHERE code = 'TEST-001')""");
        forgetLoadedData();

        // When / Then
        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCards").value(20))
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.problems[0]").value("Possiedi 1 copia di Lucciola (prova): nel mazzo ce ne sono 2."));
    }

    @Test
    void getDeck_withDeactivatedCard_isInvalidWithTheReason() throws Exception {
        // Given: l'admin disattiva una carta del mazzo (decisione 026)
        UUID deckId = createdDeckId("Sogni tranquilli");
        jdbcTemplate.update("UPDATE cards SET is_active = FALSE WHERE code = 'TEST-008'");
        forgetLoadedData();

        // When / Then
        mockMvc.perform(get("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[0].id").value(deckId.toString()))
                .andExpect(jsonPath("$[0].isValid").value(false))
                .andExpect(jsonPath("$[0].problems", hasItem("La carta Faro Lontano (prova) non è più disponibile.")));
    }

    @Test
    void getDeck_otherUsersDeck_returnsDeckNotFound() throws Exception {
        // Given
        UUID brunoDeckId = jdbcTemplate.queryForObject("""
                INSERT INTO decks (user_id, name) SELECT id, 'Di Bruno' FROM users WHERE username = 'bruno_test'
                RETURNING id""", UUID.class);

        // When / Then
        mockMvc.perform(get("/api/v1/decks/{deckId}", brunoDeckId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
    }

    @Test
    void updateDeck_newNameAndCards_replacesThem() throws Exception {
        // Given: una Rara al posto di un'altra
        UUID deckId = createdDeckId("Sogni tranquilli");
        giveCards("anna_test", "TEST-034", 2);
        Map<String, Integer> cards = twoOfEach(DREAMS_DECK.subList(0, 9));
        cards.put("TEST-034", 2);

        // When / Then
        mockMvc.perform(put("/api/v1/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(deckJson("Sogni e ricordi", cards)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deckId.toString()))
                .andExpect(jsonPath("$.name").value("Sogni e ricordi"))
                .andExpect(jsonPath("$.totalCards").value(20))
                .andExpect(jsonPath("$.cards.length()").value(10))
                .andExpect(jsonPath("$.cards[9].card.code").value("TEST-034"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM deck_cards WHERE deck_id = ?", Integer.class,
                deckId)).isEqualTo(10);
    }

    @Test
    void updateDeck_sameName_isAllowed() throws Exception {
        // Given
        UUID deckId = createdDeckId("Sogni tranquilli");

        // When / Then
        mockMvc.perform(put("/api/v1/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson("Sogni tranquilli", twoOfEach(DREAMS_DECK))))
                .andExpect(status().isOk());
    }

    @Test
    void updateDeck_invalidCards_keepsTheOldDeck() throws Exception {
        // Given
        UUID deckId = createdDeckId("Sogni tranquilli");

        // When
        mockMvc.perform(put("/api/v1/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckJson("Rovinato", twoOfEach(DREAMS_DECK.subList(0, 5)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DECK"));

        // Then
        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.name").value("Sogni tranquilli"))
                .andExpect(jsonPath("$.totalCards").value(20));
    }

    @Test
    void updateDeck_nameOfAnotherDeck_returnsDeckNameTaken() throws Exception {
        // Given
        createdDeckId("Alfa");
        UUID betaId = createdDeckId("Beta");

        // When / Then
        mockMvc.perform(put("/api/v1/decks/{deckId}", betaId).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(deckJson("Alfa", twoOfEach(DREAMS_DECK))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DECK_NAME_TAKEN"));
    }

    @Test
    void deleteDeck_rankedDeck_removesItAndLeavesNoRankedDeck() throws Exception {
        // Given
        UUID deckId = createdDeckId("Sogni tranquilli");
        jdbcTemplate.update("UPDATE users SET ranked_deck_id = ? WHERE username = 'anna_test'", deckId);
        forgetLoadedData();

        // When
        mockMvc.perform(delete("/api/v1/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        // Then
        mockMvc.perform(get("/api/v1/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT ranked_deck_id FROM users WHERE username = 'anna_test'", UUID.class)).isNull();
    }

    @Test
    void deleteDeck_otherUsersDeck_returnsDeckNotFound() throws Exception {
        // Given
        UUID brunoDeckId = jdbcTemplate.queryForObject("""
                INSERT INTO decks (user_id, name) SELECT id, 'Di Bruno' FROM users WHERE username = 'bruno_test'
                RETURNING id""", UUID.class);

        // When / Then
        mockMvc.perform(delete("/api/v1/decks/{deckId}", brunoDeckId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
        assertThat(countDecks("bruno_test")).isEqualTo(1);
    }

    // Come in una richiesta nuova: Hibernate rilegge dal database quello che è cambiato con un UPDATE diretto
    private void forgetLoadedData() {
        entityManager.flush();
        entityManager.clear();
    }

    private ResultActions createDeck(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID createdDeckId(String name) throws Exception {
        String response = createDeck(deckJson(name, twoOfEach(DREAMS_DECK)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).path("id").asString());
    }

    private static Map<String, Integer> twoOfEach(List<String> codes) {
        return codes.stream().collect(Collectors.toMap(code -> code, code -> 2, (a, b) -> a, LinkedHashMap::new));
    }

    private String deckJson(String name, Map<String, Integer> quantityByCode) {
        String cards = quantityByCode.entrySet().stream()
                .map(entry -> "{ \"cardId\": \"%s\", \"quantity\": %d }".formatted(cardId(entry.getKey()),
                        entry.getValue()))
                .collect(Collectors.joining(", "));
        return "{ \"name\": \"%s\", \"cards\": [ %s ] }".formatted(name, cards);
    }

    private UUID cardId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM cards WHERE code = ?", UUID.class, code);
    }

    private int countDecks(String username) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM decks d JOIN users u ON u.id = d.user_id WHERE u.username = ?",
                Integer.class, username);
    }

    private void giveCards(String username, String cardCode, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO user_cards (user_id, card_id, quantity)
                SELECT u.id, c.id, ? FROM users u, cards c WHERE u.username = ? AND c.code = ?""",
                quantity, username, cardCode);
    }
}
