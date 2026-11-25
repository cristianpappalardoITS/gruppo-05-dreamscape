package it.somnia.dreamscape.api.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Mazzo classificato, matchmaking (classificata e rapida) e storico (#127). anna e bruno sono amici, carla no.
// Ognuno ha il set iniziale della sua fazione come mazzo; le partite le gioca il motore dentro il backend
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchmakingApiTest {

    private static final String PASSWORD = "prova-dreamscape";
    private static final int RANKED_WIN_REWARD = 30;

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
    private UUID carlaDeck;

    @BeforeEach
    void prepareDecks() throws Exception {
        anna = bearer("anna_test");
        bruno = bearer("bruno_test");
        carla = bearer("carla_test");
        annaDeck = starterDeck(anna, "DREAMS");
        brunoDeck = starterDeck(bruno, "NIGHTMARES");
        carlaDeck = starterDeck(carla, "MEMORIES");
    }

    @Test
    void setRankedDeck_validDeck_isTheRankedOneInDecksAndDashboard() throws Exception {
        // When
        setRankedDeck(anna, annaDeck).andExpect(status().isNoContent());

        // Then
        mockMvc.perform(get("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$[0].isRanked").value(true));
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.hasRankedDeck").value(true));
    }

    @Test
    void setRankedDeck_null_removesTheRankedDeck() throws Exception {
        // Given
        setRankedDeck(anna, annaDeck).andExpect(status().isNoContent());

        // When
        mockMvc.perform(put("/api/v1/me/ranked-deck").header(HttpHeaders.AUTHORIZATION, anna)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"deckId\": null }"))
                .andExpect(status().isNoContent());

        // Then
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.hasRankedDeck").value(false));
    }

    @Test
    void setRankedDeck_invalidDeck_returnsInvalidDeckWithProblems() throws Exception {
        setRankedDeck(anna, smallDeck("anna_test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DECK"))
                .andExpect(jsonPath("$.problems").isNotEmpty());
    }

    @Test
    void setRankedDeck_deckOfAnotherPlayer_returnsDeckNotFound() throws Exception {
        setRankedDeck(anna, brunoDeck)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
    }

    @Test
    void playRanked_nobodyHasARankedDeck_returnsNoOpponentFound() throws Exception {
        playRanked(anna, annaDeck)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_OPPONENT_FOUND"));
    }

    @Test
    void playRanked_onlyRankedDeckNoLongerValid_returnsNoOpponentFound() throws Exception {
        // Given: carla ha ceduto tutte le carte, il suo mazzo classificato non è più valido
        setRankedDeck(carla, carlaDeck).andExpect(status().isNoContent());
        jdbcTemplate.update("DELETE FROM user_cards WHERE user_id = ?", userId("carla_test"));
        forgetLoadedData();

        // When / Then
        playRanked(anna, annaDeck)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_OPPONENT_FOUND"));
    }

    @Test
    void playRanked_invalidOwnDeck_returnsInvalidDeck() throws Exception {
        setRankedDeck(carla, carlaDeck).andExpect(status().isNoContent());

        playRanked(anna, smallDeck("anna_test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DECK"));
    }

    @Test
    void playRanked_withoutDeck_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/matches/ranked").header(HttpHeaders.AUTHORIZATION, anna)
                        .contentType(MediaType.APPLICATION_JSON).content("{ }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void playRanked_opponentWithRankedDeck_updatesBothRatingsAndRewardsTheWin() throws Exception {
        // Given: solo carla (non amica di anna) ha un mazzo classificato
        setRankedDeck(carla, carlaDeck).andExpect(status().isNoContent());
        int annaCredits = credits("anna_test");

        // When
        JsonNode match = json(playRanked(anna, annaDeck).andExpect(status().isCreated()));

        // Then
        assertThat(match.path("matchType").asString()).isEqualTo("RANKED");
        assertThat(match.path("opponent").path("username").asString()).isEqualTo("carla_test");
        int annaChange = match.path("ratingChange").path("challenger").asInt();
        assertThat(match.path("ratingChange").path("opponent").asInt()).isEqualTo(-annaChange);
        assertThat(rating("anna_test")).isEqualTo(1000 + annaChange);
        assertThat(rating("carla_test")).isEqualTo(1000 - annaChange);
        boolean annaWon = userId("anna_test").toString().equals(match.path("winnerId").asString());
        int expectedCredits = annaWon ? RANKED_WIN_REWARD : 0;
        assertThat(match.path("creditsEarned").asInt()).isEqualTo(expectedCredits);
        assertThat(credits("anna_test")).isEqualTo(annaCredits + expectedCredits);
    }

    @Test
    void playRanked_friendAsOpponent_winGivesRankButNoCredits() throws Exception {
        // Given: carla diventa amica di anna ed è l'unica con un mazzo classificato. Memories è la fazione su cui
        // Dreams ha il vantaggio: con bruno (Nightmares) anna può perdere decine di partite di fila
        setRankedDeck(carla, carlaDeck).andExpect(status().isNoContent());
        forgetLoadedData();
        jdbcTemplate.update("INSERT INTO friendships (requester_id, addressee_id, status, accepted_at) "
                + "VALUES (?, ?, 'ACCEPTED', now())", userId("anna_test"), userId("carla_test"));
        int annaCredits = credits("anna_test");

        // When: si gioca finché anna non vince
        JsonNode win = playRankedUntilWin(anna, annaDeck, "anna_test");

        // Then
        assertThat(win.path("opponent").path("username").asString()).isEqualTo("carla_test");
        assertThat(win.path("creditsEarned").asInt()).isZero();
        assertThat(credits("anna_test")).isEqualTo(annaCredits);
    }

    @Test
    void playRanked_alreadyThreeRewardedWinsToday_noMoreCredits() throws Exception {
        // Given: tre vittorie già premiate oggi
        setRankedDeck(carla, carlaDeck).andExpect(status().isNoContent());
        forgetLoadedData();
        for (int i = 0; i < 3; i++) {
            jdbcTemplate.update("UPDATE users SET credits = credits + 30 WHERE username = 'anna_test'");
            jdbcTemplate.update("INSERT INTO credit_transactions (user_id, type, amount, balance_after) "
                    + "SELECT id, 'MATCH_REWARD', 30, credits FROM users WHERE username = 'anna_test'");
        }
        forgetLoadedData();
        int annaCredits = credits("anna_test");

        // When
        JsonNode win = playRankedUntilWin(anna, annaDeck, "anna_test");

        // Then
        assertThat(win.path("creditsEarned").asInt()).isZero();
        assertThat(credits("anna_test")).isEqualTo(annaCredits);
    }

    @Test
    void playRanked_opponentsAtDailyLimit_comeLastEvenIfCloser() throws Exception {
        // Given: carla ha il punteggio di anna, bruno 600 punti in più; anna ha già giocato 3 classificate con carla
        setRankedDeck(carla, carlaDeck).andExpect(status().isNoContent());
        setRankedDeck(bruno, brunoDeck).andExpect(status().isNoContent());
        // Prima si scrivono i mazzi classificati: altrimenti Hibernate riscrive la riga di bruno con il punteggio vecchio
        forgetLoadedData();
        jdbcTemplate.update("UPDATE users SET rating = 1600 WHERE username = 'bruno_test'");
        forgetLoadedData();
        for (int i = 0; i < 3; i++) {
            JsonNode match = json(playRanked(anna, annaDeck).andExpect(status().isCreated()));
            assertThat(match.path("opponent").path("username").asString()).isEqualTo("carla_test");
        }

        // When
        JsonNode fourth = json(playRanked(anna, annaDeck).andExpect(status().isCreated()));

        // Then
        assertThat(fourth.path("opponent").path("username").asString()).isEqualTo("bruno_test");
        assertThat(fourth.path("ratingChange").isNull()).isFalse();
    }

    @Test
    void playCasual_opponentWithRankedDeck_noRankNoCredits() throws Exception {
        // Given
        setRankedDeck(carla, carlaDeck).andExpect(status().isNoContent());
        int annaCredits = credits("anna_test");

        // When
        JsonNode match = json(mockMvc.perform(post("/api/v1/matches/casual").header(HttpHeaders.AUTHORIZATION, anna)
                        .contentType(MediaType.APPLICATION_JSON).content(deckBody(annaDeck)))
                .andExpect(status().isCreated()));

        // Then
        assertThat(match.path("matchType").asString()).isEqualTo("CASUAL");
        assertThat(match.path("ratingChange").isNull()).isTrue();
        assertThat(match.path("creditsEarned").asInt()).isZero();
        assertThat(rating("anna_test")).isEqualTo(1000);
        assertThat(credits("anna_test")).isEqualTo(annaCredits);
    }

    @Test
    void listMatches_bothPlayers_seeTheMatchFromTheirSide() throws Exception {
        // Given: una classificata cercata da anna contro carla e una sfida tra amici con bruno
        setRankedDeck(carla, carlaDeck).andExpect(status().isNoContent());
        JsonNode ranked = json(playRanked(anna, annaDeck).andExpect(status().isCreated()));
        String challenge = "{ \"opponentId\": \"" + userId("bruno_test") + "\", \"deckId\": \"" + annaDeck + "\" }";
        String challengeId = json(mockMvc.perform(post("/api/v1/challenges").header(HttpHeaders.AUTHORIZATION, anna)
                        .contentType(MediaType.APPLICATION_JSON).content(challenge))
                .andExpect(status().isCreated())).path("id").asString();
        mockMvc.perform(post("/api/v1/challenges/" + challengeId + "/accept").header(HttpHeaders.AUTHORIZATION, bruno)
                        .contentType(MediaType.APPLICATION_JSON).content(deckBody(brunoDeck)))
                .andExpect(status().isOk());

        // When / Then: anna le vede tutte e due, la più recente prima
        mockMvc.perform(get("/api/v1/me/matches").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].matchType").value("CASUAL"))
                .andExpect(jsonPath("$.items[0].opponent.username").value("bruno_test"))
                .andExpect(jsonPath("$.items[1].id").value(ranked.path("id").asString()))
                .andExpect(jsonPath("$.items[1].startedByMe").value(true))
                .andExpect(jsonPath("$.items[1].ratingChange")
                        .value(ranked.path("ratingChange").path("challenger").asInt()));
        // Il filtro per tipo
        mockMvc.perform(get("/api/v1/me/matches?type=RANKED").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].matchType").value("RANKED"));
        // carla, scelta come avversaria, la vede dal suo lato
        String carlaResult = ranked.path("winnerId").isNull() ? "DRAW"
                : ranked.path("winnerId").asString().equals(userId("carla_test").toString()) ? "WIN" : "LOSS";
        mockMvc.perform(get("/api/v1/me/matches").header(HttpHeaders.AUTHORIZATION, carla))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].opponent.username").value("anna_test"))
                .andExpect(jsonPath("$.items[0].startedByMe").value(false))
                .andExpect(jsonPath("$.items[0].result").value(carlaResult))
                .andExpect(jsonPath("$.items[0].ratingChange")
                        .value(ranked.path("ratingChange").path("opponent").asInt()));
    }

    @Test
    void listMatches_unknownType_returnsMalformedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/me/matches?type=FRIENDLY").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void listMatches_sizeTooBig_returnsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/me/matches?size=500").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private ResultActions setRankedDeck(String token, UUID deckId) throws Exception {
        return mockMvc.perform(put("/api/v1/me/ranked-deck").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(deckBody(deckId)));
    }

    private ResultActions playRanked(String token, UUID deckId) throws Exception {
        return mockMvc.perform(post("/api/v1/matches/ranked").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(deckBody(deckId)));
    }

    // Il motore decide il vincitore: si gioca finché non vince chi cerca
    private JsonNode playRankedUntilWin(String token, UUID deckId, String username) throws Exception {
        String winnerId = userId(username).toString();
        for (int attempt = 0; attempt < 30; attempt++) {
            JsonNode match = json(playRanked(token, deckId).andExpect(status().isCreated()));
            if (winnerId.equals(match.path("winnerId").asString())) {
                return match;
            }
        }
        throw new AssertionError(username + " non ha mai vinto in 30 partite");
    }

    // Un mazzo di una carta sola: non valido
    private UUID smallDeck(String username) {
        UUID deckId = jdbcTemplate.queryForObject("""
                INSERT INTO decks (user_id, name) SELECT id, 'Mazzo corto' FROM users WHERE username = ?
                RETURNING id""", UUID.class, username);
        jdbcTemplate.update("INSERT INTO deck_cards (deck_id, card_id, quantity) SELECT ?, card_id, 1 FROM user_cards "
                + "WHERE user_id = ? LIMIT 1", deckId, userId(username));
        return deckId;
    }

    private static String deckBody(UUID deckId) {
        return "{ \"deckId\": \"" + deckId + "\" }";
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
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

    private int rating(String username) {
        forgetLoadedData();
        return jdbcTemplate.queryForObject("SELECT rating FROM users WHERE username = ?", Integer.class, username);
    }

    private int credits(String username) {
        forgetLoadedData();
        return jdbcTemplate.queryForObject("SELECT credits FROM users WHERE username = ?", Integer.class, username);
    }

    // I dati cambiati con JdbcTemplate o da un'altra richiesta: Hibernate deve rileggerli dal database
    private void forgetLoadedData() {
        entityManager.flush();
        entityManager.clear();
    }
}
