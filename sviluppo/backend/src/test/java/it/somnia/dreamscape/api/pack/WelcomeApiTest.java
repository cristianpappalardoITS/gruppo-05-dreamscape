package it.somnia.dreamscape.api.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

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

// Pagina Benvenuto (GET e POST /me/welcome) con i pacchetti veri di V101: un set iniziale per fazione
// (14 Comuni e 6 Rare) e due pacchetti di benvenuto da 5 carte, il secondo con l'Epica garantita.
// Nelle carte di prova ogni fazione ha 7 Comuni e 3 Rare: con al massimo 2 copie il set è sempre lo stesso.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WelcomeApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";
    private static final String DREAMS = "{ \"starterFaction\": \"DREAMS\" }";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private String bearer;

    @BeforeEach
    void logIn() throws Exception {
        String body = "{ \"login\": \"anna_test\", \"password\": \"" + TEST_PASSWORD + "\" }";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        bearer = "Bearer " + objectMapper.readTree(response).path("accessToken").asString();
    }

    @Test
    void getWelcome_factionNotChosen_returnsEmptyWelcome() throws Exception {
        mockMvc.perform(get("/api/v1/me/welcome").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.starterFaction").doesNotExist())
                .andExpect(jsonPath("$.starterCards.length()").value(0))
                .andExpect(jsonPath("$.welcomeOpenings.length()").value(0));
    }

    @Test
    void claimWelcome_dreams_returns201WithStarterSetAndTwoWelcomePacks() throws Exception {
        claim(DREAMS)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.starterFaction").value("DREAMS"))
                .andExpect(jsonPath("$.starterCards.length()").value(20))
                .andExpect(jsonPath("$.welcomeOpenings.length()").value(2))
                .andExpect(jsonPath("$.welcomeOpenings[0].packName").value("Welcome Pack"))
                .andExpect(jsonPath("$.welcomeOpenings[0].cards.length()").value(5))
                .andExpect(jsonPath("$.welcomeOpenings[1].packName").value("Welcome Pack Epico"))
                .andExpect(jsonPath("$.welcomeOpenings[1].cards[4].card.rarity.code").value("EPIC"));
    }

    @Test
    void claimWelcome_dreams_givesStarterSetOfThatFactionWithinMaxCopies() throws Exception {
        // When
        JsonNode starterCards = readJson(claim(DREAMS)).path("starterCards");

        // Then: 14 Comuni e 6 Rare Dreams, ogni carta al massimo 2 volte
        Map<String, Integer> copies = new HashMap<>();
        Map<String, Integer> byRarity = new HashMap<>();
        for (JsonNode item : starterCards) {
            JsonNode card = item.path("card");
            assertEquals("DREAMS", card.path("faction").path("code").asString());
            copies.merge(card.path("code").asString(), 1, Integer::sum);
            byRarity.merge(card.path("rarity").path("code").asString(), 1, Integer::sum);
        }
        assertEquals(14, byRarity.get("COMMON"));
        assertEquals(6, byRarity.get("RARE"));
        assertTrue(copies.values().stream().allMatch(count -> count <= 2), "Copie: " + copies);
    }

    @Test
    void claimWelcome_dreams_addsThirtyCardsToCollectionWithoutCredits() throws Exception {
        // Given
        entityManager.flush();
        int creditsBefore = credits();

        // When
        claim(DREAMS).andExpect(status().isCreated());
        entityManager.flush();

        // Then: 20 del set e 5 + 5 dei pacchetti di benvenuto, gratis
        assertEquals(30, jdbcTemplate.queryForObject("""
                SELECT coalesce(sum(uc.quantity), 0) FROM user_cards uc JOIN users u ON u.id = uc.user_id
                WHERE u.username = 'anna_test'""", Integer.class));
        assertEquals(creditsBefore, credits());
        assertEquals(3, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM pack_openings po JOIN users u ON u.id = po.user_id
                WHERE u.username = 'anna_test' AND po.source = 'SIGNUP' AND po.price_paid = 0""", Integer.class));
    }

    @Test
    void getWelcome_afterClaim_returnsSameGifts() throws Exception {
        // Given
        JsonNode claimed = readJson(claim(DREAMS));
        entityManager.flush();
        entityManager.clear();

        // When
        String response = mockMvc.perform(get("/api/v1/me/welcome").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Then
        assertEquals(claimed, objectMapper.readTree(response));
    }

    @Test
    void claimWelcome_secondTime_returns409() throws Exception {
        // Given
        claim(DREAMS).andExpect(status().isCreated());

        // When / Then
        claim("{ \"starterFaction\": \"MEMORIES\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WELCOME_ALREADY_CLAIMED"));
    }

    @Test
    void claimWelcome_unknownFaction_returns400AndChangesNothing() throws Exception {
        // When / Then
        claim("{ \"starterFaction\": \"PIRATI\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FACTION"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("starterFaction"));
        entityManager.flush();
        assertNull(jdbcTemplate.queryForObject(
                "SELECT starter_faction_id FROM users WHERE username = 'anna_test'", Object.class));
    }

    @Test
    void claimWelcome_noFaction_returns400() throws Exception {
        claim("{ \"starterFaction\": \"\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getWelcome_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/welcome")).andExpect(status().isUnauthorized());
    }

    private ResultActions claim(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/me/welcome").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode readJson(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private int credits() {
        return jdbcTemplate.queryForObject("SELECT credits FROM users WHERE username = 'anna_test'", Integer.class);
    }
}
