package it.somnia.dreamscape.api.admin;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

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
import tools.jackson.databind.JsonNode;

// Configurazione del gioco nel backoffice: fazioni e vantaggi, rarità, modalità. I cambi si vedono subito nelle
// pagine pubbliche (GET /factions, GET /rules) e nei mazzi
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "dreamscape.admin.username=admin_test",
        "dreamscape.admin.email=admin_test@example.test",
        "dreamscape.admin.password=password-admin-dei-test" })
@AutoConfigureMockMvc
@Transactional
class AdminConfigApiTest extends AdminApiTestSupport {

    private static final String DREAMS = """
            { "name": "Sogni", "description": "Il regno dei sogni lucidi.", "lordTitle": "Signore dei Sogni",
              "advantages": [ { "targetFaction": "MEMORIES", "attackBonus": 2, "defenseBonus": 1 } ] }""";

    private String admin;

    @BeforeEach
    void logIn() throws Exception {
        admin = bearer(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void listEffectsAndCardSets_admin_returnTheChoicesOfTheCardForm() throws Exception {
        mockMvc.perform(get("/api/v1/admin/effects").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("REST")))
                .andExpect(jsonPath("$[?(@.code == 'REST')].hasValue", hasItem(true)));
        mockMvc.perform(get("/api/v1/admin/card-sets").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("BASE")));
    }

    @Test
    void listFactions_admin_returnsTheThreeFactionsWithTheirAdvantages() throws Exception {
        mockMvc.perform(get("/api/v1/admin/factions").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].code").value("DREAMS"))
                .andExpect(jsonPath("$[0].advantages[0].targetFaction").value("MEMORIES"))
                .andExpect(jsonPath("$[0].advantages[0].attackBonus").value(1))
                .andExpect(jsonPath("$[0].advantages[0].defenseBonus").value(0));
    }

    @Test
    void updateFaction_newTextsAndBonuses_showInThePublicPages() throws Exception {
        // When
        update("/factions/DREAMS", DREAMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sogni"))
                .andExpect(jsonPath("$.lordTitle").value("Signore dei Sogni"))
                .andExpect(jsonPath("$.advantages.length()").value(1))
                .andExpect(jsonPath("$.advantages[0].attackBonus").value(2))
                .andExpect(jsonPath("$.advantages[0].defenseBonus").value(1));

        // Then: le pagine pubbliche e le regole delle partite usano i valori nuovi
        mockMvc.perform(get("/api/v1/factions"))
                .andExpect(jsonPath("$[*].name", hasItem("Sogni")));
        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(jsonPath("$.factionAdvantages[?(@.faction == 'DREAMS')].attackBonus", hasItem(2)));
    }

    @Test
    void updateFaction_addAndRemoveAdvantages_replacesThem() throws Exception {
        // Given: Dreams forte contro Nightmares invece che contro Memories
        String swapped = DREAMS.replace("MEMORIES", "NIGHTMARES");

        // When / Then
        update("/factions/DREAMS", swapped)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advantages.length()").value(1))
                .andExpect(jsonPath("$.advantages[0].targetFaction").value("NIGHTMARES"));
        update("/factions/DREAMS", DREAMS.replaceAll("\\[ \\{.*} ]", "[ ]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advantages.length()").value(0));
    }

    @Test
    void updateFaction_advantageAgainstItself_returns400() throws Exception {
        update("/factions/DREAMS", DREAMS.replace("MEMORIES", "DREAMS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("advantages[0].targetFaction"));
    }

    @Test
    void updateFaction_unknownOrRepeatedTarget_returns400() throws Exception {
        update("/factions/DREAMS", DREAMS.replace("MEMORIES", "VISIONS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("advantages[0].targetFaction"));
        String repeated = DREAMS.replace("} ] }", "}, { \"targetFaction\": \"MEMORIES\", \"attackBonus\": 1, "
                + "\"defenseBonus\": 0 } ] }");
        update("/factions/DREAMS", repeated)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("advantages[1].targetFaction"));
    }

    @Test
    void updateFaction_advantageWithoutBonus_returns400() throws Exception {
        update("/factions/DREAMS", DREAMS.replace("\"attackBonus\": 2, \"defenseBonus\": 1",
                        "\"attackBonus\": 0, \"defenseBonus\": 0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("advantages[0].attackBonus"));
    }

    @Test
    void updateFaction_nameOfAnotherFaction_returns409() throws Exception {
        update("/factions/DREAMS", DREAMS.replace("\"Sogni\"", "\"nightmares\""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FACTION_NAME_TAKEN"));
    }

    @Test
    void updateFaction_unknownCode_returns404() throws Exception {
        update("/factions/VISIONS", DREAMS)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FACTION_NOT_FOUND"));
    }

    @Test
    void updateRarity_lowerLimit_makesSavedDecksInvalid() throws Exception {
        // Given: il set iniziale di anna come mazzo valido
        String anna = bearer(PLAYER_USERNAME, PLAYER_PASSWORD);
        starterDeck(anna);

        // When: al massimo una Comune per mazzo
        update("/rarities/COMMON", """
                        { "name": "Comune", "maxPerDeck": 1, "maxCopies": 2 }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON"))
                .andExpect(jsonPath("$.maxPerDeck").value(1));

        // Then
        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(jsonPath("$.rarities[0].maxPerDeck").value(1));
        mockMvc.perform(get("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(jsonPath("$[0].isValid").value(false))
                .andExpect(jsonPath("$[0].problems").isNotEmpty());
    }

    @Test
    void updateRarity_noLimit_savesNull() throws Exception {
        update("/rarities/RARE", """
                        { "name": "Rara", "maxPerDeck": null, "maxCopies": 2 }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxPerDeck").isEmpty());
    }

    @Test
    void updateRarity_zeroCopies_returns400() throws Exception {
        update("/rarities/RARE", """
                        { "name": "Rara", "maxPerDeck": 8, "maxCopies": 0 }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateGameMode_newValues_areTheRulesOfTheNextMatches() throws Exception {
        // When
        update("/game-modes/BASE", """
                        { "name": "Base", "description": "Prova", "startingLifePoints": 25, "roundLimit": 80 }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startingLifePoints").value(25))
                .andExpect(jsonPath("$.isActive").value(true));

        // Then
        mockMvc.perform(get("/api/v1/admin/game-modes").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$[0].roundLimit").value(80));
        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(jsonPath("$.startingLifePoints").value(25))
                .andExpect(jsonPath("$.roundLimit").value(80));
    }

    @Test
    void updateGameMode_zeroLifePoints_returns400() throws Exception {
        update("/game-modes/BASE", """
                        { "name": "Base", "startingLifePoints": 0, "roundLimit": 60 }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateGameMode_unknownCode_returns404() throws Exception {
        update("/game-modes/DRAFT", """
                        { "name": "Draft", "startingLifePoints": 20, "roundLimit": 60 }""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_MODE_NOT_FOUND"));
    }

    @Test
    void updateConfig_player_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/admin/rarities/COMMON")
                        .header(HttpHeaders.AUTHORIZATION, bearer(PLAYER_USERNAME, PLAYER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Comune\", \"maxPerDeck\": null, \"maxCopies\": 2 }"))
                .andExpect(status().isForbidden());
    }

    private ResultActions update(String path, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/admin" + path).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // Il set iniziale di Dreams (20 carte, sempre un mazzo valido) diventa un mazzo
    private void starterDeck(String token) throws Exception {
        String welcome = mockMvc.perform(post("/api/v1/me/welcome").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"starterFaction\": \"DREAMS\" }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Map<String, Integer> copies = new LinkedHashMap<>();
        for (JsonNode entry : objectMapper.readTree(welcome).path("starterCards")) {
            copies.merge(entry.path("card").path("id").asString(), 1, Integer::sum);
        }
        StringBuilder cards = new StringBuilder();
        copies.forEach((cardId, quantity) -> cards.append(cards.isEmpty() ? "" : ", ")
                .append("{ \"cardId\": \"").append(cardId).append("\", \"quantity\": ").append(quantity).append(" }"));
        mockMvc.perform(post("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Set iniziale\", \"cards\": [ " + cards + " ] }"))
                .andExpect(status().isCreated());
    }
}
