package it.somnia.dreamscape.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import it.somnia.dreamscape.api.common.security.RefreshCookies;
import jakarta.servlet.http.Cookie;

// Accesso dalla registrazione al logout, con ogni passo salvato davvero nel database
class AccessIntegrationTest extends IntegrationTestSupport {

    @Test
    void registerAndLogin_newPlayer_savesSignupBonusAndDailyReward() throws Exception {
        // Given
        String username = register();

        // When
        MvcResult result = login(username);

        // Then: 500 DC della registrazione e 50 della ricompensa, ognuno con il suo movimento
        assertEquals(550, readJson(result).path("user").path("credits").asInt());
        assertEquals(550, credits(username));
        assertEquals(2, count("""
                SELECT count(*) FROM credit_transactions ct JOIN users u ON u.id = ct.user_id
                WHERE u.username = ? AND ct.type IN ('SIGNUP_BONUS', 'DAILY_REWARD')""", username));
    }

    @Test
    void loginRefreshLogout_fullSession_endsWithNoValidRefreshToken() throws Exception {
        // Given: accesso e un rinnovo del token, come all'apertura di una pagina
        String username = register();
        Cookie loginCookie = login(username).getResponse().getCookie(RefreshCookies.NAME);
        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(loginCookie))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshedCookie = refreshed.getResponse().getCookie(RefreshCookies.NAME);
        assertNotEquals(loginCookie.getValue(), refreshedCookie.getValue());
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(refreshed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        // When
        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshedCookie)).andExpect(status().isNoContent());

        // Then: nessun token di rinnovo ancora valido nel database, e il cookie non funziona più
        assertEquals(0, count("""
                SELECT count(*) FROM refresh_tokens rt JOIN users u ON u.id = rt.user_id
                WHERE u.username = ? AND rt.revoked_at IS NULL""", username));
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshedCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_stolenCookieReused_revokesEveryDevice() throws Exception {
        // Given: due dispositivi; sul primo il token viene rinnovato, poi qualcuno riusa quello vecchio
        String username = register();
        Cookie laptopOld = login(username).getResponse().getCookie(RefreshCookies.NAME);
        Cookie phone = login(username).getResponse().getCookie(RefreshCookies.NAME);
        Cookie laptopNew = mockMvc.perform(post("/api/v1/auth/refresh").cookie(laptopOld))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(RefreshCookies.NAME);

        // When
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(laptopOld))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        // Then: la revoca è salvata anche se la richiesta finisce con un errore
        assertEquals(0, count("""
                SELECT count(*) FROM refresh_tokens rt JOIN users u ON u.id = rt.user_id
                WHERE u.username = ? AND rt.revoked_at IS NULL""", username));
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(laptopNew)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(phone)).andExpect(status().isUnauthorized());
    }
}
