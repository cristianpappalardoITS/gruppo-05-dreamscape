package it.somnia.dreamscape.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;

// Mazzi dalle API (#80) con il commit di ogni richiesta: il set iniziale del Benvenuto diventa un mazzo,
// le modifiche sbagliate non toccano quello salvato, due salvataggi insieme non creano doppioni (#88).
// Le singole regole del mazzo (20 carte, copie, rarità, possesso) sono in DeckApiTest e DeckValidatorTest.
class DeckIntegrationTest extends IntegrationTestSupport {

    private static final String DECK_CARDS = """
            SELECT coalesce(sum(dc.quantity), 0) FROM deck_cards dc
            JOIN decks d ON d.id = dc.deck_id JOIN users u ON u.id = d.user_id WHERE u.username = ?""";
    private static final String DECKS = """
            SELECT count(*) FROM decks d JOIN users u ON u.id = d.user_id WHERE u.username = ?""";

    @Test
    void createDeck_starterSet_savesValidDeckReadableInALaterRequest() throws Exception {
        // Given: il set iniziale forma sempre un mazzo valido (decisione 014)
        String username = register();
        String bearer = bearer(login(username));
        String body = starterDeckBody(bearer, "Primo mazzo");

        // When
        mockMvc.perform(post("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCards").value(20))
                .andExpect(jsonPath("$.isValid").value(true));

        // Then: il mazzo è salvato e la lista, in un'altra richiesta, lo rilegge valido
        mockMvc.perform(get("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Primo mazzo"))
                .andExpect(jsonPath("$[0].isValid").value(true))
                .andExpect(jsonPath("$[0].problems.length()").value(0));
        assertEquals(20, count(DECK_CARDS, username));
    }

    @Test
    void updateDeck_invalidCards_keepsTheSavedDeckInTheDatabase() throws Exception {
        // Given: un mazzo valido già salvato
        String username = register();
        String bearer = bearer(login(username));
        String deckId = readJson(createStarterDeck(bearer, "Primo mazzo")).path("id").asString();

        // When: la modifica toglie due carte (18 in tutto)
        JsonNode cards = objectMapper.readTree(starterDeckBody(bearer, "Primo mazzo")).path("cards");
        List<Map<String, Object>> fewerCards = new ArrayList<>();
        int toRemove = 2;
        for (JsonNode card : cards) {
            int quantity = card.path("quantity").asInt();
            int removed = Math.min(toRemove, quantity);
            toRemove -= removed;
            if (quantity - removed > 0) {
                fewerCards.add(Map.of("cardId", card.path("cardId").asString(), "quantity", quantity - removed));
            }
        }
        String body = objectMapper.writeValueAsString(Map.of("name", "Mazzo corto", "cards", fewerCards));
        mockMvc.perform(put("/api/v1/decks/" + deckId).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DECK"));

        // Then: nel database restano il nome e le 20 carte di prima
        assertEquals(20, count(DECK_CARDS, username));
        assertEquals("Primo mazzo", jdbcTemplate.queryForObject(
                "SELECT name FROM decks WHERE id = ?::uuid", String.class, deckId));
    }

    @Test
    void createDeck_sameNameTwiceAtOnce_savesOnlyOneDeck() throws Exception {
        // Given: due salvataggi dello stesso mazzo che partono insieme (doppio clic, due schede)
        String username = register();
        String bearer = bearer(login(username));
        String body = starterDeckBody(bearer, "Doppio clic");
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> save = () -> {
            start.await();
            return mockMvc.perform(post("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, bearer)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getStatus();
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Integer>> results = new ArrayList<>();

        // When
        try {
            results.add(executor.submit(save));
            results.add(executor.submit(save));
            start.countDown();
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }

        // Then: uno riesce, l'altro riceve 409 (DECK_NAME_TAKEN o CONFLICT), mai un errore 500
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get());
        }
        assertTrue(statuses.contains(201) && statuses.contains(409), "Risposte: " + statuses);
        assertEquals(1, count(DECKS, username));
        assertEquals(20, count(DECK_CARDS, username));
    }

    @Test
    void deleteDeck_savedDeck_removesDeckAndItsCards() throws Exception {
        // Given
        String username = register();
        String bearer = bearer(login(username));
        String deckId = readJson(createStarterDeck(bearer, "Da cancellare")).path("id").asString();

        // When
        mockMvc.perform(delete("/api/v1/decks/" + deckId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        // Then
        assertEquals(0, count(DECKS, username));
        assertEquals(0, count(DECK_CARDS, username));
        mockMvc.perform(get("/api/v1/decks/" + deckId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
    }

    private MvcResult createStarterDeck(String bearer, String name) throws Exception {
        return mockMvc.perform(post("/api/v1/decks").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(starterDeckBody(bearer, name)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    // Sceglie la fazione Dreams (solo la prima volta) e prepara il corpo di POST /decks con le 20 carte
    // del set iniziale, una voce per carta con le sue copie
    private String starterDeckBody(String bearer, String name) throws Exception {
        MvcResult welcome = mockMvc.perform(get("/api/v1/me/welcome").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andReturn();
        if (readJson(welcome).path("starterFaction").isNull()) {
            welcome = mockMvc.perform(post("/api/v1/me/welcome").header(HttpHeaders.AUTHORIZATION, bearer)
                            .contentType(MediaType.APPLICATION_JSON).content("{ \"starterFaction\": \"DREAMS\" }"))
                    .andExpect(status().isCreated())
                    .andReturn();
        }
        Map<String, Integer> copies = new LinkedHashMap<>();
        for (JsonNode item : readJson(welcome).path("starterCards")) {
            copies.merge(item.path("card").path("id").asString(), 1, Integer::sum);
        }
        List<Map<String, Object>> cards = new ArrayList<>();
        copies.forEach((cardId, quantity) -> cards.add(Map.of("cardId", cardId, "quantity", quantity)));
        return objectMapper.writeValueAsString(Map.of("name", name, "cards", cards));
    }
}
