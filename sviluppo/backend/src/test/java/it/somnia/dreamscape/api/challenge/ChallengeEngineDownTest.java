package it.somnia.dreamscape.api.challenge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
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
import it.somnia.dreamscape.api.match.service.InProcessBattleSimulator;
import tools.jackson.databind.ObjectMapper;

// Il motore non risponde all'accettazione: niente partita, la sfida resta in attesa (guida per il frontend)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChallengeEngineDownTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InProcessBattleSimulator engine;

    @AfterEach
    void turnTheEngineBackOn() {
        engine.setUnavailable(false);
    }

    @Test
    void acceptChallenge_engineDown_returnsEngineUnavailableAndKeepsTheChallenge() throws Exception {
        // Given: una sfida pronta da giocare, con i mazzi scritti a mano dalle carte di prova
        String anna = bearer("anna_test");
        String bruno = bearer("bruno_test");
        UUID annaDeck = testDeck("anna_test");
        UUID brunoDeck = testDeck("bruno_test");
        String created = mockMvc.perform(post("/api/v1/challenges").header(HttpHeaders.AUTHORIZATION, anna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"opponentId\": \"" + userId("bruno_test") + "\", \"deckId\": \"" + annaDeck
                                + "\" }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String challengeId = objectMapper.readTree(created).path("id").asString();
        engine.setUnavailable(true);

        // When
        mockMvc.perform(post("/api/v1/challenges/{challengeId}/accept", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, bruno)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"deckId\": \"" + brunoDeck + "\" }"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ENGINE_UNAVAILABLE"));

        // Then
        mockMvc.perform(get("/api/v1/challenges").param("box", "received").header(HttpHeaders.AUTHORIZATION, bruno))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // 20 carte Comuni diverse delle carte di prova, una copia ciascuna, date al giocatore: un mazzo valido
    private UUID testDeck(String username) {
        UUID userId = userId(username);
        jdbcTemplate.update("""
                INSERT INTO user_cards (user_id, card_id, quantity)
                SELECT ?, c.id, 1 FROM cards c JOIN rarities r ON r.id = c.rarity_id
                WHERE c.code LIKE 'TEST-%' AND c.base_card_id IS NULL AND r.code = 'COMMON'
                ORDER BY c.code LIMIT 20
                ON CONFLICT DO NOTHING""", userId);
        UUID deckId = jdbcTemplate.queryForObject("INSERT INTO decks (user_id, name) VALUES (?, 'Comuni') RETURNING id",
                UUID.class, userId);
        jdbcTemplate.update("""
                INSERT INTO deck_cards (deck_id, card_id, quantity)
                SELECT ?, c.id, 1 FROM cards c JOIN rarities r ON r.id = c.rarity_id
                WHERE c.code LIKE 'TEST-%' AND c.base_card_id IS NULL AND r.code = 'COMMON'
                ORDER BY c.code LIMIT 20""", deckId);
        return deckId;
    }

    private String bearer(String username) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"login\": \"" + username + "\", \"password\": \"prova-dreamscape\" }"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(response).path("accessToken").asString();
    }

    private UUID userId(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", UUID.class, username);
    }
}
