package it.somnia.dreamscape.api.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Acquisto di un pacchetto (POST /packs/{packId}/open) con il Dream Pack di V101 (500 DC, 5 carte).
// anna_test dopo l'accesso ha 550 DC: 500 della registrazione e 50 della ricompensa giornaliera.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PackPurchaseApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";
    private static final String TEST_PACK = "Dream Pack";

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
    void openPack_enoughCredits_returns201WithCardsAndBalance() throws Exception {
        // When / Then
        mockMvc.perform(post(openUrl(packId(TEST_PACK))).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.openingId").exists())
                .andExpect(jsonPath("$.creditsAfter").value(50))
                .andExpect(jsonPath("$.cards.length()").value(5))
                .andExpect(jsonPath("$.cards[0].card.rarity.code").value("COMMON"))
                .andExpect(jsonPath("$.cards[3].card.rarity.code").value("RARE"));
    }

    @Test
    void openPack_enoughCredits_savesOpeningReadableAfterwards() throws Exception {
        // Given
        JsonNode purchase = buy(TEST_PACK, null);
        String openingId = purchase.path("openingId").asString();

        // When / Then: la pagina dell'apertura rilegge le stesse carte nello stesso ordine
        mockMvc.perform(get("/api/v1/pack-openings/" + openingId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packName").value(TEST_PACK))
                .andExpect(jsonPath("$.cards[0].card.id").value(purchase.path("cards").get(0).path("card")
                        .path("id").asString()))
                .andExpect(jsonPath("$.cards[4].card.id").value(purchase.path("cards").get(4).path("card")
                        .path("id").asString()));
    }

    @Test
    void openPack_enoughCredits_writesCreditTransactionLinkedToOpening() throws Exception {
        // Given
        String openingId = buy(TEST_PACK, null).path("openingId").asString();
        entityManager.flush();

        // When
        Integer amount = jdbcTemplate.queryForObject("""
                SELECT ct.amount FROM credit_transactions ct
                WHERE ct.type = 'PACK_PURCHASE' AND ct.pack_opening_id = ?::uuid""", Integer.class, openingId);

        // Then
        assertEquals(-500, amount);
        assertEquals(50, credits("anna_test"));
    }

    @Test
    void openPack_cardsAlreadyOwned_addsCopiesToExistingRows() throws Exception {
        // Given: anna ha già una copia di ogni Comune, quindi le 3 Comuni del pacchetto aumentano le copie
        jdbcTemplate.update("""
                INSERT INTO user_cards (user_id, card_id, quantity)
                SELECT u.id, c.id, 1 FROM users u, cards c JOIN rarities r ON r.id = c.rarity_id
                WHERE u.username = 'anna_test' AND r.code = 'COMMON' AND c.base_card_id IS NULL""");
        int rowsBefore = userCardRows();
        int copiesBefore = userCardCopies();

        // When
        buy(TEST_PACK, null);
        entityManager.flush();

        // Then: 5 copie in più, ma al massimo 2 righe nuove (la Rara e la carta speciale)
        assertEquals(copiesBefore + 5, userCardCopies());
        int newRows = userCardRows() - rowsBefore;
        assertTrue(newRows <= 2, "Righe nuove: " + newRows);
    }

    @Test
    void openPack_notEnoughCredits_returns409AndChangesNothing() throws Exception {
        // Given: prima si scrivono le modifiche dell'accesso, altrimenti Hibernate le scriverebbe dopo sopra i 100
        entityManager.flush();
        jdbcTemplate.update("UPDATE users SET credits = 100 WHERE username = 'anna_test'");

        // When / Then
        mockMvc.perform(post(openUrl(packId(TEST_PACK))).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_CREDITS"));
        entityManager.flush();
        assertEquals(100, credits("anna_test"));
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM pack_openings po JOIN users u ON u.id = po.user_id
                WHERE u.username = 'anna_test'""", Integer.class));
    }

    @Test
    void openPack_unknownPack_returns404() throws Exception {
        mockMvc.perform(post(openUrl(UUID.randomUUID())).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PACK_NOT_FOUND"));
    }

    @Test
    void openPack_welcomePack_returns404() throws Exception {
        // Given: i pacchetti di benvenuto si regalano, non si comprano
        UUID welcomePack = packId("Welcome Pack");

        // When / Then
        mockMvc.perform(post(openUrl(welcomePack)).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PACK_NOT_FOUND"));
    }

    @Test
    void openPack_saleEnded_returns409() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE packs SET available_until = now() - interval '1 day' WHERE name = ?", TEST_PACK);

        // When / Then
        mockMvc.perform(post(openUrl(packId(TEST_PACK))).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PACK_NOT_ON_SALE"));
    }

    @Test
    void openPack_validCover_savesChosenCover() throws Exception {
        // Given
        UUID coverId = insertCover(TEST_PACK, "Dreams Lord", true);

        // When
        String openingId = buy(TEST_PACK, coverId).path("openingId").asString();
        entityManager.flush();

        // Then
        assertEquals(coverId, jdbcTemplate.queryForObject(
                "SELECT pack_cover_id FROM pack_openings WHERE id = ?::uuid", UUID.class, openingId));
    }

    @Test
    void openPack_coverOfAnotherPack_returns400() throws Exception {
        // Given
        UUID otherCover = insertCover("Welcome Pack", "Copertina del benvenuto", true);

        // When / Then
        mockMvc.perform(post(openUrl(packId(TEST_PACK))).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"coverId\": \"" + otherCover + "\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COVER"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("coverId"));
    }

    @Test
    void openPack_inactiveCover_returns400() throws Exception {
        // Given
        UUID oldCover = insertCover(TEST_PACK, "Copertina vecchia", false);

        // When / Then
        mockMvc.perform(post(openUrl(packId(TEST_PACK))).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"coverId\": \"" + oldCover + "\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COVER"));
    }

    @Test
    void openPack_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(post(openUrl(packId(TEST_PACK)))).andExpect(status().isUnauthorized());
    }

    private JsonNode buy(String packName, UUID coverId) throws Exception {
        String body = coverId != null ? "{ \"coverId\": \"" + coverId + "\" }" : "{}";
        String response = mockMvc.perform(post(openUrl(packId(packName))).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private static String openUrl(UUID packId) {
        return "/api/v1/packs/" + packId + "/open";
    }

    private UUID packId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM packs WHERE name = ?", UUID.class, name);
    }

    private UUID insertCover(String packName, String coverName, boolean active) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO pack_covers (pack_id, name, image_url, is_active)
                SELECT p.id, ?, '/images/packs/prova.webp', ? FROM packs p WHERE p.name = ?
                RETURNING id""", UUID.class, coverName, active, packName);
    }

    private int credits(String username) {
        return jdbcTemplate.queryForObject("SELECT credits FROM users WHERE username = ?", Integer.class, username);
    }

    private int userCardRows() {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM user_cards uc JOIN users u ON u.id = uc.user_id
                WHERE u.username = 'anna_test'""", Integer.class);
    }

    private int userCardCopies() {
        return jdbcTemplate.queryForObject("""
                SELECT coalesce(sum(uc.quantity), 0) FROM user_cards uc JOIN users u ON u.id = uc.user_id
                WHERE u.username = 'anna_test'""", Integer.class);
    }
}
