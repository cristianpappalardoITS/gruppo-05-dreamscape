package it.somnia.dreamscape.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import tools.jackson.databind.JsonNode;

// Dashboard admin (GET /admin/stats). Altri test possono aver lasciato dati nel database: si confrontano i numeri
// prima e dopo, o con lo stesso conteggio fatto in SQL
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "dreamscape.admin.username=admin_test",
        "dreamscape.admin.email=admin_test@example.test",
        "dreamscape.admin.password=password-admin-dei-test" })
@AutoConfigureMockMvc
@Transactional
class AdminStatsApiTest extends AdminApiTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String admin;

    @BeforeEach
    void logIn() throws Exception {
        admin = bearer(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void getStats_admin_countsCardsPacksAndMatchesLikeTheDatabase() throws Exception {
        // When
        JsonNode stats = stats();

        // Then
        assertThat(stats.path("generatedAt").asString()).isNotBlank();
        assertThat(stats.path("cards").path("active").asLong()).isEqualTo(count(
                "SELECT COUNT(*) FROM cards WHERE base_card_id IS NULL AND is_active"));
        assertThat(stats.path("cards").path("fullArt").asLong()).isEqualTo(count(
                "SELECT COUNT(*) FROM cards WHERE variant_type = 'FULL_ART' AND is_active"));
        assertThat(stats.path("cards").path("disabled").asLong()).isEqualTo(count(
                "SELECT COUNT(*) FROM cards WHERE NOT is_active"));
        assertThat(stats.path("packs").path("openings").asLong()).isEqualTo(count(
                "SELECT COUNT(*) FROM pack_openings"));
        assertThat(stats.path("matches").path("ranked").asLong()).isEqualTo(count(
                "SELECT COUNT(*) FROM matches WHERE match_type = 'RANKED'"));
        assertThat(stats.path("matches").path("casual").asLong()).isEqualTo(count(
                "SELECT COUNT(*) FROM matches WHERE match_type = 'CASUAL'"));
        assertThat(stats.path("credits").path("inCirculation").asLong()).isEqualTo(count(
                "SELECT COALESCE(SUM(credits), 0) FROM users WHERE role = 'PLAYER'"));
        // I pacchetti in vendita sono quelli del negozio
        String shop = mockMvc.perform(get("/api/v1/packs").header(HttpHeaders.AUTHORIZATION,
                        bearer(PLAYER_USERNAME, PLAYER_PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        assertThat(stats.path("packs").path("onSale").asLong()).isEqualTo(objectMapper.readTree(shop).size());
    }

    @Test
    void getStats_newPlayerAndCredits_areCountedTodayAndInTheLastDays() throws Exception {
        // Given
        JsonNode before = stats();

        // When: un giocatore nuovo si registra (bonus di 500 DC) e anna spende 100 DC
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        { "username": "nuova_stella", "email": "nuova_stella@example.test",
                          "password": "una-password-lunga", "privacyAccepted": true, "termsAccepted": true }"""))
                .andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE users SET credits = credits - 100 WHERE username = 'anna_test'");
        jdbcTemplate.update("INSERT INTO credit_transactions (user_id, type, amount, balance_after) "
                + "SELECT id, 'PACK_PURCHASE', -100, credits FROM users WHERE username = 'anna_test'");
        JsonNode after = stats();

        // Then
        assertThat(delta(before, after, "players", "total")).isEqualTo(1);
        assertThat(delta(before, after, "players", "newToday")).isEqualTo(1);
        assertThat(delta(before, after, "players", "newLast7Days")).isEqualTo(1);
        JsonNode lastDaysBefore = before.path("credits").path("last7Days");
        JsonNode lastDays = after.path("credits").path("last7Days");
        assertThat(lastDays.path("earned").asLong() - lastDaysBefore.path("earned").asLong()).isEqualTo(500);
        assertThat(lastDays.path("spent").asLong() - lastDaysBefore.path("spent").asLong()).isEqualTo(100);
        assertThat(typeTotals(lastDays, "PACK_PURCHASE").path("spent").asLong()
                - typeTotals(lastDaysBefore, "PACK_PURCHASE").path("spent").asLong()).isEqualTo(100);
        assertThat(typeTotals(lastDays, "SIGNUP_BONUS").path("transactions").asLong()
                - typeTotals(lastDaysBefore, "SIGNUP_BONUS").path("transactions").asLong()).isEqualTo(1);
        assertThat(delta(before, after, "credits", "inCirculation")).isEqualTo(400);
    }

    @Test
    void getStats_playerLoggedInToday_isActive() throws Exception {
        // Given: carla non ha mai fatto l'accesso
        jdbcTemplate.update("UPDATE users SET last_login_at = NULL WHERE username = 'carla_test'");
        JsonNode before = stats();

        // When
        bearer("carla_test", PLAYER_PASSWORD);

        // Then
        assertThat(delta(before, stats(), "players", "activeLast7Days")).isEqualTo(1);
    }

    @Test
    void getStats_player_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, bearer(PLAYER_USERNAME, PLAYER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    private JsonNode stats() throws Exception {
        String body = mockMvc.perform(get("/api/v1/admin/stats").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.total").isNumber())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private static long delta(JsonNode before, JsonNode after, String section, String field) {
        return after.path(section).path(field).asLong() - before.path(section).path(field).asLong();
    }

    // I numeri di un tipo di movimento; un nodo vuoto (tutti 0) se il tipo non c'è
    private static JsonNode typeTotals(JsonNode movements, String type) {
        for (JsonNode totals : movements.path("byType")) {
            if (type.equals(totals.path("type").asString())) {
                return totals;
            }
        }
        return movements.path("mancante");
    }
}
