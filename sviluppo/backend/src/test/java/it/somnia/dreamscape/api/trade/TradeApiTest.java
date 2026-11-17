package it.somnia.dreamscape.api.trade;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import tools.jackson.databind.ObjectMapper;

// Scambi (#93): proposta, liste, rifiuto, annullamento e collezione di un amico. Utenti di prova di V902:
// anna_test e bruno_test sono amici, carla_test no. Le carte si danno qui con una INSERT nella collezione
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TradeApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        // anna_test: 2 Lucciola e 1 Nuvola Morbida; bruno_test: 3 Aquilone e 1 Bolla di Sapone
        giveCard("anna_test", "TEST-001", 2);
        giveCard("anna_test", "TEST-002", 1);
        giveCard("bruno_test", "TEST-004", 3);
        giveCard("bruno_test", "TEST-005", 1);
    }

    // ------------------------------------------------------------------ proposta

    @Test
    void proposeTrade_valid_createsAPendingTradeForBoth() throws Exception {
        // When
        propose(anna, brunoId, "Ti va?", items("TEST-001", 1), items("TEST-004", 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.proposer.username").value("anna_test"))
                .andExpect(jsonPath("$.recipient.username").value("bruno_test"))
                .andExpect(jsonPath("$.message").value("Ti va?"))
                .andExpect(jsonPath("$.offered[0].card.code").value("TEST-001"))
                .andExpect(jsonPath("$.offered[0].card.name").value("Lucciola (prova)"))
                .andExpect(jsonPath("$.offered[0].quantity").value(1))
                .andExpect(jsonPath("$.requested[0].card.code").value("TEST-004"))
                .andExpect(jsonPath("$.requested[0].quantity").value(2))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.resolvedAt").doesNotExist());

        // Then: anna lo vede tra i mandati, bruno tra i ricevuti e nel contatore della Dashboard
        mockMvc.perform(get("/api/v1/trades").param("box", "sent").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].recipient.username").value("bruno_test"));
        mockMvc.perform(get("/api/v1/trades").param("box", "received").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].offered[0].card.code").value("TEST-001"));
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.pendingTrades").value(1));
    }

    @Test
    void proposeTrade_offeredCopies_areLockedForOtherTradesButStayInTheCollection() throws Exception {
        // When
        propose(anna, brunoId, null, items("TEST-001", 1), items("TEST-004", 1))
                .andExpect(status().isCreated());

        // Then: 2 copie possedute, 1 sola ancora offribile
        mockMvc.perform(get("/api/v1/me/cards").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.items[0].card.code").value("TEST-001"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].availableForTrade").value(1))
                .andExpect(jsonPath("$.items[1].availableForTrade").value(1));
    }

    @Test
    void proposeTrade_moreCopiesThanTheUnlockedOnes_returnsCardsLockedInTrade() throws Exception {
        // Given: le 2 Lucciola sono già offerte in uno scambio in attesa
        propose(anna, brunoId, null, items("TEST-001", 2), items("TEST-004", 1))
                .andExpect(status().isCreated());

        // When / Then
        propose(anna, brunoId, null, items("TEST-001", 1), items("TEST-005", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARDS_LOCKED_IN_TRADE"))
                .andExpect(jsonPath("$.message")
                        .value("Tutte le tue copie di Lucciola (prova) sono già offerte in scambi in attesa."));
    }

    @Test
    void proposeTrade_moreCopiesThanOwned_returnsCardsNotOwned() throws Exception {
        // When / Then
        propose(anna, brunoId, null, items("TEST-001", 3), items("TEST-004", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARDS_NOT_OWNED"))
                .andExpect(jsonPath("$.message").value("Possiedi 2 copie di Lucciola (prova): ne offri 3."));
    }

    @Test
    void proposeTrade_sameCardTwiceInTheRequest_sumsTheCopies() throws Exception {
        // When / Then: 1 + 1 Lucciola diventano una riga da 2
        propose(anna, brunoId, null, "[" + item("TEST-001", 1) + ", " + item("TEST-001", 1) + "]",
                items("TEST-004", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offered.length()").value(1))
                .andExpect(jsonPath("$.offered[0].quantity").value(2));
    }

    @Test
    void proposeTrade_friendDoesNotHaveTheCopies_returnsRequestedCardsNotAvailable() throws Exception {
        // When / Then: bruno_test ha 1 sola Bolla di Sapone
        propose(anna, brunoId, null, items("TEST-001", 1), items("TEST-005", 2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUESTED_CARDS_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.message")
                        .value("bruno_test ha 1 copia disponibile di Bolla di Sapone (prova): ne chiedi 2."));
    }

    @Test
    void proposeTrade_friendAlreadyOfferedTheCopies_returnsRequestedCardsNotAvailable() throws Exception {
        // Given: bruno_test offre la sua unica Bolla di Sapone in un altro scambio
        propose(bruno, annaId, null, items("TEST-005", 1), items("TEST-002", 1))
                .andExpect(status().isCreated());

        // When / Then
        propose(anna, brunoId, null, items("TEST-001", 1), items("TEST-005", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUESTED_CARDS_NOT_AVAILABLE"));
    }

    @Test
    void proposeTrade_notFriends_returnsNotFriends() throws Exception {
        // When / Then
        propose(anna, userId("carla_test"), null, items("TEST-001", 1), items("TEST-004", 1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_FRIENDS"));
    }

    @Test
    void proposeTrade_toYourself_returnsInvalidTrade() throws Exception {
        // When / Then
        propose(anna, annaId, null, items("TEST-001", 1), items("TEST-002", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRADE"));
    }

    @Test
    void proposeTrade_sameCardOnBothSides_returnsInvalidTrade() throws Exception {
        // Given
        giveCard("bruno_test", "TEST-001", 1);

        // When / Then
        propose(anna, brunoId, null, items("TEST-001", 1), items("TEST-001", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRADE"));
    }

    @Test
    void proposeTrade_unknownRecipient_returnsUserNotFound() throws Exception {
        // When / Then
        propose(anna, UUID.randomUUID(), null, items("TEST-001", 1), items("TEST-004", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void proposeTrade_unknownCard_returnsCardNotFound() throws Exception {
        // When / Then
        propose(anna, brunoId, null, "[{ \"cardId\": \"" + UUID.randomUUID() + "\", \"quantity\": 1 }]",
                items("TEST-004", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
    }

    @Test
    void proposeTrade_nothingRequested_returnsValidationError() throws Exception {
        // When / Then
        propose(anna, brunoId, null, items("TEST-001", 1), "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("requested"));
    }

    // ------------------------------------------------------------------ dettaglio e liste

    @Test
    void getTrade_otherPlayer_returnsTradeNotFound() throws Exception {
        // Given
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-004", 1));

        // When / Then
        mockMvc.perform(get("/api/v1/trades/" + tradeId).header(HttpHeaders.AUTHORIZATION, bearer("carla_test")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRADE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/trades/" + tradeId).header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tradeId));
    }

    @Test
    void listTrades_unknownBox_returnsValidationError() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/trades").param("box", "all").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ------------------------------------------------------------------ rifiuto e annullamento

    @Test
    void rejectTrade_recipient_closesItAndUnlocksTheCopies() throws Exception {
        // Given
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 2), items("TEST-004", 1));

        // When
        mockMvc.perform(post("/api/v1/trades/" + tradeId + "/reject").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        // Then: le copie tornano offribili, lo scambio passa allo storico di tutti e due
        mockMvc.perform(get("/api/v1/me/cards").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.items[0].availableForTrade").value(2));
        mockMvc.perform(get("/api/v1/trades").param("box", "history").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].status").value("REJECTED"));
        mockMvc.perform(get("/api/v1/trades").param("box", "received").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void rejectTrade_proposer_returnsForbidden() throws Exception {
        // Given
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-004", 1));

        // When / Then: chi propone annulla, non rifiuta
        mockMvc.perform(post("/api/v1/trades/" + tradeId + "/reject").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void cancelTrade_proposer_closesIt_andTwiceReturnsNotPending() throws Exception {
        // Given
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-004", 1));

        // When / Then
        mockMvc.perform(post("/api/v1/trades/" + tradeId + "/cancel").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/v1/trades/" + tradeId + "/cancel").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRADE_NOT_PENDING"));
    }

    @Test
    void cancelTrade_recipient_returnsForbidden() throws Exception {
        // Given
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-004", 1));

        // When / Then
        mockMvc.perform(post("/api/v1/trades/" + tradeId + "/cancel").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ amicizia tolta e account cancellato

    @Test
    void removeFriend_cancelsThePendingTrades() throws Exception {
        // Given: uno scambio per parte
        String annaTrade = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-004", 1));
        String brunoTrade = proposeAndGetId(bruno, annaId, items("TEST-005", 1), items("TEST-002", 1));

        // When
        mockMvc.perform(delete("/api/v1/friends/" + brunoId).header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isNoContent());

        // Then
        expectStatus(anna, annaTrade, "CANCELLED");
        expectStatus(anna, brunoTrade, "CANCELLED");
        mockMvc.perform(get("/api/v1/me/cards").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.items[0].availableForTrade").value(2));
    }

    @Test
    void deleteAccount_cancelsThePendingTrades() throws Exception {
        // Given
        String tradeId = proposeAndGetId(anna, brunoId, items("TEST-001", 1), items("TEST-004", 1));

        // When
        mockMvc.perform(delete("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bruno)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"" + TEST_PASSWORD + "\" }"))
                .andExpect(status().isNoContent());

        // Then
        expectStatus(anna, tradeId, "CANCELLED");
    }

    // ------------------------------------------------------------------ collezione di un amico

    @Test
    void listFriendCollection_friend_returnsItsCardsWithTheAvailableCopies() throws Exception {
        // Given: bruno_test offre 2 Aquilone ad anna_test
        propose(bruno, annaId, null, items("TEST-004", 2), items("TEST-002", 1))
                .andExpect(status().isCreated());

        // When / Then
        mockMvc.perform(get("/api/v1/users/" + brunoId + "/cards").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].card.code").value("TEST-004"))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].availableForTrade").value(1));
    }

    @Test
    void listFriendCollection_notAFriend_returnsNotFriends() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/users/" + userId("carla_test") + "/cards").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_FRIENDS"));
    }

    @Test
    void listFriendCollection_unknownUser_returnsUserNotFound() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID() + "/cards").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    // ------------------------------------------------------------------ utilità

    private ResultActions propose(String bearer, UUID recipientId, String message, String offered, String requested)
            throws Exception {
        String body = "{ \"recipientId\": \"" + recipientId + "\", "
                + (message == null ? "" : "\"message\": \"" + message + "\", ")
                + "\"offered\": " + offered + ", \"requested\": " + requested + " }";
        return mockMvc.perform(post("/api/v1/trades").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String proposeAndGetId(String bearer, UUID recipientId, String offered, String requested)
            throws Exception {
        String body = propose(bearer, recipientId, null, offered, requested)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asString();
    }

    private void expectStatus(String bearer, String tradeId, String expected) throws Exception {
        mockMvc.perform(get("/api/v1/trades/" + tradeId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected));
    }

    // Una lista con una sola carta: [{ "cardId": "…", "quantity": n }]
    private String items(String cardCode, int quantity) {
        return "[" + item(cardCode, quantity) + "]";
    }

    private String item(String cardCode, int quantity) {
        UUID cardId = jdbcTemplate.queryForObject("SELECT id FROM cards WHERE code = ?", UUID.class, cardCode);
        return "{ \"cardId\": \"" + cardId + "\", \"quantity\": " + quantity + " }";
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
