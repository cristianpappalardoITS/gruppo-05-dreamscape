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

// Negozio dei pacchetti (GET /packs), dalle API fino al database con i pacchetti veri di V101
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PackApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";
    // I pacchetti di V101 dal più economico: Nap 300, Dream 500, i tre di fazione 600 (per nome), REM 1.000
    private static final String TEST_PACK = "Dream Pack";

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
    void listPacks_loggedIn_returnsTheSixShopPacksFromTheCheapest() throws Exception {
        // Given: i sei pacchetti SHOP della decisione 030; set iniziali e benvenuto non si vendono,
        // i pacchetti di prova di V901 sono spenti (V903)

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("Nap Pack"))
                .andExpect(jsonPath("$[0].priceCredits").value(300))
                .andExpect(jsonPath("$[1].name").value(TEST_PACK))
                .andExpect(jsonPath("$[1].priceCredits").value(500))
                .andExpect(jsonPath("$[1].fullArtChance").value(20.0))
                .andExpect(jsonPath("$[1].factionCode").doesNotExist())
                .andExpect(jsonPath("$[1].availableUntil").doesNotExist())
                .andExpect(jsonPath("$[1].imageUrl").doesNotExist())
                .andExpect(jsonPath("$[2].name").value("Faceless Pack"))
                .andExpect(jsonPath("$[3].name").value("Lucid Pack"))
                .andExpect(jsonPath("$[4].name").value("Still Time Pack"))
                .andExpect(jsonPath("$[5].name").value("REM Pack"))
                .andExpect(jsonPath("$[5].priceCredits").value(1000));
    }

    @Test
    void listPacks_packWithImage_returnsImageUrl() throws Exception {
        // Given: l'immagine del Dream Pack, il retro delle carte (decisione 030)
        jdbcTemplate.update("UPDATE packs SET image_url = '/images/packs/dream-pack-v1.webp' WHERE name = ?",
                TEST_PACK);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[1].name").value(TEST_PACK))
                .andExpect(jsonPath("$[1].imageUrl").value("/images/packs/dream-pack-v1.webp"))
                .andExpect(jsonPath("$[0].imageUrl").doesNotExist());
    }

    @Test
    void listPacks_factionPack_returnsFactionAndHalfFullArtChance() throws Exception {
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[3].name").value("Lucid Pack"))
                .andExpect(jsonPath("$[3].priceCredits").value(600))
                .andExpect(jsonPath("$[3].factionCode").value("DREAMS"))
                .andExpect(jsonPath("$[3].fullArtChance").value(10.0))
                .andExpect(jsonPath("$[2].factionCode").value("NIGHTMARES"))
                .andExpect(jsonPath("$[4].factionCode").value("MEMORIES"));
    }

    @Test
    void listPacks_specialSlot_returnsChancesFromWeights() throws Exception {
        // When / Then: slot 3 del Dream Pack con pesi 70, 25 e 5, dalla rarità più comune
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[1].slots.length()").value(3))
                .andExpect(jsonPath("$[1].slots[0].cardCount").value(3))
                .andExpect(jsonPath("$[1].slots[0].rarities[0].code").value("COMMON"))
                .andExpect(jsonPath("$[1].slots[0].rarities[0].chance").value(100.0))
                .andExpect(jsonPath("$[1].slots[2].rarities[0].code").value("RARE"))
                .andExpect(jsonPath("$[1].slots[2].rarities[0].chance").value(70.0))
                .andExpect(jsonPath("$[1].slots[2].rarities[1].code").value("EPIC"))
                .andExpect(jsonPath("$[1].slots[2].rarities[1].chance").value(25.0))
                .andExpect(jsonPath("$[1].slots[2].rarities[2].code").value("LEGENDARY"))
                .andExpect(jsonPath("$[1].slots[2].rarities[2].chance").value(5.0));
    }

    @Test
    void listPacks_napAndRemPacks_returnTheirSpecialSlots() throws Exception {
        // When / Then: Nap Pack 2 Comuni + 1 carta 85/12/3; REM Pack 6 Comuni, 2 Rare, 2 carte 65/29/6
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[0].slots.length()").value(2))
                .andExpect(jsonPath("$[0].slots[0].cardCount").value(2))
                .andExpect(jsonPath("$[0].slots[1].rarities[0].chance").value(85.0))
                .andExpect(jsonPath("$[0].slots[1].rarities[1].chance").value(12.0))
                .andExpect(jsonPath("$[0].slots[1].rarities[2].chance").value(3.0))
                .andExpect(jsonPath("$[5].slots[0].cardCount").value(6))
                .andExpect(jsonPath("$[5].slots[1].cardCount").value(2))
                .andExpect(jsonPath("$[5].slots[2].cardCount").value(2))
                .andExpect(jsonPath("$[5].slots[2].rarities[0].chance").value(65.0))
                .andExpect(jsonPath("$[5].slots[2].rarities[1].chance").value(29.0))
                .andExpect(jsonPath("$[5].slots[2].rarities[2].chance").value(6.0));
    }

    @Test
    void listPacks_withCovers_returnsOnlyActiveCoversInOrder() throws Exception {
        // Given
        insertCover("Nightmares Lord", "NIGHTMARES", 2, true);
        insertCover("Dreams Lord", "DREAMS", 1, true);
        insertCover("Copertina vecchia", null, 3, false);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[1].covers.length()").value(2))
                .andExpect(jsonPath("$[1].covers[0].name").value("Dreams Lord"))
                .andExpect(jsonPath("$[1].covers[0].factionCode").value("DREAMS"))
                .andExpect(jsonPath("$[1].covers[1].name").value("Nightmares Lord"))
                .andExpect(jsonPath("$[0].covers.length()").value(0));
    }

    @Test
    void listPacks_saleEnded_hidesPack() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE packs SET available_until = now() - interval '1 day' WHERE name = ?", TEST_PACK);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[1].name").value("Faceless Pack"));
    }

    @Test
    void listPacks_saleNotStarted_hidesPack() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE packs SET available_from = now() + interval '1 day' WHERE name = ?", TEST_PACK);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[1].name").value("Faceless Pack"));
    }

    @Test
    void listPacks_saleWithEndDate_returnsAvailableUntil() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE packs SET available_until = '2099-01-01T00:00:00Z' WHERE name = ?", TEST_PACK);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[1].availableUntil").value("2099-01-01T00:00:00Z"));
    }

    @Test
    void listPacks_inactivePack_hidesPack() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE packs SET is_active = FALSE WHERE name = ?", TEST_PACK);

        // When / Then
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[1].name").value("Faceless Pack"));
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
