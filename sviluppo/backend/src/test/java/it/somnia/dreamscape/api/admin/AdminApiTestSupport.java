package it.somnia.dreamscape.api.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

// Accesso dei test del backoffice: l'admin lo crea AdminInitializer all'avvio, con le proprietà dei test
abstract class AdminApiTestSupport {

    // Stesse proprietà di AuthApiTest: così il contesto di Spring si riusa e il database parte una volta sola
    static final String ADMIN_USERNAME = "admin_test";
    static final String ADMIN_PASSWORD = "password-admin-dei-test";
    static final String PLAYER_USERNAME = "anna_test";
    static final String PLAYER_PASSWORD = "prova-dreamscape";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    String bearer(String login, String password) throws Exception {
        String body = "{ \"login\": \"" + login + "\", \"password\": \"" + password + "\" }";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(response).path("accessToken").asString();
    }
}
