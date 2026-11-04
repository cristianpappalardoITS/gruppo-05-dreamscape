package it.somnia.dreamscape.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.common.security.RefreshCookies;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.api.user.service.PasswordResetNotifier;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;

// Registrazione, accesso, refresh, uscita, profilo e recupero password, dalle API fino al database
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "dreamscape.admin.username=admin_test",
        "dreamscape.admin.email=admin_test@example.test",
        "dreamscape.admin.password=password-admin-dei-test" })
@AutoConfigureMockMvc
@Transactional
class AuthApiTest {

    private static final String TEST_PASSWORD = "prova-dreamscape";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private PasswordResetNotifier passwordResetNotifier;

    @Test
    void register_validRequest_createsPlayerWithSignupBonus() throws Exception {
        // Given
        String body = """
                { "username": "lucida", "email": "lucida@example.test", "password": "una-password-lunga",
                  "privacyAccepted": true, "termsAccepted": true }""";

        // When / Then
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("lucida"))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.credits").value(500));
        User user = userRepository.findByUsernameIgnoreCase("lucida").orElseThrow();
        assertTrue(user.getPrivacyAcceptedAt() != null && user.getTermsAcceptedAt() != null);
    }

    @Test
    void register_usernameTakenWithOtherCase_returns409() throws Exception {
        // Given: anna_test esiste già
        String body = """
                { "username": "ANNA_TEST", "email": "altra@example.test", "password": "una-password-lunga",
                  "privacyAccepted": true, "termsAccepted": true }""";

        // When / Then
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("username"));
    }

    @Test
    void register_shortPasswordAndNoTerms_returns400WithFieldErrors() throws Exception {
        // Given
        String body = """
                { "username": "lucida", "email": "lucida@example.test", "password": "corta",
                  "privacyAccepted": true, "termsAccepted": false }""";

        // When / Then
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void login_withEmail_returnsTokenCookieAndDailyReward() throws Exception {
        // Given: anna_test ha 500 DC e non ha ancora avuto la ricompensa di oggi

        // When
        MvcResult result = login("Anna_Test@example.test", TEST_PASSWORD);

        // Then
        Cookie cookie = result.getResponse().getCookie(RefreshCookies.NAME);
        assertTrue(cookie != null && cookie.isHttpOnly());
        assertEquals("/api/v1", cookie.getPath());
        assertEquals(550, readJson(result).path("user").path("credits").asInt());
        assertEquals(900, readJson(result).path("expiresIn").asInt());
    }

    @Test
    void login_twiceOnTheSameDay_givesTheDailyRewardOnce() throws Exception {
        // Given
        login("anna_test", TEST_PASSWORD);

        // When
        MvcResult second = login("anna_test", TEST_PASSWORD);

        // Then
        assertEquals(550, readJson(second).path("user").path("credits").asInt());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        // Given
        String body = """
                { "login": "anna_test", "password": "password-sbagliata" }""";

        // When / Then
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void getMe_withoutToken_returns401InTheErrorFormat() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMe_withToken_returnsTheProfile() throws Exception {
        // Given
        String token = accessToken(login("bruno_test", TEST_PASSWORD));

        // When / Then
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bruno_test"))
                .andExpect(jsonPath("$.email").value("bruno_test@example.test"));
    }

    @Test
    void refresh_validCookie_returnsNewTokenAndReplacesTheCookie() throws Exception {
        // Given
        Cookie oldCookie = login("anna_test", TEST_PASSWORD).getResponse().getCookie(RefreshCookies.NAME);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh").cookie(oldCookie))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        Cookie newCookie = result.getResponse().getCookie(RefreshCookies.NAME);
        assertNotEquals(oldCookie.getValue(), newCookie.getValue());
        assertTrue(readJson(result).path("accessToken").asString().length() > 20);
    }

    @Test
    void refresh_reusedOldCookie_disconnectsAllDevices() throws Exception {
        // Given: il vecchio cookie è già stato sostituito
        Cookie oldCookie = login("anna_test", TEST_PASSWORD).getResponse().getCookie(RefreshCookies.NAME);
        Cookie newCookie = mockMvc.perform(post("/api/v1/auth/refresh").cookie(oldCookie))
                .andReturn().getResponse().getCookie(RefreshCookies.NAME);

        // When: qualcuno riusa il vecchio
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(oldCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        // Then: anche il nuovo non vale più
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(newCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesTheRefreshToken() throws Exception {
        // Given
        Cookie cookie = login("anna_test", TEST_PASSWORD).getResponse().getCookie(RefreshCookies.NAME);

        // When
        mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie)).andExpect(status().isNoContent());

        // Then
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_rightCurrentPassword_disconnectsOnlyTheOtherDevices() throws Exception {
        // Given: anna_test collegata da due dispositivi
        MvcResult phone = login("anna_test", TEST_PASSWORD);
        Cookie laptop = login("anna_test", TEST_PASSWORD).getResponse().getCookie(RefreshCookies.NAME);
        Cookie phoneCookie = phone.getResponse().getCookie(RefreshCookies.NAME);
        String body = """
                { "currentPassword": "prova-dreamscape", "newPassword": "nuova-password-lunga" }""";

        // When: cambia la password dal telefono
        mockMvc.perform(put("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(phone))
                .cookie(phoneCookie).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        // Then
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(laptop)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(phoneCookie)).andExpect(status().isOk());
        login("anna_test", "nuova-password-lunga");
    }

    @Test
    void changePassword_wrongCurrentPassword_returns400() throws Exception {
        // Given
        String token = accessToken(login("anna_test", TEST_PASSWORD));
        String body = """
                { "currentPassword": "sbagliata", "newPassword": "nuova-password-lunga" }""";

        // When / Then
        mockMvc.perform(put("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"));
    }

    @Test
    void deleteAccount_rightPassword_anonymizesAndBlocksTheToken() throws Exception {
        // Given
        MvcResult session = login("carla_test", TEST_PASSWORD);
        String token = accessToken(session);
        User before = userRepository.findByUsernameIgnoreCase("carla_test").orElseThrow();

        // When
        mockMvc.perform(delete("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"prova-dreamscape\" }"))
                .andExpect(status().isNoContent());

        // Then
        User after = userRepository.findById(before.getId()).orElseThrow();
        assertEquals("deleted-" + before.getId().toString().replace("-", ""), after.getUsername());
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"login\": \"carla_test@example.test\", \"password\": \"prova-dreamscape\" }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordResetRequest_unknownEmail_returns202WithoutSending() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset-requests").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"nessuno@example.test\" }"))
                .andExpect(status().isAccepted());

        verify(passwordResetNotifier, never()).sendResetLink(anyString(), anyString(), anyString());
    }

    @Test
    void passwordReset_linkFromTheEmail_setsTheNewPasswordOnce() throws Exception {
        // Given: bruno_test chiede il link
        mockMvc.perform(post("/api/v1/auth/password-reset-requests").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"bruno_test@example.test\" }"))
                .andExpect(status().isAccepted());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(passwordResetNotifier).sendResetLink(eq("bruno_test@example.test"), eq("bruno_test"), link.capture());
        String token = link.getValue().substring(link.getValue().indexOf("token=") + "token=".length());
        String body = "{ \"token\": \"" + token + "\", \"newPassword\": \"password-ritrovata\" }";

        // When
        mockMvc.perform(post("/api/v1/auth/password-resets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        // Then: la nuova password funziona e il link non vale una seconda volta
        login("bruno_test", "password-ritrovata");
        mockMvc.perform(post("/api/v1/auth/password-resets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void adminArea_player_returns403() throws Exception {
        String token = accessToken(login("anna_test", TEST_PASSWORD));

        mockMvc.perform(get("/api/v1/admin/stats").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void login_adminFromEnvironment_hasAdminRole() throws Exception {
        // Given: l'admin creato all'avvio dalle proprietà dei test

        // When
        MvcResult result = login("admin_test", "password-admin-dei-test");

        // Then
        assertEquals("ADMIN", readJson(result).path("user").path("role").asString());
    }

    @Test
    void creditTransactions_afterLogin_listsTheDailyRewardFirst() throws Exception {
        // Given
        String token = accessToken(login("anna_test", TEST_PASSWORD));

        // When / Then
        mockMvc.perform(get("/api/v1/me/credit-transactions?page=0&size=10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("DAILY_REWARD"))
                .andExpect(jsonPath("$.items[0].balanceAfter").value(550))
                .andExpect(jsonPath("$.items[1].type").value("SIGNUP_BONUS"))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    private MvcResult login(String loginName, String password) throws Exception {
        String body = "{ \"login\": \"" + loginName + "\", \"password\": \"" + password + "\" }";
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String accessToken(MvcResult loginResult) throws Exception {
        return readJson(loginResult).path("accessToken").asString();
    }

    private tools.jackson.databind.JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
