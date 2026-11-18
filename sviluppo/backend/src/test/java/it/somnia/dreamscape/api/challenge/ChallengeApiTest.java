package it.somnia.dreamscape.api.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
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
import it.somnia.dreamscape.engine.battle.BattleEngine;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Sfide tra amici e partite (/challenges, /matches): anna_test e bruno_test sono amici, carla_test no (V902).
// Ognuno riceve il set iniziale dalla pagina Benvenuto e lo usa come mazzo, sempre valido. Le partite le gioca il
// motore dentro il backend (EngineTestConfiguration)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChallengeApiTest {

    private static final String PASSWORD = "prova-dreamscape";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private String anna;
    private String bruno;
    private String carla;
    private UUID annaDeck;
    private UUID brunoDeck;

    @BeforeEach
    void prepareDecks() throws Exception {
        anna = bearer("anna_test");
        bruno = bearer("bruno_test");
        carla = bearer("carla_test");
        annaDeck = starterDeck(anna, "DREAMS");
        brunoDeck = starterDeck(bruno, "NIGHTMARES");
    }

    @Test
    void createChallenge_friendWithValidDeck_isPendingForBoth() throws Exception {
        // When
        challenge(anna, userId("bruno_test"), annaDeck)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.challenger.username").value("anna_test"))
                .andExpect(jsonPath("$.opponent.username").value("bruno_test"))
                .andExpect(jsonPath("$.challengerDeckId").value(annaDeck.toString()))
                .andExpect(jsonPath("$.challengerDeckValid").value(true))
                .andExpect(jsonPath("$.challengerDeckProblems.length()").value(0));

        // Then
        mockMvc.perform(get("/api/v1/challenges").param("box", "sent").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/v1/challenges").param("box", "received").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].challenger.username").value("anna_test"));
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.pendingChallenges").value(1));
    }

    @Test
    void createChallenge_notAFriend_returnsNotFriends() throws Exception {
        challenge(anna, userId("carla_test"), annaDeck)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_FRIENDS"));
    }

    @Test
    void createChallenge_yourself_returnsInvalidChallenge() throws Exception {
        challenge(anna, userId("anna_test"), annaDeck)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CHALLENGE"));
    }

    @Test
    void createChallenge_secondOneInEitherDirection_returnsAlreadyPending() throws Exception {
        // Given
        challenge(anna, userId("bruno_test"), annaDeck).andExpect(status().isCreated());

        // When / Then: anche bruno verso anna
        challenge(anna, userId("bruno_test"), annaDeck)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_ALREADY_PENDING"));
        challenge(bruno, userId("anna_test"), brunoDeck)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_ALREADY_PENDING"));
    }

    @Test
    void createChallenge_invalidDeck_returnsInvalidDeckWithProblems() throws Exception {
        // Given: un mazzo di una carta sola, scritto a mano (dall'API non si salva)
        UUID smallDeck = jdbcTemplate.queryForObject("""
                INSERT INTO decks (user_id, name) SELECT id, 'Mazzo corto' FROM users WHERE username = 'anna_test'
                RETURNING id""", UUID.class);
        jdbcTemplate.update("INSERT INTO deck_cards (deck_id, card_id, quantity) SELECT ?, card_id, 1 FROM user_cards "
                + "WHERE user_id = ? LIMIT 1", smallDeck, userId("anna_test"));

        // When / Then
        challenge(anna, userId("bruno_test"), smallDeck)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DECK"))
                .andExpect(jsonPath("$.problems").isNotEmpty());
    }

    @Test
    void acceptChallenge_validDecks_playsACasualMatchForBoth() throws Exception {
        // Given
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));

        // When
        String matchId = objectMapper.readTree(accept(bruno, challengeId, brunoDeck)
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .path("matchId").asString();

        // Then: la partita la vedono tutti e due, con il replay e le copie dei mazzi
        for (String player : new String[] { anna, bruno }) {
            mockMvc.perform(get("/api/v1/matches/{matchId}", matchId).header(HttpHeaders.AUTHORIZATION, player))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matchType").value("CASUAL"))
                    .andExpect(jsonPath("$.challenger.username").value("anna_test"))
                    .andExpect(jsonPath("$.opponent.username").value("bruno_test"))
                    .andExpect(jsonPath("$.ratingChange").doesNotExist())
                    .andExpect(jsonPath("$.creditsEarned").value(0))
                    .andExpect(jsonPath("$.startingLifePoints").value(20))
                    .andExpect(jsonPath("$.engineVersion").value(BattleEngine.ENGINE_VERSION))
                    .andExpect(jsonPath("$.coinTossWinner").isNotEmpty())
                    .andExpect(jsonPath("$.rounds").isNotEmpty())
                    .andExpect(jsonPath("$.rounds[0].events[0].phase").value("START"))
                    .andExpect(jsonPath("$.rounds[0].events[0].type").value("CARD_PLAYED"))
                    .andExpect(jsonPath("$.decks.challenger.deckId").value(annaDeck.toString()))
                    .andExpect(jsonPath("$.decks.opponent.deckId").value(brunoDeck.toString()))
                    .andExpect(jsonPath("$.decks.challenger.cards").isNotEmpty());
        }
        mockMvc.perform(get("/api/v1/challenges").param("box", "received").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.length()").value(0));
        forgetLoadedData();
        String status = jdbcTemplate.queryForObject("SELECT status FROM challenges WHERE id = ?::uuid", String.class,
                challengeId);
        assertThat(status).isEqualTo("COMPLETED");
    }

    @Test
    void getMatch_someoneWhoDidNotPlay_returnsMatchNotFound() throws Exception {
        // Given
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));
        String matchId = objectMapper.readTree(accept(bruno, challengeId, brunoDeck).andReturn().getResponse()
                .getContentAsString()).path("matchId").asString();

        // When / Then
        mockMvc.perform(get("/api/v1/matches/{matchId}", matchId).header(HttpHeaders.AUTHORIZATION, carla))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
    }

    @Test
    void acceptChallenge_challengerDeckNoLongerValid_staysPendingAndBothSeeIt() throws Exception {
        // Given: dopo la sfida anna perde tutte le copie di una carta del mazzo (es. le ha scambiate)
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));
        jdbcTemplate.update("DELETE FROM user_cards WHERE user_id = ? AND card_id = (SELECT card_id FROM deck_cards "
                + "WHERE deck_id = ? ORDER BY card_id LIMIT 1)", userId("anna_test"), annaDeck);
        forgetLoadedData();

        // When
        accept(bruno, challengeId, brunoDeck)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGER_DECK_INVALID"))
                .andExpect(jsonPath("$.problems").isNotEmpty());

        // Then: la sfida resta in attesa e tutti e due vedono che il mazzo va sistemato
        mockMvc.perform(get("/api/v1/challenges").param("box", "sent").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].challengerDeckValid").value(false))
                .andExpect(jsonPath("$[0].challengerDeckProblems").isNotEmpty());
        mockMvc.perform(get("/api/v1/challenges").param("box", "received").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$[0].challengerDeckValid").value(false));
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.pendingChallenges").value(1));
    }

    @Test
    void acceptChallenge_challengerDeckDeleted_returnsChallengerDeckInvalid() throws Exception {
        // Given
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));
        mockMvc.perform(delete("/api/v1/decks/{deckId}", annaDeck).header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isNoContent());
        forgetLoadedData();

        // When / Then
        accept(bruno, challengeId, brunoDeck)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGER_DECK_INVALID"))
                .andExpect(jsonPath("$.problems[0]").value("Il mazzo della sfida è stato cancellato."));
        mockMvc.perform(get("/api/v1/challenges").param("box", "sent").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$[0].challengerDeckId").doesNotExist())
                .andExpect(jsonPath("$[0].challengerDeckValid").value(false));
    }

    @Test
    void acceptChallenge_ownInvalidDeck_returnsInvalidDeck() throws Exception {
        // Given
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));
        jdbcTemplate.update("DELETE FROM user_cards WHERE user_id = ?", userId("bruno_test"));
        forgetLoadedData();

        // When / Then
        accept(bruno, challengeId, brunoDeck)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DECK"));
    }

    @Test
    void acceptChallenge_byWhoSentIt_returnsForbidden() throws Exception {
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));

        accept(anna, challengeId, annaDeck)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void acceptChallenge_noLongerFriends_isCancelledByTheFriendshipRemoval() throws Exception {
        // Given: anna toglie bruno dagli amici dopo la sfida (decisione 026)
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));
        mockMvc.perform(delete("/api/v1/friends/{userId}", userId("bruno_test"))
                        .header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isNoContent());
        forgetLoadedData();

        // When / Then
        accept(bruno, challengeId, brunoDeck)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_NOT_PENDING"));
        mockMvc.perform(get("/api/v1/challenges").param("box", "received").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void acceptChallenge_friendshipGoneWithoutTheEvent_cancelsTheChallenge() throws Exception {
        // Given: l'amicizia sparisce senza passare da DELETE /friends (es. dati sistemati a mano)
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));
        jdbcTemplate.update("DELETE FROM friendships WHERE (requester_id = ? AND addressee_id = ?) "
                + "OR (requester_id = ? AND addressee_id = ?)", userId("anna_test"), userId("bruno_test"),
                userId("bruno_test"), userId("anna_test"));
        forgetLoadedData();

        // When / Then
        accept(bruno, challengeId, brunoDeck)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_CANCELLED"));
        forgetLoadedData();
        String status = jdbcTemplate.queryForObject("SELECT status FROM challenges WHERE id = ?::uuid", String.class,
                challengeId);
        assertThat(status).isEqualTo("CANCELLED");
    }

    @Test
    void rejectAndCancel_onlyTheRightSide_closeTheChallenge() throws Exception {
        // Given
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));

        // When / Then: chi ha sfidato non rifiuta, chi è sfidato non annulla
        mockMvc.perform(post("/api/v1/challenges/{challengeId}/reject", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/challenges/{challengeId}/cancel", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/challenges/{challengeId}/reject", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(status().isNoContent());

        // Then: chiusa per tutti e due
        mockMvc.perform(post("/api/v1/challenges/{challengeId}/cancel", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_NOT_PENDING"));
        challenge(anna, userId("bruno_test"), annaDeck).andExpect(status().isCreated());
    }

    @Test
    void cancelChallenge_byWhoSentIt_returnsNoContent() throws Exception {
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));

        mockMvc.perform(post("/api/v1/challenges/{challengeId}/cancel", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/challenges").param("box", "sent").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void challenges_someoneElse_returnsChallengeNotFound() throws Exception {
        String challengeId = idOf(challenge(anna, userId("bruno_test"), annaDeck));

        mockMvc.perform(post("/api/v1/challenges/{challengeId}/reject", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, carla))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_NOT_FOUND"));
    }

    @Test
    void listChallenges_unknownBox_returnsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/challenges").param("box", "history").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private ResultActions challenge(String token, UUID opponentId, UUID deckId) throws Exception {
        return mockMvc.perform(post("/api/v1/challenges").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"opponentId\": \"" + opponentId + "\", \"deckId\": \"" + deckId + "\" }"));
    }

    private ResultActions accept(String token, String challengeId, UUID deckId) throws Exception {
        return mockMvc.perform(post("/api/v1/challenges/{challengeId}/accept", challengeId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"deckId\": \"" + deckId + "\" }"));
    }

    private String idOf(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString()).path("id").asString();
    }

    // Il set iniziale della fazione (20 carte, sempre un mazzo valido) diventa un mazzo
    private UUID starterDeck(String token, String faction) throws Exception {
        String welcome = mockMvc.perform(post("/api/v1/me/welcome").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"starterFaction\": \"" + faction + "\" }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Map<String, Integer> copies = new LinkedHashMap<>();
        for (JsonNode entry : objectMapper.readTree(welcome).path("starterCards")) {
            copies.merge(entry.path("card").path("id").asString(), 1, Integer::sum);
        }
        StringBuilder cards = new StringBuilder();
        copies.forEach((cardId, quantity) -> cards.append(cards.isEmpty() ? "" : ", ")
                .append("{ \"cardId\": \"").append(cardId).append("\", \"quantity\": ").append(quantity).append(" }"));
        String deck = mockMvc.perform(post("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Set iniziale\", \"cards\": [ " + cards + " ] }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(deck).path("id").asString());
    }

    private String bearer(String username) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"login\": \"" + username + "\", \"password\": \"" + PASSWORD + "\" }"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(response).path("accessToken").asString();
    }

    private UUID userId(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", UUID.class, username);
    }

    // I dati cambiati con JdbcTemplate o da un'altra richiesta: Hibernate deve rileggerli dal database
    private void forgetLoadedData() {
        entityManager.flush();
        entityManager.clear();
    }
}
