package it.somnia.dreamscape.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;

// Benvenuto e acquisto dei pacchetti veri (V101) dopo la registrazione, con il commit di ogni passo
class PackIntegrationTest extends IntegrationTestSupport {

    private static final String COLLECTION_SIZE = """
            SELECT coalesce(sum(uc.quantity), 0) FROM user_cards uc JOIN users u ON u.id = uc.user_id
            WHERE u.username = ?""";
    private static final String OPENINGS = """
            SELECT count(*) FROM pack_openings po JOIN users u ON u.id = po.user_id WHERE u.username = ?""";

    @Test
    void welcomeThenPurchase_newPlayer_savesCollectionBalanceAndMovements() throws Exception {
        // Given: un giocatore appena registrato, dopo il primo accesso del giorno (550 DC)
        String username = register();
        String bearer = bearer(login(username));

        // When: sceglie la fazione e poi compra un Nap Pack (300 DC, 3 carte)
        mockMvc.perform(post("/api/v1/me/welcome").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"starterFaction\": \"DREAMS\" }"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/packs/" + packId("Nap Pack") + "/open").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditsAfter").value(250))
                .andExpect(jsonPath("$.cards.length()").value(3));

        // Then: 20 carte del set, 5 + 5 dei pacchetti di benvenuto e 3 del Nap Pack
        assertEquals(33, count(COLLECTION_SIZE, username));
        assertEquals(4, count(OPENINGS, username));
        assertEquals(250, credits(username));
        assertEquals(-300, jdbcTemplate.queryForObject("""
                SELECT ct.amount FROM credit_transactions ct JOIN users u ON u.id = ct.user_id
                WHERE u.username = ? AND ct.type = 'PACK_PURCHASE'""", Integer.class, username));
    }

    @Test
    void openPack_afterCommit_openingReadableInALaterRequest() throws Exception {
        // Given: l'acquisto è già salvato e la sua transazione è chiusa
        String username = register();
        String bearer = bearer(login(username));
        JsonNode purchase = readJson(mockMvc.perform(post("/api/v1/packs/" + packId("Nap Pack") + "/open")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isCreated())
                .andReturn());

        // When / Then: la pagina dell'apertura, ricaricata, rilegge le stesse carte nello stesso ordine
        MvcResult opening = mockMvc.perform(get("/api/v1/pack-openings/" + purchase.path("openingId").asString())
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packName").value("Nap Pack"))
                .andReturn();
        for (int index = 0; index < 3; index++) {
            assertEquals(purchase.path("cards").get(index).path("card").path("id").asString(),
                    readJson(opening).path("cards").get(index).path("card").path("id").asString());
        }
    }

    @Test
    void openPack_notEnoughCredits_leavesDatabaseUnchanged() throws Exception {
        // Given: dopo un Nap Pack restano 250 DC, meno dei 500 del Dream Pack
        String username = register();
        String bearer = bearer(login(username));
        mockMvc.perform(post("/api/v1/packs/" + packId("Nap Pack") + "/open").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isCreated());

        // When
        mockMvc.perform(post("/api/v1/packs/" + packId("Dream Pack") + "/open").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_CREDITS"));

        // Then: niente apertura, niente carte e niente movimento in più
        assertEquals(250, credits(username));
        assertEquals(1, count(OPENINGS, username));
        assertEquals(3, count(COLLECTION_SIZE, username));
        assertEquals(1, count("""
                SELECT count(*) FROM credit_transactions ct JOIN users u ON u.id = ct.user_id
                WHERE u.username = ? AND ct.type = 'PACK_PURCHASE'""", username));
    }

    private UUID packId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM packs WHERE name = ?", UUID.class, name);
    }
}
