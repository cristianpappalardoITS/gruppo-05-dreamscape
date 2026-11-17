package it.somnia.dreamscape.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.common.security.RefreshCookies;
import it.somnia.dreamscape.api.user.service.GoogleTokenVerifier;
import it.somnia.dreamscape.api.user.service.GoogleTokenVerifier.GoogleIdentity;
import it.somnia.dreamscape.api.user.service.UserExceptions.InvalidGoogleTokenException;
import tools.jackson.databind.ObjectMapper;

// Accesso con Google (POST /auth/google, decisione 032). L'id token vero lo firma solo Google:
// qui la verifica è finta e restituisce l'account Google che serve al test
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "dreamscape.google.client-id=client-dei-test.apps.googleusercontent.com")
@AutoConfigureMockMvc
@Transactional
class GoogleAuthApiTest {

    private static final String ID_TOKEN = "id-token-di-google";
    private static final String NEW_EMAIL = "nuovo.giocatore@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    @Test
    void google_unknownAccountWithoutUsername_asksForSignupWithASuggestedUsername() throws Exception {
        // Given
        googleAccount("sub-nuovo", NEW_EMAIL, true);

        // When / Then
        loginWithGoogle("{ \"idToken\": \"" + ID_TOKEN + "\" }")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.signupRequired").value(true))
                .andExpect(jsonPath("$.email").value(NEW_EMAIL))
                .andExpect(jsonPath("$.suggestedUsername").value("nuovo_giocatore"))
                .andExpect(cookie().doesNotExist(RefreshCookies.NAME));
        assertEquals(0, countUsers(NEW_EMAIL));
    }

    @Test
    void google_suggestedUsernameAlreadyUsed_addsANumber() throws Exception {
        // Given: anna_test esiste già, ma con un'altra email
        googleAccount("sub-anna-gmail", "anna_test@gmail.com", true);

        // When / Then
        loginWithGoogle("{ \"idToken\": \"" + ID_TOKEN + "\" }")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.suggestedUsername").value("anna_test2"));
    }

    @Test
    void google_signupWithUsernameAndConsents_createsThePlayerAndLogsIn() throws Exception {
        // Given
        googleAccount("sub-nuovo", NEW_EMAIL, true);

        // When / Then: 500 DC di registrazione + 50 del primo accesso del giorno
        loginWithGoogle(signupBody("Nuovo_Giocatore", true, true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("Nuovo_Giocatore"))
                .andExpect(jsonPath("$.user.role").value("PLAYER"))
                .andExpect(jsonPath("$.user.credits").value(550))
                .andExpect(cookie().exists(RefreshCookies.NAME))
                .andExpect(cookie().httpOnly(RefreshCookies.NAME, true));
        assertEquals("sub-nuovo", jdbcTemplate.queryForObject(
                "SELECT google_subject FROM users WHERE email = ?", String.class, NEW_EMAIL));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?", String.class, NEW_EMAIL));
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT privacy_accepted_at FROM users WHERE email = ?", Object.class, NEW_EMAIL));
    }

    @Test
    void google_linkedAccount_logsInWithStatus200() throws Exception {
        // Given: la registrazione, poi un nuovo accesso con lo stesso account Google
        googleAccount("sub-nuovo", NEW_EMAIL, true);
        loginWithGoogle(signupBody("Nuovo_Giocatore", true, true)).andExpect(status().isCreated());

        // When / Then
        loginWithGoogle("{ \"idToken\": \"" + ID_TOKEN + "\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("Nuovo_Giocatore"))
                .andExpect(cookie().exists(RefreshCookies.NAME));
        assertEquals(1, countUsers(NEW_EMAIL));
    }

    @Test
    void google_signupWithoutConsents_returnsValidationErrors() throws Exception {
        // Given
        googleAccount("sub-nuovo", NEW_EMAIL, true);

        // When / Then
        loginWithGoogle(signupBody("Nuovo_Giocatore", true, false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("termsAccepted"));
        assertEquals(0, countUsers(NEW_EMAIL));
    }

    @Test
    void google_signupWithUsedUsername_returnsUsernameTaken() throws Exception {
        // Given
        googleAccount("sub-nuovo", NEW_EMAIL, true);

        // When / Then
        loginWithGoogle(signupBody("ANNA_TEST", true, true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
    }

    @Test
    void google_signupWithInvalidUsername_returnsValidationError() throws Exception {
        // Given
        googleAccount("sub-nuovo", NEW_EMAIL, true);

        // When / Then
        loginWithGoogle(signupBody("mario rossi!", true, true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("username"));
    }

    @Test
    void google_verifiedEmailOfExistingAccount_linksItAndLogsIn() throws Exception {
        // Given: anna_test si è registrata con la password; Google ha la stessa email, verificata
        googleAccount("sub-anna", "Anna_Test@example.test", true);

        // When / Then
        loginWithGoogle("{ \"idToken\": \"" + ID_TOKEN + "\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("anna_test"));
        assertEquals("sub-anna", jdbcTemplate.queryForObject(
                "SELECT google_subject FROM users WHERE username = 'anna_test'", String.class));
    }

    @Test
    void google_linkedAccount_canStillLogInWithThePassword() throws Exception {
        // Given
        googleAccount("sub-anna", "anna_test@example.test", true);
        loginWithGoogle("{ \"idToken\": \"" + ID_TOKEN + "\" }").andExpect(status().isOk());

        // When / Then
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"login\": \"anna_test\", \"password\": \"prova-dreamscape\" }"))
                .andExpect(status().isOk());
    }

    @Test
    void google_existingEmailNotVerified_isRefused() throws Exception {
        // Given
        googleAccount("sub-anna", "anna_test@example.test", false);

        // When / Then
        loginWithGoogle("{ \"idToken\": \"" + ID_TOKEN + "\" }")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GOOGLE_EMAIL_NOT_VERIFIED"));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT google_subject FROM users WHERE username = 'anna_test'", String.class));
    }

    @Test
    void google_accountLinkedToAnotherGoogleAccount_returnsConflict() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE users SET google_subject = 'sub-vecchio' WHERE username = 'anna_test'");
        googleAccount("sub-nuovo", "anna_test@example.test", true);

        // When / Then
        loginWithGoogle("{ \"idToken\": \"" + ID_TOKEN + "\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GOOGLE_ACCOUNT_CONFLICT"));
    }

    @Test
    void google_disabledAccount_returnsAccountDisabled() throws Exception {
        // Given
        jdbcTemplate.update("UPDATE users SET google_subject = 'sub-anna', is_enabled = FALSE "
                + "WHERE username = 'anna_test'");
        googleAccount("sub-anna", "anna_test@example.test", true);

        // When / Then
        loginWithGoogle("{ \"idToken\": \"" + ID_TOKEN + "\" }")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void google_invalidToken_returnsInvalidGoogleToken() throws Exception {
        // Given
        when(googleTokenVerifier.verify(anyString())).thenThrow(new InvalidGoogleTokenException());

        // When / Then
        loginWithGoogle("{ \"idToken\": \"token-falso\" }")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_GOOGLE_TOKEN"));
    }

    @Test
    void google_withoutIdToken_returnsValidationError() throws Exception {
        loginWithGoogle("{ }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("idToken"));
    }

    @Test
    void googleOnlyAccount_cannotLogInWithAPassword() throws Exception {
        // Given
        googleAccount("sub-nuovo", NEW_EMAIL, true);
        loginWithGoogle(signupBody("Nuovo_Giocatore", true, true)).andExpect(status().isCreated());

        // When / Then
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"login\": \"" + NEW_EMAIL + "\", \"password\": \"qualsiasi-password\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void googleOnlyAccount_changePassword_saysThereIsNoPassword() throws Exception {
        // Given
        String bearer = signUpWithGoogle();

        // When / Then
        mockMvc.perform(put("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"currentPassword\": \"qualsiasi\", \"newPassword\": \"nuova-password-1\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_NOT_SET"));
    }

    @Test
    void googleOnlyAccount_deleteAccount_saysThereIsNoPassword() throws Exception {
        // Given
        String bearer = signUpWithGoogle();

        // When / Then
        mockMvc.perform(delete("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"qualsiasi\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_NOT_SET"));
    }

    @Test
    void deleteAccount_linkedToGoogle_unlinksIt() throws Exception {
        // Given: anna_test collegata a Google, poi cancella l'account con la sua password
        googleAccount("sub-anna", "anna_test@example.test", true);
        String bearer = "Bearer " + objectMapper.readTree(loginWithGoogle("{ \"idToken\": \"" + ID_TOKEN + "\" }")
                .andReturn().getResponse().getContentAsString()).path("accessToken").asString();

        // When
        mockMvc.perform(delete("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"prova-dreamscape\" }"))
                .andExpect(status().isNoContent());

        // Then: con lo stesso account Google si potrà registrare un account nuovo
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE google_subject = 'sub-anna'", Integer.class));
    }

    private ResultActions loginWithGoogle(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String signUpWithGoogle() throws Exception {
        googleAccount("sub-nuovo", NEW_EMAIL, true);
        String response = loginWithGoogle(signupBody("Nuovo_Giocatore", true, true))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(response).path("accessToken").asString();
    }

    private void googleAccount(String subject, String email, boolean emailVerified) {
        when(googleTokenVerifier.verify(anyString())).thenReturn(new GoogleIdentity(subject, email, emailVerified));
    }

    private static String signupBody(String username, boolean privacyAccepted, boolean termsAccepted) {
        return """
                { "idToken": "%s", "username": "%s", "privacyAccepted": %s, "termsAccepted": %s }"""
                .formatted(ID_TOKEN, username, privacyAccepted, termsAccepted);
    }

    private int countUsers(String email) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE lower(email) = lower(?)", Integer.class,
                email);
    }
}
