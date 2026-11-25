package it.somnia.dreamscape.api.mission;

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
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.mission.dto.response.MissionResponse;
import it.somnia.dreamscape.api.mission.service.MissionService;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Missioni nelle partite vere (#125): il matchmaking fa avanzare solo chi cerca la partita, una sfida tra amici
// tutti e due. anna_test e bruno_test sono amici, carla_test no (V902); ognuno usa il suo set iniziale come mazzo
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MissionApiTest {

    private static final String PASSWORD = "prova-dreamscape";
    private static final int DAILY_REWARD = 100;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MissionService missionService;

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
    void playCasual_matchmaking_completesOnlyTheDailyOfWhoSearched() throws Exception {
        // Given: carla è l'unica con un mazzo classificato, quindi è l'avversaria
        mockMvc.perform(put("/api/v1/me/ranked-deck").header(HttpHeaders.AUTHORIZATION, carla)
                        .contentType(MediaType.APPLICATION_JSON).content(deckBody(carlaDeck)))
                .andExpect(status().isNoContent());
        int annaCredits = credits("anna_test");
        int carlaCredits = credits("carla_test");

        // When
        mockMvc.perform(post("/api/v1/matches/casual").header(HttpHeaders.AUTHORIZATION, anna)
                        .contentType(MediaType.APPLICATION_JSON).content(deckBody(annaDeck)))
                .andExpect(status().isCreated());

        // Then
        assertThat(daily("anna_test").isCompleted()).isTrue();
        assertThat(credits("anna_test")).isEqualTo(annaCredits + DAILY_REWARD);
        assertThat(daily("carla_test").progress()).isZero();
        assertThat(credits("carla_test")).isEqualTo(carlaCredits);
    }

    @Test
    void acceptChallenge_friendsMatch_completesTheDailyOfBoth() throws Exception {
        // Given
        int annaCredits = credits("anna_test");
        int brunoCredits = credits("bruno_test");
        String challengeId = objectMapper.readTree(mockMvc.perform(post("/api/v1/challenges")
                                .header(HttpHeaders.AUTHORIZATION, anna)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ \"opponentId\": \"" + userId("bruno_test") + "\", \"deckId\": \""
                                        + annaDeck + "\" }"))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString())
                .path("id").asString();

        // When
        mockMvc.perform(post("/api/v1/challenges/{challengeId}/accept", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, bruno)
                        .contentType(MediaType.APPLICATION_JSON).content(deckBody(brunoDeck)))
                .andExpect(status().isOk());

        // Then
        assertThat(daily("anna_test").isCompleted()).isTrue();
        assertThat(daily("bruno_test").isCompleted()).isTrue();
        assertThat(credits("anna_test")).isEqualTo(annaCredits + DAILY_REWARD);
        assertThat(credits("bruno_test")).isEqualTo(brunoCredits + DAILY_REWARD);
    }

    @Test
    void getSummary_afterTheFirstMatchOfTheDay_showsTheDailyCompleted() throws Exception {
        // Given
        mockMvc.perform(put("/api/v1/me/ranked-deck").header(HttpHeaders.AUTHORIZATION, carla)
                        .contentType(MediaType.APPLICATION_JSON).content(deckBody(carlaDeck)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/matches/casual").header(HttpHeaders.AUTHORIZATION, anna)
                        .contentType(MediaType.APPLICATION_JSON).content(deckBody(annaDeck)))
                .andExpect(status().isCreated());

        // When / Then: la giornaliera è fatta, la settimanale no (una partita rapida non conta)
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missions.length()").value(2))
                .andExpect(jsonPath("$.missions[0].code").value("DAILY_PLAY"))
                .andExpect(jsonPath("$.missions[0].name").value("Missione giornaliera"))
                .andExpect(jsonPath("$.missions[0].description").value("Gioca una partita."))
                .andExpect(jsonPath("$.missions[0].progress").value(1))
                .andExpect(jsonPath("$.missions[0].goalCount").value(1))
                .andExpect(jsonPath("$.missions[0].rewardCredits").value(DAILY_REWARD))
                .andExpect(jsonPath("$.missions[0].isCompleted").value(true))
                .andExpect(jsonPath("$.missions[0].resetsAt").isNotEmpty())
                .andExpect(jsonPath("$.missions[1].code").value("WEEKLY_WIN"))
                .andExpect(jsonPath("$.missions[1].progress").value(0))
                .andExpect(jsonPath("$.missions[1].goalCount").value(5))
                .andExpect(jsonPath("$.missions[1].rewardCredits").value(250))
                .andExpect(jsonPath("$.missions[1].isCompleted").value(false));
    }

    private MissionResponse daily(String username) {
        return missionService.missionsOf(userId(username)).stream()
                .filter(mission -> mission.code().equals("DAILY_PLAY"))
                .findFirst()
                .orElseThrow();
    }

    private static String deckBody(UUID deckId) {
        return "{ \"deckId\": \"" + deckId + "\" }";
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

    private int credits(String username) {
        entityManager.flush();
        entityManager.clear();
        return jdbcTemplate.queryForObject("SELECT credits FROM users WHERE username = ?", Integer.class, username);
    }
}
