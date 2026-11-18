package it.somnia.dreamscape.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import tools.jackson.databind.node.ObjectNode;

// Carte nel backoffice (GET, POST, PUT /admin/cards), con le carte di prova di V900. Le liste si limitano
// alle carte di prova (search "TEST-"), così non dipendono da quante carte ha il catalogo vero
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "dreamscape.admin.username=admin_test",
        "dreamscape.admin.email=admin_test@example.test",
        "dreamscape.admin.password=password-admin-dei-test" })
@AutoConfigureMockMvc
@Transactional
class AdminCardApiTest extends AdminApiTestSupport {

    // Una Leggendaria nuova senza full art: le tre Leggendarie di prova la hanno già
    private static final String NEW_LEGENDARY = """
            { "code": "TEST-100", "name": "Il Guardiano Nuovo", "description": "Veglia sui sogni.",
              "attack": 7, "defense": 7, "factionCode": "DREAMS", "rarityCode": "LEGENDARY", "cardSetCode": "TEST",
              "effectCode": "REST", "effectValue": 2 }""";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String admin;

    @BeforeEach
    void logIn() throws Exception {
        admin = bearer(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void listCards_testSet_includesFullArtsAndInactiveCards() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE cards SET is_active = FALSE WHERE code = 'TEST-001'");

        // When / Then: 39 carte normali + 3 full art
        mockMvc.perform(get("/api/v1/admin/cards").param("search", "TEST-").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(42))
                .andExpect(jsonPath("$.items[0].code").value("TEST-001"))
                .andExpect(jsonPath("$.items[0].isActive").value(false));
    }

    @Test
    void listCards_legendaryFilter_findsFullArtsThroughTheirBaseCard() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cards").param("rarity", "LEGENDARY").param("faction", "DREAMS")
                        .param("search", "TEST-").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].code").value("TEST-013"))
                .andExpect(jsonPath("$.items[1].code").value("TEST-013-FA"))
                .andExpect(jsonPath("$.items[1].variantType").value("FULL_ART"))
                .andExpect(jsonPath("$.items[1].name").doesNotExist());
    }

    @Test
    void getCard_cardInADeck_returnsHowManyDecksUseIt() throws Exception {
        // Given: un mazzo di anna_test con TEST-013-FA conta anche per TEST-013
        UUID deckId = jdbcTemplate.queryForObject("""
                INSERT INTO decks (user_id, name) SELECT id, 'Mazzo del test' FROM users WHERE username = 'anna_test'
                RETURNING id""", UUID.class);
        jdbcTemplate.update("INSERT INTO deck_cards (deck_id, card_id, quantity) SELECT ?, id, 1 FROM cards "
                + "WHERE code = 'TEST-013-FA'", deckId);

        // When / Then
        mockMvc.perform(get("/api/v1/admin/cards/{cardId}", cardId("TEST-013")).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEST-013"))
                .andExpect(jsonPath("$.effectCode").value("REST"))
                .andExpect(jsonPath("$.effectValue").value(3))
                .andExpect(jsonPath("$.deckCount").value(1));
    }

    @Test
    void createCard_normalCard_isInTheCatalog() throws Exception {
        // When
        createCard(NEW_LEGENDARY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TEST-100"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.illustrationFocusY").value(0.4))
                .andExpect(jsonPath("$.effectCode").value("REST"))
                .andExpect(jsonPath("$.deckCount").value(0));

        // Then: il catalogo pubblico la mostra con il testo dell'effetto
        mockMvc.perform(get("/api/v1/cards").param("search", "Guardiano Nuovo"))
                .andExpect(jsonPath("$.items[0].code").value("TEST-100"))
                .andExpect(jsonPath("$.items[0].effect.text")
                        .value("Quando viene giocata: il tuo giocatore guadagna 2 punti vita."));
    }

    @Test
    void createCard_fullArtOfALegendary_takesTheDataFromTheBase() throws Exception {
        // Given
        String baseId = objectMapper.readTree(createCard(NEW_LEGENDARY).andReturn().getResponse().getContentAsString())
                .path("id").asString();

        // When / Then
        createCard("""
                { "code": "TEST-100-FA", "baseCardId": "%s", "illustrationUrl": "/images/cards/test-100-fullart-v1.webp",
                  "illustrationThumbUrl": "/images/cards/test-100-fullart-v1-thumb.webp", "illustrationFocusY": 0.3 }"""
                .formatted(baseId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variantType").value("FULL_ART"))
                .andExpect(jsonPath("$.baseCardId").value(baseId))
                .andExpect(jsonPath("$.cardSetCode").value("TEST"));
        mockMvc.perform(get("/api/v1/cards/{cardId}", cardId("TEST-100-FA")))
                .andExpect(jsonPath("$.name").value("Il Guardiano Nuovo"))
                .andExpect(jsonPath("$.attack").value(7));
    }

    @Test
    void createCard_secondFullArt_returnsFullArtExists() throws Exception {
        createCard("{ \"code\": \"TEST-013-FA\", \"baseCardId\": \"%s\" }".formatted(cardId("TEST-013")))
                .andExpect(status().isConflict());
        createCard("{ \"code\": \"TEST-013-FB\", \"baseCardId\": \"%s\" }".formatted(cardId("TEST-013")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FULL_ART_EXISTS"));
    }

    @Test
    void createCard_fullArtOfARare_isRefused() throws Exception {
        createCard("{ \"code\": \"TEST-008-FA\", \"baseCardId\": \"%s\" }".formatted(cardId("TEST-008")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Oggi la full art c'è solo per le Leggendarie."));
    }

    @Test
    void createCard_codeAlreadyUsed_returnsCardCodeTaken() throws Exception {
        createCard(NEW_LEGENDARY.replace("TEST-100", "TEST-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARD_CODE_TAKEN"));
    }

    @Test
    void createCard_nameAlreadyUsed_returnsCardNameTaken() throws Exception {
        createCard(NEW_LEGENDARY.replace("Il Guardiano Nuovo", "Lucciola (prova)"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARD_NAME_TAKEN"));
    }

    @Test
    void createCard_effectWithoutItsValue_returnsValidationError() throws Exception {
        createCard(NEW_LEGENDARY.replace(", \"effectValue\": 2", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("effectValue"));
    }

    @Test
    void createCard_valueForAnEffectWithoutX_returnsValidationError() throws Exception {
        createCard(NEW_LEGENDARY.replace("\"REST\"", "\"NEUTRALITY\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Questo effetto non ha il valore X."));
    }

    @Test
    void createCard_unknownFaction_returnsValidationError() throws Exception {
        createCard(NEW_LEGENDARY.replace("\"DREAMS\"", "\"DAYDREAMS\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("factionCode"));
    }

    @Test
    void createCard_onlyOneIllustrationCopy_returnsValidationError() throws Exception {
        createCard(NEW_LEGENDARY.replace("\"effectValue\": 2", "\"effectValue\": 2, \"illustrationUrl\": \"/images/cards/x.webp\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("illustrationThumbUrl"));
    }

    @Test
    void updateCard_newStatsAndDeactivation_areSaved() throws Exception {
        // When
        mockMvc.perform(put("/api/v1/admin/cards/{cardId}", cardId("TEST-007")).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "code": "TEST-007", "name": "Gatto Sognante (prova)", "attack": 5, "defense": 4,
                                  "factionCode": "DREAMS", "rarityCode": "COMMON", "cardSetCode": "TEST",
                                  "effectCode": "LULLABY", "effectValue": 1, "isActive": false }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attack").value(5))
                .andExpect(jsonPath("$.effectCode").value("LULLABY"))
                .andExpect(jsonPath("$.isActive").value(false));

        // Then: disattivata, non è più nel catalogo pubblico
        mockMvc.perform(get("/api/v1/cards").param("search", "Gatto Sognante"))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void updateCard_withoutEffect_removesIt() throws Exception {
        mockMvc.perform(put("/api/v1/admin/cards/{cardId}", cardId("TEST-007")).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "code": "TEST-007", "name": "Gatto Sognante (prova)", "attack": 4, "defense": 5,
                                  "factionCode": "DREAMS", "rarityCode": "COMMON", "cardSetCode": "TEST" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectCode").doesNotExist());
    }

    @Test
    void updateCard_withoutIllustrationFields_keepsIllustrationAndFocus() throws Exception {
        // Given: TEST-007 con un'illustrazione e un ritaglio suoi
        ObjectNode card = cardRequest("TEST-007");
        card.put("illustrationUrl", "/images/cards/test-007-v1.webp");
        card.put("illustrationThumbUrl", "/images/cards/test-007-v1-thumb.webp");
        card.put("illustrationFocusY", 0.25);
        updateCard("TEST-007", card).andExpect(status().isOk());

        // When: un PUT con i soli dati della carta
        card.remove(List.of("illustrationUrl", "illustrationThumbUrl", "illustrationFocusY"));
        card.put("attack", 5);

        // Then
        updateCard("TEST-007", card)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attack").value(5))
                .andExpect(jsonPath("$.illustrationUrl").value("/images/cards/test-007-v1.webp"))
                .andExpect(jsonPath("$.illustrationThumbUrl").value("/images/cards/test-007-v1-thumb.webp"))
                .andExpect(jsonPath("$.illustrationFocusY").value(0.25));
    }

    @Test
    void updateCard_emptyIllustrationUrls_removesIllustration() throws Exception {
        // Given
        ObjectNode card = cardRequest("TEST-007");
        card.put("illustrationUrl", "/images/cards/test-007-v1.webp");
        card.put("illustrationThumbUrl", "/images/cards/test-007-v1-thumb.webp");
        updateCard("TEST-007", card).andExpect(status().isOk());

        // When / Then
        card.put("illustrationUrl", "");
        card.put("illustrationThumbUrl", "");
        updateCard("TEST-007", card)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.illustrationUrl").doesNotExist())
                .andExpect(jsonPath("$.illustrationThumbUrl").doesNotExist());
    }

    @Test
    void updateCard_lordToAnotherFaction_isRefused() throws Exception {
        // Given: il Lord di Dreams del set base (V102)
        String lord = jdbcTemplate.queryForObject("SELECT c.code FROM factions f JOIN cards c ON c.id = f.lord_card_id "
                + "WHERE f.code = 'DREAMS'", String.class);
        ObjectNode card = cardRequest(lord);
        card.put("factionCode", "NIGHTMARES");

        // When / Then
        updateCard(lord, card)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("factionCode"));
    }

    @Test
    void updateCard_lordNoLongerLegendary_isRefused() throws Exception {
        String lord = jdbcTemplate.queryForObject("SELECT c.code FROM factions f JOIN cards c ON c.id = f.lord_card_id "
                + "WHERE f.code = 'DREAMS'", String.class);
        ObjectNode card = cardRequest(lord);
        card.put("rarityCode", "EPIC");

        updateCard(lord, card)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("rarityCode"));
    }

    @Test
    void updateCard_legendaryWithFullArtNoLongerLegendary_isRefused() throws Exception {
        // Given: TEST-026 ha la sua full art (V900)
        ObjectNode card = cardRequest("TEST-026");
        card.put("rarityCode", "EPIC");

        // When / Then
        updateCard("TEST-026", card)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Questa carta ha la full art: resta Leggendaria."));
    }

    @Test
    void updateCard_legendaryWithFullArtNewStats_isSaved() throws Exception {
        ObjectNode card = cardRequest("TEST-026");
        card.put("attack", 10);

        updateCard("TEST-026", card)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attack").value(10));
    }

    @Test
    void listCards_wholeTestSet_usesAFewQueriesNotSomeForEachCard() throws Exception {
        // Given
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        // When: 42 carte, fra normali e full art
        try {
            mockMvc.perform(get("/api/v1/admin/cards").param("search", "TEST-").param("size", "100")
                            .header(HttpHeaders.AUTHORIZATION, admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(42));

            // Then: prima erano più di 120 (tre per carta)
            assertThat(statistics.getPrepareStatementCount()).isLessThan(10);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @Test
    void updateCard_differentCode_isRefused() throws Exception {
        mockMvc.perform(put("/api/v1/admin/cards/{cardId}", cardId("TEST-007")).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_LEGENDARY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Il codice di una carta non si cambia."));
    }

    @Test
    void updateCard_fullArtWithOwnStats_isRefused() throws Exception {
        mockMvc.perform(put("/api/v1/admin/cards/{cardId}", cardId("TEST-013-FA")).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "code": "TEST-013-FA", "baseCardId": "%s", "attack": 9 }""".formatted(cardId("TEST-013"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("baseCardId"));
    }

    @Test
    void adminCards_player_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cards").header(HttpHeaders.AUTHORIZATION, bearer(PLAYER_USERNAME, PLAYER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCards_withoutLogin_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cards"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions createCard(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/cards").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions updateCard(String code, ObjectNode body) throws Exception {
        return mockMvc.perform(put("/api/v1/admin/cards/{cardId}", cardId(code))
                .header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    // La carta come la rimanda il backoffice in un PUT: i dati di GET /admin/cards/{cardId}, senza i campi in sola
    // lettura
    private ObjectNode cardRequest(String code) throws Exception {
        String body = mockMvc.perform(get("/api/v1/admin/cards/{cardId}", cardId(code))
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ObjectNode card = (ObjectNode) objectMapper.readTree(body);
        card.remove(List.of("id", "variantType", "deckCount"));
        return card;
    }

    private UUID cardId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM cards WHERE code = ?", UUID.class, code);
    }
}
