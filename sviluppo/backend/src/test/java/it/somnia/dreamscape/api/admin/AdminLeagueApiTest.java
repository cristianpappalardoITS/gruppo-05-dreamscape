package it.somnia.dreamscape.api.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

// Leghe: GET /leagues per i giocatori e GET, PUT /admin/leagues per l'admin, con le quattro leghe di V10
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "dreamscape.admin.username=admin_test",
        "dreamscape.admin.email=admin_test@example.test",
        "dreamscape.admin.password=password-admin-dei-test" })
@AutoConfigureMockMvc
@Transactional
class AdminLeagueApiTest extends AdminApiTestSupport {

    // Tutte le soglie salgono: con un aggiornamento alla volta nell'ordine sbagliato due leghe avrebbero la stessa
    // soglia per un momento (uq_leagues_min_rating)
    private static final String RAISED_THRESHOLDS = """
            { "leagues": [
              { "code": "DORMANT", "minRating": 0, "rewardCredits": 0 },
              { "code": "DREAMER", "minRating": 1300, "rewardCredits": 100 },
              { "code": "LUCID", "minRating": 1500, "rewardCredits": 200 },
              { "code": "DREAMWEAVER", "minRating": 1700, "rewardCredits": 400 } ] }""";

    private String admin;

    @BeforeEach
    void logIn() throws Exception {
        admin = bearer(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void listLeagues_player_returnsTheFourLeaguesFromTheLowest() throws Exception {
        mockMvc.perform(get("/api/v1/leagues").header(HttpHeaders.AUTHORIZATION, player()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].code").value("DORMANT"))
                .andExpect(jsonPath("$[0].minRating").value(0))
                .andExpect(jsonPath("$[1].name").value("Sognatore"))
                .andExpect(jsonPath("$[1].minRating").value(1100))
                .andExpect(jsonPath("$[1].rewardCredits").value(0))
                .andExpect(jsonPath("$[3].name").value("Dreamweaver"))
                .andExpect(jsonPath("$[3].minRating").value(1500));
    }

    @Test
    void listLeagues_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/leagues")).andExpect(status().isUnauthorized());
    }

    @Test
    void updateLeagues_allThresholdsRaised_savesThemAndTheDashboardUsesThem() throws Exception {
        // When
        updateLeagues(RAISED_THRESHOLDS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].minRating").value(1300))
                .andExpect(jsonPath("$[3].minRating").value(1700))
                .andExpect(jsonPath("$[3].rewardCredits").value(400));

        // Then: le stesse soglie per i giocatori; a 1000 punti la lega successiva parte ora da 1300
        mockMvc.perform(get("/api/v1/leagues").header(HttpHeaders.AUTHORIZATION, player()))
                .andExpect(jsonPath("$[2].minRating").value(1500))
                .andExpect(jsonPath("$[2].rewardCredits").value(200));
        mockMvc.perform(get("/api/v1/me/summary").header(HttpHeaders.AUTHORIZATION, player()))
                .andExpect(jsonPath("$.league.code").value("DORMANT"))
                .andExpect(jsonPath("$.league.nextLeagueMinRating").value(1300));
    }

    @Test
    void updateLeagues_thresholdsLoweredInAnyOrder_savesThem() throws Exception {
        // Given: le leghe in un ordine diverso da quello delle soglie
        String lowered = """
                { "leagues": [
                  { "code": "DREAMWEAVER", "minRating": 1200, "rewardCredits": 0 },
                  { "code": "LUCID", "minRating": 1100, "rewardCredits": 0 },
                  { "code": "DREAMER", "minRating": 1050, "rewardCredits": 0 },
                  { "code": "DORMANT", "minRating": 0, "rewardCredits": 0 } ] }""";

        // When / Then
        updateLeagues(lowered)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DORMANT"))
                .andExpect(jsonPath("$[1].minRating").value(1050))
                .andExpect(jsonPath("$[2].minRating").value(1100))
                .andExpect(jsonPath("$[3].minRating").value(1200));
    }

    @Test
    void updateLeagues_lowestLeagueNotFromZero_returns400() throws Exception {
        updateLeagues(RAISED_THRESHOLDS.replace("\"minRating\": 0", "\"minRating\": 10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("leagues[0].minRating"));
    }

    @Test
    void updateLeagues_rewardOnTheLowestLeague_returns400() throws Exception {
        updateLeagues(RAISED_THRESHOLDS.replace("\"minRating\": 0, \"rewardCredits\": 0",
                        "\"minRating\": 0, \"rewardCredits\": 50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("leagues[0].rewardCredits"));
    }

    @Test
    void updateLeagues_thresholdNotAboveThePreviousLeague_returns400() throws Exception {
        updateLeagues(RAISED_THRESHOLDS.replace("\"minRating\": 1500", "\"minRating\": 1300"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("leagues[2].minRating"));
    }

    @Test
    void updateLeagues_missingLeague_returns400() throws Exception {
        String threeLeagues = """
                { "leagues": [
                  { "code": "DORMANT", "minRating": 0, "rewardCredits": 0 },
                  { "code": "DREAMER", "minRating": 1100, "rewardCredits": 0 },
                  { "code": "LUCID", "minRating": 1300, "rewardCredits": 0 } ] }""";

        updateLeagues(threeLeagues)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("leagues"));
    }

    @Test
    void updateLeagues_unknownOrRepeatedLeague_returns400() throws Exception {
        updateLeagues(RAISED_THRESHOLDS.replace("DREAMWEAVER", "NIGHTMARE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("leagues[3].code"));
        updateLeagues(RAISED_THRESHOLDS.replace("DREAMWEAVER", "LUCID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("leagues[3].code"));
    }

    @Test
    void updateLeagues_negativeReward_returns400() throws Exception {
        updateLeagues(RAISED_THRESHOLDS.replace("\"rewardCredits\": 400", "\"rewardCredits\": -1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateLeagues_player_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/admin/leagues").header(HttpHeaders.AUTHORIZATION, player())
                        .contentType(MediaType.APPLICATION_JSON).content(RAISED_THRESHOLDS))
                .andExpect(status().isForbidden());
    }

    private ResultActions updateLeagues(String body) throws Exception {
        return mockMvc.perform(put("/api/v1/admin/leagues").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String player() throws Exception {
        return bearer(PLAYER_USERNAME, PLAYER_PASSWORD);
    }
}
