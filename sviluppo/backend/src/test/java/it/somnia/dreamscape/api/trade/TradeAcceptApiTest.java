package it.somnia.dreamscape.api.trade;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

// Accettazione degli scambi e avvisi su mazzi e ultima copia (#93). Utenti di prova di V902: anna_test e
// bruno_test sono amici. anna_test ha le carte di un mazzo Dreams valido, bruno_test due carte Nightmares
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TradeAcceptApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";

    // 7 Comuni e 3 Rare di Dreams, 2 copie ciascuna: un mazzo valido da 20 carte
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

    private String anna;
    private String bruno;
    private UUID annaId;
    private UUID brunoId;

    @BeforeEach
    void giveCards() throws Exception {
        anna = bearer("anna_test");
        bruno = bearer("bruno_test");
        annaId = userId("anna_test");
        brunoId = userId("bruno_test");
        DREAMS_DECK.forEach(code -> giveCard("anna_test", code, 2));
        giveCard("bruno_test", "TEST-014", 2);
        giveCard("bruno_test", "TEST-015", 1);
    }

    // ------------------------------------------------------------------ accettazione

    @Test
    void acceptTrade_recipient_movesTheCardsBothWays() throws Exception {
        // Given: anna_test dà 1 Lucciola e chiede 2 Ombra Strisciante
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-014", 2));

        // When
        accept(bruno, tradeId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        // Then
        expectCopies("anna_test", "TEST-001", 1);
        expectCopies("anna_test", "TEST-014", 2);
        expectCopies("bruno_test", "TEST-001", 1);
        // bruno_test ha dato tutte le sue Ombra Strisciante: la riga non c'è più
        expectCopies("bruno_test", "TEST-014", 0);
        mockMvc.perform(get("/api/v1/trades").param("box", "history").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.items[0].status").value("ACCEPTED"));
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.pendingTrades").value(0));
    }

    @Test
    void acceptTrade_proposer_returnsForbidden() throws Exception {
        // Given
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-014", 1));

        // When / Then
        accept(anna, tradeId)
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptTrade_twice_returnsNotPending() throws Exception {
        // Given
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-014", 1));
        accept(bruno, tradeId).andExpect(status().isOk());

        // When / Then: le carte non si spostano due volte
        accept(bruno, tradeId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRADE_NOT_PENDING"));
        expectCopies("anna_test", "TEST-014", 1);
    }

    @Test
    void acceptTrade_copiesNowOfferedElsewhere_returns409AndStaysPending() throws Exception {
        // Given: anna_test chiede l'unica Ragno del Soffitto, che poi bruno_test offre in un altro scambio
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-015", 1));
        String otherTradeId = proposeAndGetId(bruno, annaId, items("TEST-015", 1), items("TEST-002", 1));

        // When / Then: nessuna carta si muove, lo scambio resta in attesa
        accept(bruno, tradeId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUESTED_CARDS_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.message").value("Non hai copie disponibili di Ragno del Soffitto (prova)."));
        expectStatus(bruno, tradeId, "PENDING");
        expectCopies("anna_test", "TEST-001", 2);

        // Annullato l'altro scambio, la copia torna disponibile e si può accettare
        mockMvc.perform(post("/api/v1/trades/" + otherTradeId + "/cancel").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(status().isOk());
        accept(bruno, tradeId).andExpect(status().isOk());
        expectCopies("anna_test", "TEST-015", 1);
    }

    @Test
    void acceptTrade_noLongerFriends_cancelsTheTrade() throws Exception {
        // Given: l'amicizia sparisce senza passare da DELETE /friends (che annullerebbe già lo scambio)
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-014", 1));
        jdbcTemplate.update("DELETE FROM friendships WHERE requester_id IN (?, ?) AND addressee_id IN (?, ?)",
                annaId, brunoId, annaId, brunoId);
        entityManager.flush();
        entityManager.clear();

        // When / Then
        accept(bruno, tradeId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRADE_CANCELLED"));
        expectStatus(anna, tradeId, "CANCELLED");
        expectCopies("anna_test", "TEST-001", 2);
    }

    // ------------------------------------------------------------------ avvisi

    @Test
    void previewTrade_cardUsedInAValidDeck_warnsTheDeckBecomesInvalid() throws Exception {
        // Given: un mazzo valido che usa 2 Lucciola, ed è il mazzo classificato
        String deckId = createDeck(anna, "Sogni", DREAMS_DECK, 2);
        setRankedDeck("anna_test", deckId);

        // When / Then: se ne offre 1, nel mazzo ne resterebbe 1 sola
        preview(anna, items("TEST-001", 1), items("TEST-014", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckWarnings.length()").value(1))
                .andExpect(jsonPath("$.deckWarnings[0].deckId").value(deckId))
                .andExpect(jsonPath("$.deckWarnings[0].deckName").value("Sogni"))
                .andExpect(jsonPath("$.deckWarnings[0].isRanked").value(true))
                .andExpect(jsonPath("$.deckWarnings[0].cardName").value("Lucciola (prova)"))
                .andExpect(jsonPath("$.deckWarnings[0].outcome").value("INVALID"))
                .andExpect(jsonPath("$.lastCopyWarnings.length()").value(0));
        // L'anteprima non salva niente
        mockMvc.perform(get("/api/v1/trades").param("box", "sent").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void previewTrade_allTheCopies_warnsItIsTheLastCopy() throws Exception {
        // When / Then
        preview(anna, items("TEST-001", 2), items("TEST-014", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckWarnings.length()").value(0))
                .andExpect(jsonPath("$.lastCopyWarnings.length()").value(1))
                .andExpect(jsonPath("$.lastCopyWarnings[0].cardName").value("Lucciola (prova)"));
    }

    @Test
    void previewTrade_sameChecksAsTheProposal() throws Exception {
        // When / Then: più copie di quelle possedute
        preview(anna, items("TEST-001", 3), items("TEST-014", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARDS_NOT_OWNED"));
    }

    @Test
    void getTradeWarnings_recipient_warnsAboutTheCardsItWouldGive() throws Exception {
        // Given: anna_test chiede a bruno_test la sua unica Ragno del Soffitto
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-015", 1));

        // When / Then
        mockMvc.perform(get("/api/v1/trades/" + tradeId + "/deck-warnings").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckWarnings.length()").value(0))
                .andExpect(jsonPath("$.lastCopyWarnings[0].cardName").value("Ragno del Soffitto (prova)"));
    }

    @Test
    void fullArtInADeck_withAFreeNormalCopy_switchesToTheNormalVersion() throws Exception {
        // Given: il mazzo usa la full art della Sognatrice Lucida; anna_test ha anche una copia normale libera
        giveCard("anna_test", "TEST-013-FA", 1);
        giveCard("anna_test", "TEST-013", 1);
        String deckId = createDeckWithFullArt(anna);
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-013-FA", 1), items("TEST-014", 1));

        // When: l'avviso dice che il mazzo passerà alla versione normale
        mockMvc.perform(get("/api/v1/trades/" + tradeId + "/deck-warnings").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.deckWarnings[0].deckId").value(deckId))
                .andExpect(jsonPath("$.deckWarnings[0].cardName").value("Sognatrice Lucida (prova)"))
                .andExpect(jsonPath("$.deckWarnings[0].outcome").value("NORMAL_VERSION"))
                .andExpect(jsonPath("$.lastCopyWarnings[0].cardName").value("Sognatrice Lucida (prova) (full art)"));
        accept(bruno, tradeId).andExpect(status().isOk());

        // Then: il mazzo usa la carta normale e resta valido
        mockMvc.perform(get("/api/v1/decks/" + deckId).header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.totalCards").value(20))
                .andExpect(jsonPath("$.cards[?(@.card.code == 'TEST-013')].quantity").value(1))
                .andExpect(jsonPath("$.cards[?(@.card.code == 'TEST-013-FA')]").isEmpty());
    }

    @Test
    void fullArtInADeck_withoutANormalCopy_makesTheDeckInvalid() throws Exception {
        // Given: il mazzo usa la full art e anna_test non ha la carta normale
        giveCard("anna_test", "TEST-013-FA", 1);
        String deckId = createDeckWithFullArt(anna);

        // When / Then
        preview(anna, items("TEST-013-FA", 1), items("TEST-014", 1))
                .andExpect(jsonPath("$.deckWarnings[0].deckId").value(deckId))
                .andExpect(jsonPath("$.deckWarnings[0].outcome").value("INVALID"));
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-013-FA", 1), items("TEST-014", 1));
        accept(bruno, tradeId).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/decks/" + deckId).header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.isValid").value(false));
    }

    // ------------------------------------------------------------------ utilità

    private ResultActions accept(String bearer, String tradeId) throws Exception {
        return mockMvc.perform(post("/api/v1/trades/" + tradeId + "/accept").header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions preview(String bearer, String offered, String requested) throws Exception {
        return mockMvc.perform(post("/api/v1/trades/preview").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(tradeBody(brunoId, offered, requested)));
    }

    private String proposeAndGetId(String bearer, UUID recipientId, String offered, String requested)
            throws Exception {
        String body = mockMvc.perform(post("/api/v1/trades").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(tradeBody(recipientId, offered, requested)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asString();
    }

    private static String tradeBody(UUID recipientId, String offered, String requested) {
        return "{ \"recipientId\": \"" + recipientId + "\", \"offered\": " + offered + ", \"requested\": "
                + requested + " }";
    }

    private void expectStatus(String bearer, String tradeId, String expected) throws Exception {
        mockMvc.perform(get("/api/v1/trades/" + tradeId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.status").value(expected));
    }

    // Le copie nel database, senza passare da Hibernate
    private void expectCopies(String username, String cardCode, int expected) {
        entityManager.flush();
        Integer copies = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(uc.quantity), 0) FROM user_cards uc
                JOIN users u ON u.id = uc.user_id JOIN cards c ON c.id = uc.card_id
                WHERE u.username = ? AND c.code = ?""", Integer.class, username, cardCode);
        org.junit.jupiter.api.Assertions.assertEquals(expected, copies, username + " " + cardCode);
    }

    private String createDeck(String bearer, String name, List<String> codes, int copies) throws Exception {
        List<String> cards = new ArrayList<>();
        codes.forEach(code -> cards.add(deckCard(code, copies)));
        return saveDeck(bearer, name, cards);
    }

    // 14 Comuni, 5 Rare e la full art della Leggendaria: 20 carte
    private String createDeckWithFullArt(String bearer) throws Exception {
        List<String> cards = new ArrayList<>();
        List.of("TEST-001", "TEST-002", "TEST-003", "TEST-004", "TEST-005", "TEST-006", "TEST-007", "TEST-008",
                "TEST-009").forEach(code -> cards.add(deckCard(code, 2)));
        cards.add(deckCard("TEST-010", 1));
        cards.add(deckCard("TEST-013-FA", 1));
        return saveDeck(bearer, "Lucida", cards);
    }

    private String saveDeck(String bearer, String name, List<String> cards) throws Exception {
        String body = "{ \"name\": \"" + name + "\", \"cards\": [" + String.join(", ", cards) + "] }";
        String response = mockMvc.perform(post("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isValid").value(true))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asString();
    }

    private String deckCard(String cardCode, int copies) {
        return "{ \"cardId\": \"" + cardId(cardCode) + "\", \"quantity\": " + copies + " }";
    }

    private void setRankedDeck(String username, String deckId) {
        entityManager.flush();
        jdbcTemplate.update("UPDATE users SET ranked_deck_id = ? WHERE username = ?", UUID.fromString(deckId),
                username);
        entityManager.clear();
    }

    private String items(String cardCode, int quantity) {
        return "[{ \"cardId\": \"" + cardId(cardCode) + "\", \"quantity\": " + quantity + " }]";
    }

    private UUID cardId(String cardCode) {
        return jdbcTemplate.queryForObject("SELECT id FROM cards WHERE code = ?", UUID.class, cardCode);
    }

    private void giveCard(String username, String cardCode, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO user_cards (user_id, card_id, quantity)
                SELECT u.id, c.id, ? FROM users u, cards c WHERE u.username = ? AND c.code = ?""",
                quantity, username, cardCode);
    }

    private UUID userId(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", UUID.class, username);
    }

    private String bearer(String username) throws Exception {
        String body = "{ \"login\": \"" + username + "\", \"password\": \"" + TEST_PASSWORD + "\" }";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(response).path("accessToken").asString();
    }
}
