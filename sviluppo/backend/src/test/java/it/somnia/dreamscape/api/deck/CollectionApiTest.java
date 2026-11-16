package it.somnia.dreamscape.api.deck;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import tools.jackson.databind.ObjectMapper;

// Collezione dell'utente collegato (GET /me/cards), con le carte di prova di V900
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CollectionApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void listCollection_noCards_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/me/cards").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void listCollection_withFullArt_returnsEachVersionAsItsOwnEntryByCode() throws Exception {
        // Given
        giveCards("anna_test", "TEST-013-FA", 1);
        giveCards("anna_test", "TEST-001", 2);
        giveCards("anna_test", "TEST-013", 1);

        // When / Then
        mockMvc.perform(get("/api/v1/me/cards").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items[0].card.code").value("TEST-001"))
                .andExpect(jsonPath("$.items[0].card.name").value("Lucciola (prova)"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].card.code").value("TEST-013"))
                .andExpect(jsonPath("$.items[2].card.code").value("TEST-013-FA"))
                .andExpect(jsonPath("$.items[2].card.variantType").value("FULL_ART"))
                .andExpect(jsonPath("$.items[2].card.name").value("Sognatrice Lucida (prova)"))
                .andExpect(jsonPath("$.items[2].card.effect.code").value("REST"))
                .andExpect(jsonPath("$.items[2].quantity").value(1));
    }

    @Test
    void listCollection_fullArtTrue_returnsOnlyFullArts() throws Exception {
        // Given
        giveCards("anna_test", "TEST-013", 1);
        giveCards("anna_test", "TEST-013-FA", 1);

        // When / Then
        mockMvc.perform(get("/api/v1/me/cards").param("fullArt", "true").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].card.code").value("TEST-013-FA"));
    }

    @Test
    void listCollection_fullArtFalse_returnsOnlyNormalCards() throws Exception {
        // Given
        giveCards("anna_test", "TEST-013", 1);
        giveCards("anna_test", "TEST-013-FA", 1);

        // When / Then
        mockMvc.perform(get("/api/v1/me/cards").param("fullArt", "false").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].card.code").value("TEST-013"));
    }

    @Test
    void listCollection_factionAndRarity_matchFullArtThroughItsBaseCard() throws Exception {
        // Given: la full art non ha fazione e rarità proprie (decisione 025)
        giveCards("anna_test", "TEST-001", 2);
        giveCards("anna_test", "TEST-013", 1);
        giveCards("anna_test", "TEST-013-FA", 1);
        giveCards("anna_test", "TEST-026", 1);

        // When / Then
        mockMvc.perform(get("/api/v1/me/cards").param("faction", "DREAMS").param("rarity", "LEGENDARY")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].card.code").value("TEST-013"))
                .andExpect(jsonPath("$.items[1].card.code").value("TEST-013-FA"));
    }

    @Test
    void listCollection_search_matchesFullArtThroughItsBaseName() throws Exception {
        // Given
        giveCards("anna_test", "TEST-001", 2);
        giveCards("anna_test", "TEST-013-FA", 1);

        // When / Then
        mockMvc.perform(get("/api/v1/me/cards").param("search", "SOGNATRICE")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].card.code").value("TEST-013-FA"));
    }

    @Test
    void listCollection_inactiveCard_isStillInTheCollection() throws Exception {
        // Given: chi la possiede la tiene (decisione 026)
        giveCards("anna_test", "TEST-001", 2);
        jdbcTemplate.update("UPDATE cards SET is_active = FALSE WHERE code = 'TEST-001'");

        // When / Then
        mockMvc.perform(get("/api/v1/me/cards").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].card.code").value("TEST-001"));
    }

    @Test
    void listCollection_otherUsersCards_areNotShown() throws Exception {
        // Given
        giveCards("bruno_test", "TEST-001", 2);

        // When / Then
        mockMvc.perform(get("/api/v1/me/cards").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void listCollection_pages_returnTotals() throws Exception {
        // Given
        giveCards("anna_test", "TEST-001", 1);
        giveCards("anna_test", "TEST-002", 1);
        giveCards("anna_test", "TEST-003", 1);

        // When / Then
        mockMvc.perform(get("/api/v1/me/cards").param("page", "1").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].card.code").value("TEST-003"));
    }

    @Test
    void listCollection_withoutLogin_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me/cards"))
                .andExpect(status().isUnauthorized());
    }

    private void giveCards(String username, String cardCode, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO user_cards (user_id, card_id, quantity)
                SELECT u.id, c.id, ? FROM users u, cards c WHERE u.username = ? AND c.code = ?""",
                quantity, username, cardCode);
    }
}
