package it.somnia.dreamscape.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Base dei test di integrazione (#88): API vere su PostgreSQL 18 in Testcontainers.
// Niente @Transactional: ogni chiamata ha la sua transazione e fa il commit, come una richiesta vera.
// Per questo ogni test registra utenti suoi e li cancella alla fine (ON DELETE CASCADE porta via il resto).
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "password-integrazione";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    private final List<String> createdUsernames = new ArrayList<>();

    @AfterEach
    void deleteCreatedUsers() {
        createdUsernames.forEach(username -> jdbcTemplate.update("DELETE FROM users WHERE username = ?", username));
    }

    /**
     * Registra un giocatore nuovo con uno username che non si ripete tra un test e l'altro.
     *
     * @return lo username del giocatore
     */
    protected String register() throws Exception {
        String username = "integra_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String body = """
                { "username": "%s", "email": "%s@example.test", "password": "%s",
                  "privacyAccepted": true, "termsAccepted": true }""".formatted(username, username, PASSWORD);
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        return username;
    }

    protected MvcResult login(String username) throws Exception {
        String body = "{ \"login\": \"" + username + "\", \"password\": \"" + PASSWORD + "\" }";
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
    }

    protected String bearer(MvcResult loginOrRefresh) throws Exception {
        return "Bearer " + readJson(loginOrRefresh).path("accessToken").asString();
    }

    protected JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected int credits(String username) {
        return jdbcTemplate.queryForObject("SELECT credits FROM users WHERE username = ?", Integer.class, username);
    }

    protected int count(String sql, String username) {
        return jdbcTemplate.queryForObject(sql, Integer.class, username);
    }
}
