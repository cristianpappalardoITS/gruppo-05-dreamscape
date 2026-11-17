package it.somnia.dreamscape.api.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import it.somnia.dreamscape.api.TestcontainersConfiguration;

// Senza GOOGLE_CLIENT_ID (come nei test e in locale) l'accesso con Google è spento
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GoogleLoginDisabledApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void google_withoutClientId_returnsGoogleLoginDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"idToken\": \"qualsiasi\" }"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("GOOGLE_LOGIN_DISABLED"));
    }
}
