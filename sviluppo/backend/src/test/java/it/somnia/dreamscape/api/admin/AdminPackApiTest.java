package it.somnia.dreamscape.api.admin;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;

// Pacchetti nel backoffice (GET, POST, PUT /admin/packs), con i pacchetti veri di V101
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "dreamscape.admin.username=admin_test",
        "dreamscape.admin.email=admin_test@example.test",
        "dreamscape.admin.password=password-admin-dei-test" })
@AutoConfigureMockMvc
@Transactional
class AdminPackApiTest extends AdminApiTestSupport {

    // Un pacchetto del negozio: 4 Comuni e una carta speciale 80/20
    private static final String NEW_PACK = """
            { "name": "Sogno Breve", "description": "Un pacchetto di prova.", "packType": "SHOP", "priceCredits": 250,
              "fullArtChance": 10, "slots": [
                { "cardCount": 4, "rarities": [ { "rarityCode": "COMMON", "weight": 100 } ] },
                { "cardCount": 1, "rarities": [ { "rarityCode": "RARE", "weight": 80 }, { "rarityCode": "EPIC", "weight": 20 } ] }
              ],
              "covers": [ { "name": "Copertina Dreams", "factionCode": "DREAMS", "imageUrl": "/images/packs/sogno-breve-v1.webp" } ] }""";

    private String admin;

    @BeforeEach
    void logIn() throws Exception {
        admin = bearer(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void listPacks_includesStarterWelcomeAndDisabledPacks() throws Exception {
        mockMvc.perform(get("/api/v1/admin/packs").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Dream Pack")))
                .andExpect(jsonPath("$[*].name", hasItem("Welcome Pack Epico")))
                .andExpect(jsonPath("$[*].packType", hasItem("STARTER")))
                .andExpect(jsonPath("$[*].isActive", hasItem(false)));
    }

    @Test
    void createPack_shopPack_isOnSaleInTheShop() throws Exception {
        // When
        createPack(NEW_PACK)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sogno Breve"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.slots.length()").value(2))
                .andExpect(jsonPath("$.slots[1].rarities[0].rarityCode").value("RARE"))
                .andExpect(jsonPath("$.slots[1].rarities[1].weight").value(20))
                .andExpect(jsonPath("$.covers[0].factionCode").value("DREAMS"));

        // Then: il negozio lo mostra, con le probabilità calcolate dai pesi
        mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION, bearer(PLAYER_USERNAME, PLAYER_PASSWORD)))
                .andExpect(jsonPath("$[0].name").value("Sogno Breve"))
                .andExpect(jsonPath("$[0].priceCredits").value(250))
                .andExpect(jsonPath("$[0].slots[1].rarities[1].chance").value(20.0))
                .andExpect(jsonPath("$[0].covers[0].name").value("Copertina Dreams"));
    }

    @Test
    void updatePack_newSlotsAndRemovedCover_replacesSlotsAndTurnsCoverOff() throws Exception {
        // Given
        String packId = idOf(createPack(NEW_PACK));

        // When: uno slot solo e nessuna copertina
        mockMvc.perform(put("/api/v1/admin/packs/{packId}", packId).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "name": "Sogno Breve", "packType": "SHOP", "priceCredits": 300, "fullArtChance": 10,
                                  "slots": [ { "cardCount": 5, "rarities": [ { "rarityCode": "COMMON", "weight": 100 } ] } ] }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCredits").value(300))
                .andExpect(jsonPath("$.slots.length()").value(1))
                .andExpect(jsonPath("$.slots[0].cardCount").value(5))
                .andExpect(jsonPath("$.covers[0].isActive").value(false));
    }

    @Test
    void createPack_weightsNotAddingUpTo100_returnsValidationError() throws Exception {
        createPack(NEW_PACK.replace("\"weight\": 20", "\"weight\": 15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le probabilità dello slot 2 sommano a 95: devono fare 100."));
    }

    @Test
    void createPack_shopPackWithoutPrice_returnsValidationError() throws Exception {
        createPack(NEW_PACK.replace("\"priceCredits\": 250", "\"priceCredits\": 0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("priceCredits"));
    }

    @Test
    void createPack_paidStarterPack_returnsValidationError() throws Exception {
        createPack(NEW_PACK.replace("\"SHOP\"", "\"STARTER\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("priceCredits"));
    }

    @Test
    void createPack_saleEndingBeforeItStarts_returnsValidationError() throws Exception {
        createPack(NEW_PACK.replace("\"fullArtChance\": 10", """
                "fullArtChance": 10, "availableFrom": "2026-12-01T00:00:00Z", "availableUntil": "2026-11-01T00:00:00Z\""""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("availableUntil"));
    }

    @Test
    void createPack_nameAlreadyUsed_returnsPackNameTaken() throws Exception {
        createPack(NEW_PACK.replace("Sogno Breve", "Dream Pack"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PACK_NAME_TAKEN"));
    }

    @Test
    void createPack_withoutSlots_returnsValidationError() throws Exception {
        createPack("""
                { "name": "Vuoto", "packType": "SHOP", "priceCredits": 100, "fullArtChance": 0, "slots": [] }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("slots"));
    }

    @Test
    void adminPacks_player_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/packs").header(HttpHeaders.AUTHORIZATION, bearer(PLAYER_USERNAME, PLAYER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    private ResultActions createPack(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/packs").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String idOf(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("id").asString();
    }
}
