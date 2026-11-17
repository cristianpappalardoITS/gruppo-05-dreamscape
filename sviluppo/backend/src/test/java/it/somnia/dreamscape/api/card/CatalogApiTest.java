package it.somnia.dreamscape.api.card;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;

// Catalogo pubblico (GET /cards, /cards/{cardId}, /factions, /rules), senza accesso, con le carte di V900
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogApiTest {

    // Le carte normali di V900 (13 per fazione) e quelle del set base di V102 (33 per fazione).
    // Le full art non sono nella lista. In ordine di codice le BASE- vengono prima delle TEST-.
    private static final int TEST_BASE_CARDS = 39;
    private static final int BASE_SET_CARDS = 99;
    private static final int CATALOG_CARDS = TEST_BASE_CARDS + BASE_SET_CARDS;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listCards_noFilters_returnsFirstPageOfBaseCardsByCode() throws Exception {
        mockMvc.perform(get("/api/v1/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(24))
                .andExpect(jsonPath("$.totalItems").value(CATALOG_CARDS))
                .andExpect(jsonPath("$.totalPages").value(6))
                .andExpect(jsonPath("$.items.length()").value(24))
                .andExpect(jsonPath("$.items[0].code").value("BASE-001"))
                .andExpect(jsonPath("$.items[1].code").value("BASE-002"));
    }

    @Test
    void listCards_cardWithEffect_returnsTheCardObjectOfTheGuide() throws Exception {
        // When / Then: TEST-007 è una Comune Dreams con Riposo 1
        mockMvc.perform(get("/api/v1/cards").param("search", "Gatto Sognante"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].code").value("TEST-007"))
                .andExpect(jsonPath("$.items[0].baseCardId").doesNotExist())
                .andExpect(jsonPath("$.items[0].variantType").doesNotExist())
                .andExpect(jsonPath("$.items[0].name").value("Gatto Sognante (prova)"))
                .andExpect(jsonPath("$.items[0].faction.code").value("DREAMS"))
                .andExpect(jsonPath("$.items[0].faction.name").value("Dreams"))
                .andExpect(jsonPath("$.items[0].rarity.code").value("COMMON"))
                .andExpect(jsonPath("$.items[0].rarity.name").value("Comune"))
                .andExpect(jsonPath("$.items[0].attack").value(4))
                .andExpect(jsonPath("$.items[0].defense").value(5))
                .andExpect(jsonPath("$.items[0].effect.code").value("REST"))
                .andExpect(jsonPath("$.items[0].effect.value").value(1))
                .andExpect(jsonPath("$.items[0].effect.text")
                        .value("Quando viene giocata: il tuo giocatore guadagna 1 punti vita."))
                .andExpect(jsonPath("$.items[0].illustrationFocusY").value(0.4))
                .andExpect(jsonPath("$.items[0].lordTitle").doesNotExist());
    }

    @Test
    void listCards_cardWithoutEffect_returnsNullEffect() throws Exception {
        mockMvc.perform(get("/api/v1/cards").param("search", "Lucciola (prova)"))
                .andExpect(jsonPath("$.items[0].code").value("TEST-001"))
                .andExpect(jsonPath("$.items[0].effect").doesNotExist());
    }

    @Test
    void listCards_factionAndRarity_returnsOnlyMatchingCards() throws Exception {
        mockMvc.perform(get("/api/v1/cards").param("faction", "NIGHTMARES").param("rarity", "RARE"))
                .andExpect(status().isOk())
                // 8 Rare Nightmares del set base, poi le 3 di prova
                .andExpect(jsonPath("$.totalItems").value(11))
                .andExpect(jsonPath("$.items[8].code").value("TEST-021"))
                .andExpect(jsonPath("$.items[9].code").value("TEST-022"))
                .andExpect(jsonPath("$.items[10].code").value("TEST-023"));
    }

    @Test
    void listCards_unknownFaction_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/cards").param("faction", "DAYDREAMS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void listCards_searchInOtherCase_findsCardByPartOfName() throws Exception {
        mockMvc.perform(get("/api/v1/cards").param("search", "SPECCHIO"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].code").value("TEST-020"));
    }

    @Test
    void listCards_searchWithPercent_matchesItLiterally() throws Exception {
        // Given: % non è un carattere jolly, nessuna carta ha % nel nome

        // When / Then
        mockMvc.perform(get("/api/v1/cards").param("search", "%"))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void listCards_featured_returnsTheLegendaryCardsWithLordTitleOnlyForLords() throws Exception {
        // Given: i Lord di V102 sono BASE-004, BASE-008 e BASE-012; la Leggendaria Dreams di prova
        // prende il posto di BASE-004 come Lord di Dreams
        jdbcTemplate.update("""
                UPDATE factions SET lord_card_id = (SELECT id FROM cards WHERE code = 'TEST-013')
                WHERE code = 'DREAMS'""");

        // When / Then: 12 Leggendarie del set base, poi le 3 di prova
        mockMvc.perform(get("/api/v1/cards").param("featured", "true"))
                .andExpect(jsonPath("$.totalItems").value(15))
                .andExpect(jsonPath("$.items[0].code").value("BASE-004"))
                .andExpect(jsonPath("$.items[0].lordTitle").doesNotExist())
                .andExpect(jsonPath("$.items[1].code").value("BASE-008"))
                .andExpect(jsonPath("$.items[1].lordTitle").value("Nightmares Lord"))
                .andExpect(jsonPath("$.items[12].code").value("TEST-013"))
                .andExpect(jsonPath("$.items[12].rarity.code").value("LEGENDARY"))
                .andExpect(jsonPath("$.items[12].lordTitle").value("Dreams Lord"))
                .andExpect(jsonPath("$.items[13].code").value("TEST-026"))
                .andExpect(jsonPath("$.items[13].lordTitle").doesNotExist())
                .andExpect(jsonPath("$.items[14].code").value("TEST-039"));
    }

    @Test
    void listCards_inactiveCard_isNotInTheCatalog() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE cards SET is_active = FALSE WHERE code = 'TEST-001'");

        // When / Then
        mockMvc.perform(get("/api/v1/cards"))
                .andExpect(jsonPath("$.totalItems").value(CATALOG_CARDS - 1));
        mockMvc.perform(get("/api/v1/cards").param("search", "Lucciola (prova)"))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void listCards_secondPage_returnsTheRemainingCards() throws Exception {
        // Ultima pagina: dalla 121esima carta, cioè la 22esima di prova
        mockMvc.perform(get("/api/v1/cards").param("page", "5").param("size", "24"))
                .andExpect(jsonPath("$.page").value(5))
                .andExpect(jsonPath("$.items.length()").value(CATALOG_CARDS - 120))
                .andExpect(jsonPath("$.items[0].code").value("TEST-022"));
    }

    @Test
    void listCards_sizeTooBig_returnsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/cards").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listCards_pageNotANumber_returnsMalformedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/cards").param("page", "prima"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getCard_fullArt_returnsItsIdAndCodeWithTheBaseCardData() throws Exception {
        // Given
        UUID baseId = cardId("TEST-013");
        UUID fullArtId = cardId("TEST-013-FA");

        // When / Then
        mockMvc.perform(get("/api/v1/cards/{cardId}", fullArtId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fullArtId.toString()))
                .andExpect(jsonPath("$.code").value("TEST-013-FA"))
                .andExpect(jsonPath("$.baseCardId").value(baseId.toString()))
                .andExpect(jsonPath("$.variantType").value("FULL_ART"))
                .andExpect(jsonPath("$.name").value("Sognatrice Lucida (prova)"))
                .andExpect(jsonPath("$.rarity.code").value("LEGENDARY"))
                .andExpect(jsonPath("$.attack").value(6))
                .andExpect(jsonPath("$.defense").value(8))
                .andExpect(jsonPath("$.effect.code").value("REST"))
                .andExpect(jsonPath("$.effect.value").value(3));
    }

    @Test
    void getCard_inactiveCard_isStillReadable() throws Exception {
        // Given: chi la possiede la tiene nella collezione e ne vede il dettaglio (decisione 026)
        jdbcTemplate.update("UPDATE cards SET is_active = FALSE WHERE code = 'TEST-001'");

        // When / Then
        mockMvc.perform(get("/api/v1/cards/{cardId}", cardId("TEST-001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEST-001"));
    }

    @Test
    void getCard_unknownId_returnsCardNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/cards/{cardId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Carta non trovata."));
    }

    @Test
    void getCard_idNotUuid_returnsMalformedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/cards/{cardId}", "42"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void listFactions_returnsTheThreeFactionsWithStrengthsAndWeaknesses() throws Exception {
        // When / Then: ciclo Dreams → Memories → Nightmares → Dreams (V100), in ordine di codice
        mockMvc.perform(get("/api/v1/factions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].code").value("DREAMS"))
                .andExpect(jsonPath("$[0].name").value("Dreams"))
                .andExpect(jsonPath("$[0].lordTitle").value("Dreams Lord"))
                .andExpect(jsonPath("$[0].lordCardId").value(cardId("BASE-004").toString()))
                .andExpect(jsonPath("$[0].strongAgainst[0]").value("MEMORIES"))
                .andExpect(jsonPath("$[0].weakAgainst[0]").value("NIGHTMARES"))
                .andExpect(jsonPath("$[1].code").value("MEMORIES"))
                .andExpect(jsonPath("$[1].strongAgainst[0]").value("NIGHTMARES"))
                .andExpect(jsonPath("$[1].weakAgainst[0]").value("DREAMS"))
                .andExpect(jsonPath("$[2].code").value("NIGHTMARES"));
    }

    @Test
    void listFactions_lordLinked_returnsLordCardId() throws Exception {
        // Given
        UUID lordId = cardId("TEST-026");
        jdbcTemplate.update("UPDATE factions SET lord_card_id = ? WHERE code = 'NIGHTMARES'", lordId);

        // When / Then
        mockMvc.perform(get("/api/v1/factions"))
                .andExpect(jsonPath("$[2].code").value("NIGHTMARES"))
                .andExpect(jsonPath("$[2].lordCardId").value(lordId.toString()));
    }

    @Test
    void getRules_returnsDeckSizeBaseModeAndRarityLimits() throws Exception {
        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckSize").value(20))
                .andExpect(jsonPath("$.startingLifePoints").value(20))
                .andExpect(jsonPath("$.roundLimit").value(60))
                .andExpect(jsonPath("$.rarities.length()").value(4))
                .andExpect(jsonPath("$.rarities[0].code").value("COMMON"))
                .andExpect(jsonPath("$.rarities[0].maxPerDeck").doesNotExist())
                .andExpect(jsonPath("$.rarities[0].maxCopies").value(2))
                .andExpect(jsonPath("$.rarities[1].maxPerDeck").value(8))
                .andExpect(jsonPath("$.rarities[2].maxPerDeck").value(4))
                .andExpect(jsonPath("$.rarities[3].code").value("LEGENDARY"))
                .andExpect(jsonPath("$.rarities[3].maxPerDeck").value(2))
                .andExpect(jsonPath("$.rarities[3].maxCopies").value(1))
                .andExpect(jsonPath("$.factionAdvantages.length()").value(3))
                .andExpect(jsonPath("$.factionAdvantages[0].attackBonus").value(1));
    }

    @Test
    void getRules_baseModeChanged_returnsTheNewNumbers() throws Exception {
        // Given: l'admin cambia punti vita e round dal backoffice
        jdbcTemplate.update("UPDATE game_modes SET starting_life_points = 25, round_limit = 50 WHERE code = 'BASE'");

        // When / Then
        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(jsonPath("$.startingLifePoints").value(25))
                .andExpect(jsonPath("$.roundLimit").value(50));
    }

    private UUID cardId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM cards WHERE code = ?", UUID.class, code);
    }
}
