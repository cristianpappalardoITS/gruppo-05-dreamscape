package it.somnia.dreamscape.api.pack;

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

// Negozio dei pacchetti (GET /packs), dalle API fino al database con i pacchetti di prova di V901
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PackApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";
    private static final String TEST_PACK = "Pacchetto di prova";

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
    void listPacks_loggedIn_returnsOnlyShopPacks() throws Exception {
        // Given: in V901 c'è un solo pacchetto SHOP; set iniziali e benvenuto non si vendono

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(TEST_PACK))
                .andExpect(jsonPath("$[0].priceCredits").value(500))
                .andExpect(jsonPath("$[0].fullArtChance").value(20.0))
                .andExpect(jsonPath("$[0].factionCode").doesNotExist())
                .andExpect(jsonPath("$[0].availableUntil").doesNotExist());
    }

    @Test
    void listPacks_specialSlot_returnsChancesFromWeights() throws Exception {
        // When / Then: slot 3 con pesi 70, 25 e 5, dalla rarità più comune
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[0].slots.length()").value(3))
                .andExpect(jsonPath("$[0].slots[0].cardCount").value(3))
                .andExpect(jsonPath("$[0].slots[0].rarities[0].code").value("COMMON"))
                .andExpect(jsonPath("$[0].slots[0].rarities[0].chance").value(100.0))
                .andExpect(jsonPath("$[0].slots[2].rarities[0].code").value("RARE"))
                .andExpect(jsonPath("$[0].slots[2].rarities[0].chance").value(70.0))
                .andExpect(jsonPath("$[0].slots[2].rarities[1].code").value("EPIC"))
                .andExpect(jsonPath("$[0].slots[2].rarities[1].chance").value(25.0))
                .andExpect(jsonPath("$[0].slots[2].rarities[2].code").value("LEGENDARY"))
                .andExpect(jsonPath("$[0].slots[2].rarities[2].chance").value(5.0));
    }

    @Test
    void listPacks_withCovers_returnsOnlyActiveCoversInOrder() throws Exception {
        // Given
        insertCover("Nightmares Lord", "NIGHTMARES", 2, true);
        insertCover("Dreams Lord", "DREAMS", 1, true);
        insertCover("Copertina vecchia", null, 3, false);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[0].covers.length()").value(2))
                .andExpect(jsonPath("$[0].covers[0].name").value("Dreams Lord"))
                .andExpect(jsonPath("$[0].covers[0].factionCode").value("DREAMS"))
                .andExpect(jsonPath("$[0].covers[1].name").value("Nightmares Lord"));
    }

    @Test
    void listPacks_saleEnded_hidesPack() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE packs SET available_until = now() - interval '1 day' WHERE name = ?", TEST_PACK);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listPacks_saleNotStarted_hidesPack() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE packs SET available_from = now() + interval '1 day' WHERE name = ?", TEST_PACK);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listPacks_saleWithEndDate_returnsAvailableUntil() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE packs SET available_until = '2099-01-01T00:00:00Z' WHERE name = ?", TEST_PACK);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[0].availableUntil").value("2099-01-01T00:00:00Z"));
    }

    @Test
    void listPacks_inactivePack_hidesPack() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE packs SET is_active = FALSE WHERE name = ?", TEST_PACK);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listPacks_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/packs")).andExpect(status().isUnauthorized());
    }

    private void insertCover(String name, String factionCode, int sortOrder, boolean active) {
        jdbcTemplate.update("""
                INSERT INTO pack_covers (pack_id, faction_id, name, image_url, sort_order, is_active)
                SELECT p.id, (SELECT f.id FROM factions f WHERE f.code = ?), ?, ?, ?, ?
                FROM packs p WHERE p.name = ?""",
                factionCode, name, "/images/packs/prova.webp", sortOrder, active, TEST_PACK);
    }
}
