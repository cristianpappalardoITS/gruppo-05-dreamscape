package it.somnia.dreamscape.engine.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.somnia.dreamscape.engine.battle.BattleEngine;
import it.somnia.dreamscape.engine.server.config.EngineKeyFilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// POST /api/v1/simulations con la chiave giusta, senza chiave e con dati rotti. L'ultimo test controlla che
// i nomi dei campi del log siano quelli di docs/database/schema.md, che il frontend usa per il replay
@SpringBootTest
@AutoConfigureMockMvc
class SimulationApiTest {

    // La stessa chiave di src/test/resources/application.properties
    private static final String KEY = "chiave-dei-test-del-motore";
    private static final String URL = "/api/v1/simulations";

    private static final String RULES = """
            { "startingLifePoints": 20, "roundLimit": 60, "factionAdvantages": [
                { "faction": "DREAMS", "target": "MEMORIES", "attackBonus": 1, "defenseBonus": 0 },
                { "faction": "MEMORIES", "target": "NIGHTMARES", "attackBonus": 1, "defenseBonus": 0 },
                { "faction": "NIGHTMARES", "target": "DREAMS", "attackBonus": 1, "defenseBonus": 0 } ] }""";

    // Mazzi da 20 carte con effetti che lasciano nel log tutti gli otto tipi di evento
    private static final String CHALLENGER_DECK = deck(
            card("00000000-0000-7000-8000-000000000001", "Il Faro nella Nebbia", 5, 6, "DREAMS", 7,
                    "{ \"code\": \"WEAKENING\", \"value\": 2 }"),
            card("00000000-0000-7000-8000-000000000002", "La Coperta Calda", 3, 7, "DREAMS", 7,
                    "{ \"code\": \"HOPE\", \"value\": 3 }"),
            card("00000000-0000-7000-8000-000000000003", "Il Gatto Sognante", 4, 5, "DREAMS", 6, null));
    private static final String OPPONENT_DECK = deck(
            card("00000000-0000-7000-8000-000000000004", "Lo Scudo d'Ombra", 4, 6, "NIGHTMARES", 7,
                    "{ \"code\": \"BARRIER\", \"value\": null }"),
            card("00000000-0000-7000-8000-000000000005", "L'Uomo Senza Volto", 6, 5, "NIGHTMARES", 7, null),
            card("00000000-0000-7000-8000-000000000006", "Il Corridoio Infinito", 5, 5, "NIGHTMARES", 6, null));

    // I campi di ogni tipo di evento, nell'ordine di docs/database/schema.md
    private static final Map<String, List<String>> EVENT_FIELDS = Map.of(
            "CARD_PLAYED", List.of("phase", "type", "side", "cardId", "attack", "defense"),
            "EFFECT", List.of("phase", "type", "side", "cardId", "code", "value", "targetSide", "target", "stat",
                    "before", "after", "untilEndOfRound"),
            "EFFECT_BLOCKED", List.of("phase", "type", "side", "cardId", "code", "blocked"),
            "ATTACK", List.of("phase", "type", "side", "cardId", "attack", "factionBonus", "attackModifier",
                    "damage", "defenseBefore", "defenseAfter"),
            "CARD_DIED", List.of("phase", "type", "side", "cardId"),
            "PLAYER_DAMAGE", List.of("phase", "type", "side", "amount", "lifePointsBefore", "lifePointsAfter"),
            "BONUS_EXPIRED", List.of("phase", "type", "side", "cardId", "code", "stat", "before", "after"),
            "GAME_OVER", List.of("phase", "type", "winner", "endReason"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void simulate_withKey_returnsResultAndLog() throws Exception {
        // When / Then
        simulate(request(42, true), KEY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engineVersion").value(BattleEngine.ENGINE_VERSION))
                .andExpect(jsonPath("$.endReason").isNotEmpty())
                .andExpect(jsonPath("$.coinTossWinner").isNotEmpty())
                .andExpect(jsonPath("$.log.engineVersion").value(BattleEngine.ENGINE_VERSION))
                .andExpect(jsonPath("$.log.rounds[0].round").value(1))
                .andExpect(jsonPath("$.log.rounds[0].events[0].phase").value("START"))
                .andExpect(jsonPath("$.log.rounds[0].events[0].type").value("CARD_PLAYED"));
    }

    @Test
    void simulate_withoutLog_returnsOnlyTheResult() throws Exception {
        // When / Then
        simulate(request(42, false), KEY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engineVersion").value(BattleEngine.ENGINE_VERSION))
                .andExpect(jsonPath("$.totalRounds").isNumber())
                .andExpect(jsonPath("$.log").doesNotExist());
    }

    @Test
    void simulate_sameSeedTwice_returnsTheSameMatch() throws Exception {
        // When
        String first = body(request(7, true));
        String second = body(request(7, true));

        // Then
        assertEquals(first, second);
    }

    @Test
    void simulate_withoutKey_returns401() throws Exception {
        // When / Then
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(request(42, true)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_ENGINE_KEY"));
    }

    @Test
    void simulate_wrongKey_returns401() throws Exception {
        // When / Then
        simulate(request(42, true), KEY + "-sbagliata")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ENGINE_KEY"));
    }

    @Test
    void unknownPath_withoutKey_returns401() throws Exception {
        // Senza chiave non si scopre nemmeno quali indirizzi esistono
        mockMvc.perform(get("/api/v1/altro"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void simulate_emptyDeck_returns400() throws Exception {
        // Given
        String request = request("{ \"cards\": [] }", OPPONENT_DECK, "42", true);

        // When / Then
        simulate(request, KEY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SIMULATION"))
                .andExpect(jsonPath("$.message").value("Richiesta della partita non valida: Il mazzo è vuoto"));
    }

    @Test
    void simulate_unknownEffect_returns400() throws Exception {
        // Given: un effetto che il motore non conosce si scopre solo quando la carta viene giocata
        String deck = deck(card("00000000-0000-7000-8000-000000000009", "La Carta Strana", 4, 5, "DREAMS", 20,
                "{ \"code\": \"TELEPORT\", \"value\": 1 }"));

        // When / Then
        simulate(request(deck, OPPONENT_DECK, "42", true), KEY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SIMULATION"));
    }

    @Test
    void simulate_withoutSeed_returns400() throws Exception {
        // When / Then: senza seed la partita non si potrebbe rigiocare
        simulate(request(CHALLENGER_DECK, OPPONENT_DECK, "null", true), KEY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SIMULATION"));
    }

    @Test
    void simulate_malformedJson_returns400() throws Exception {
        // When / Then
        simulate("{ \"challengerDeck\": ", KEY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SIMULATION"));
    }

    @Test
    void simulate_log_hasTheFieldNamesOfSchemaMd() throws Exception {
        // When
        JsonNode response = objectMapper.readTree(body(request(42, true)));

        // Then: risposta, log, round e stato a fine round
        assertEquals(List.of("engineVersion", "winner", "endReason", "totalRounds", "challengerLifePoints",
                "opponentLifePoints", "coinTossWinner", "log"), names(response));
        JsonNode log = response.get("log");
        assertEquals(List.of("engineVersion", "coinTossWinner", "rounds"), names(log));
        assertEquals(response.get("totalRounds").asInt(), log.get("rounds").size());
        Set<String> seenTypes = new HashSet<>();
        for (JsonNode round : log.get("rounds")) {
            assertEquals(List.of("round", "clash", "events", "outcome", "afterRound"), names(round));
            assertEquals(List.of("challenger", "opponent"), names(round.get("afterRound")));
            for (JsonNode side : List.of(round.get("afterRound").get("challenger"),
                    round.get("afterRound").get("opponent"))) {
                assertEquals(List.of("cardId", "attack", "defense", "lifePoints", "cardsInDeck", "cardsInDiscard"),
                        names(side));
            }
            // Ogni evento ha i campi del suo tipo, nello stesso ordine
            for (JsonNode event : round.get("events")) {
                String type = event.get("type").asString();
                assertEquals(EVENT_FIELDS.get(type), names(event), "Campi dell'evento " + type);
                seenTypes.add(type);
            }
        }
        assertEquals(EVENT_FIELDS.keySet(), seenTypes, "I mazzi del test devono produrre tutti i tipi di evento");
    }

    private ResultActions simulate(String request, String key) throws Exception {
        return mockMvc.perform(post(URL)
                .header(EngineKeyFilter.KEY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
    }

    private String body(String request) throws Exception {
        String body = simulate(request, KEY).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertTrue(body.startsWith("{"));
        return body;
    }

    private static List<String> names(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.properties().forEach(entry -> names.add(entry.getKey()));
        return names;
    }

    private static String request(long seed, boolean withLog) {
        return request(CHALLENGER_DECK, OPPONENT_DECK, Long.toString(seed), withLog);
    }

    private static String request(String challengerDeck, String opponentDeck, String seed, boolean withLog) {
        return """
                { "challengerDeck": %s, "opponentDeck": %s, "rules": %s, "seed": %s, "withLog": %s }"""
                .formatted(challengerDeck, opponentDeck, RULES, seed, withLog);
    }

    private static String deck(String... cards) {
        return "{ \"cards\": [" + String.join(", ", cards) + "] }";
    }

    // Una carta come nella copia del mazzo salvata in matches (schema.md): i campi in più vengono ignorati
    private static String card(String id, String name, int attack, int defense, String faction, int quantity,
                               String effect) {
        return """
                { "cardId": "%s", "cardCode": "BASE-000", "name": "%s", "attack": %d, "defense": %d,
                  "faction": "%s", "rarity": "COMMON", "quantity": %d, "effects": [%s] }"""
                .formatted(id, name, attack, defense, faction, quantity, effect == null ? "" : effect);
    }
}
