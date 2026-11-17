package it.somnia.dreamscape.api.match.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import it.somnia.dreamscape.api.common.config.EngineProperties;
import it.somnia.dreamscape.api.match.service.MatchExceptions.EngineUnavailableException;
import it.somnia.dreamscape.engine.EndReason;
import it.somnia.dreamscape.engine.Side;
import it.somnia.dreamscape.engine.SimulationRequest;
import it.somnia.dreamscape.engine.input.CardEffect;
import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.Faction;
import it.somnia.dreamscape.engine.input.FactionAdvantage;
import it.somnia.dreamscape.engine.input.GameRules;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// HttpBattleSimulator contro un motore finto: un server HTTP del JDK che risponde come engine-service.
// Non serve né Docker né il motore vero: si controllano la chiamata, la chiave e ogni modo in cui il motore
// può non rispondere (tutti 503 ENGINE_UNAVAILABLE)
class HttpBattleSimulatorTest {

    private static final String KEY = "chiave-dei-test";
    private static final UUID CHALLENGER_CARD = UUID.fromString("019a1f30-0000-7000-8000-000000000003");
    private static final UUID OPPONENT_CARD = UUID.fromString("019a1f30-0000-7000-8000-000000000008");

    // Una risposta di engine-service con un round solo (formato in docs/database/schema.md)
    private static final String ENGINE_RESPONSE = """
            { "engineVersion": "0.2.0", "winner": "OPPONENT", "endReason": "LIFE_POINTS", "totalRounds": 1,
              "challengerLifePoints": 0, "opponentLifePoints": 6, "coinTossWinner": "OPPONENT",
              "log": { "engineVersion": "0.2.0", "coinTossWinner": "OPPONENT", "rounds": [
                { "round": 1, "clash": 1, "events": [
                  { "phase": "START", "type": "CARD_PLAYED", "side": "CHALLENGER",
                    "cardId": "019a1f30-0000-7000-8000-000000000003", "attack": 4, "defense": 7 } ],
                  "outcome": "NONE_DIED", "afterRound": {} } ] } }""";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final AtomicInteger calls = new AtomicInteger();

    private HttpServer engine;
    private int responseStatus;
    private String responseBody;
    private Duration responseDelay;
    private String receivedKey;
    private String receivedBody;

    @BeforeEach
    void startFakeEngine() throws IOException {
        responseStatus = 200;
        responseBody = ENGINE_RESPONSE;
        responseDelay = Duration.ZERO;
        engine = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        engine.createContext(HttpBattleSimulator.SIMULATIONS_PATH, this::answer);
        engine.start();
    }

    @AfterEach
    void stopFakeEngine() {
        engine.stop(0);
    }

    @Test
    void simulate_engineAnswers_returnsTheResultAndTheRawLog() {
        // When
        SimulationResult result = simulator(KEY).simulate(request());

        // Then
        assertEquals("0.2.0", result.engineVersion());
        assertEquals(Side.OPPONENT, result.winner());
        assertEquals(EndReason.LIFE_POINTS, result.endReason());
        assertEquals(1, result.totalRounds());
        assertEquals(0, result.challengerLifePoints());
        assertEquals(6, result.opponentLifePoints());
        assertEquals(Side.OPPONENT, result.coinTossWinner());
        assertFalse(result.isDraw());
        // Il log resta com'è, pronto per matches.round_log
        assertEquals("CARD_PLAYED", result.log().get("rounds").get(0).get("events").get(0).get("type").asString());
    }

    @Test
    void simulate_sendsTheKeyAndTheRequestOfEngineCore() {
        // When
        simulator(KEY).simulate(request());

        // Then
        assertEquals(KEY, receivedKey);
        JsonNode body = jsonMapper.readTree(receivedBody);
        assertEquals(42L, body.get("seed").asLong());
        assertTrue(body.get("withLog").asBoolean());
        JsonNode card = body.get("challengerDeck").get("cards").get(0);
        assertEquals(CHALLENGER_CARD.toString(), card.get("cardId").asString());
        assertEquals("DREAMS", card.get("faction").asString());
        assertEquals(20, card.get("quantity").asInt());
        assertEquals("WEAKENING", card.get("effects").get(0).get("code").asString());
        assertEquals(2, card.get("effects").get(0).get("value").asInt());
        assertEquals(20, body.get("rules").get("startingLifePoints").asInt());
        assertEquals(60, body.get("rules").get("roundLimit").asInt());
        assertEquals(1, body.get("rules").get("factionAdvantages").get(0).get("attackBonus").asInt());
    }

    @Test
    void simulate_drawWithoutLog_returnsNoWinnerAndNoLog() {
        // Given
        responseBody = """
                { "engineVersion": "0.2.0", "winner": null, "endReason": "ROUND_LIMIT", "totalRounds": 60,
                  "challengerLifePoints": 4, "opponentLifePoints": 6, "coinTossWinner": "CHALLENGER", "log": null }""";

        // When
        SimulationResult result = simulator(KEY).simulate(request());

        // Then
        assertTrue(result.isDraw());
        assertNull(result.log());
    }

    @Test
    void simulate_engineDown_throwsEngineUnavailable() {
        // Given: nessuno risponde sulla porta del motore
        EngineProperties properties = properties(KEY, Duration.ofSeconds(5));
        engine.stop(0);

        // When / Then
        assertEngineUnavailable(() -> new HttpBattleSimulator(properties).simulate(request()));
    }

    @Test
    void simulate_engineRejectsTheKey_throwsEngineUnavailable() {
        // Given
        responseStatus = 401;
        responseBody = "{ \"status\": 401, \"code\": \"INVALID_ENGINE_KEY\", \"message\": \"Chiave sbagliata\" }";

        // When / Then
        assertEngineUnavailable(() -> simulator(KEY).simulate(request()));
    }

    @Test
    void simulate_engineError_throwsEngineUnavailable() {
        // Given
        responseStatus = 500;
        responseBody = "{ \"status\": 500, \"code\": \"ENGINE_ERROR\", \"message\": \"Errore\" }";

        // When / Then
        assertEngineUnavailable(() -> simulator(KEY).simulate(request()));
    }

    @Test
    void simulate_engineTooSlow_throwsEngineUnavailable() {
        // Given: il motore risponde dopo il tempo massimo
        responseDelay = Duration.ofSeconds(2);
        HttpBattleSimulator simulator = new HttpBattleSimulator(properties(KEY, Duration.ofMillis(200)));

        // When / Then
        assertEngineUnavailable(() -> simulator.simulate(request()));
    }

    @Test
    void simulate_answerWithoutResult_throwsEngineUnavailable() {
        // Given
        responseBody = "{}";

        // When / Then
        assertEngineUnavailable(() -> simulator(KEY).simulate(request()));
    }

    @Test
    void simulate_withoutApiKey_throwsWithoutCallingTheEngine() {
        // When / Then
        assertEngineUnavailable(() -> simulator("").simulate(request()));
        assertEquals(0, calls.get());
    }

    private void answer(HttpExchange exchange) throws IOException {
        calls.incrementAndGet();
        receivedKey = exchange.getRequestHeaders().getFirst(HttpBattleSimulator.KEY_HEADER);
        receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            Thread.sleep(responseDelay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private HttpBattleSimulator simulator(String key) {
        return new HttpBattleSimulator(properties(key, Duration.ofSeconds(5)));
    }

    private EngineProperties properties(String key, Duration readTimeout) {
        return new EngineProperties("http://127.0.0.1:" + engine.getAddress().getPort(), key, Duration.ofSeconds(1),
                readTimeout);
    }

    private static void assertEngineUnavailable(Runnable call) {
        EngineUnavailableException exception = assertThrows(EngineUnavailableException.class, call::run);
        assertEquals(503, exception.getStatus().value());
        assertEquals("ENGINE_UNAVAILABLE", exception.getCode());
    }

    private static SimulationRequest request() {
        DeckSnapshot challenger = new DeckSnapshot(List.of(new CardSnapshot(CHALLENGER_CARD, "Il Faro nella Nebbia", 4,
                7, Faction.DREAMS, 20, List.of(new CardEffect("WEAKENING", 2)))));
        DeckSnapshot opponent = new DeckSnapshot(List.of(new CardSnapshot(OPPONENT_CARD, "L'Uomo Senza Volto", 8, 5,
                Faction.NIGHTMARES, 20, List.of())));
        GameRules rules = new GameRules(20, 60,
                List.of(new FactionAdvantage(Faction.NIGHTMARES, Faction.DREAMS, 1, 0)));
        return new SimulationRequest(challenger, opponent, rules, 42L, true);
    }
}
