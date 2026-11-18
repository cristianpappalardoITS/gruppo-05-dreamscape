package it.somnia.dreamscape.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// La descrizione OpenAPI letta da Swagger: senza lo schema del token il pulsante Authorize non compare,
// e ogni endpoint ha un titolo, un gruppo in italiano e gli errori nel formato unico
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    private static final Set<String> TAGS = Set.of("Accesso", "Profilo", "Benvenuto", "Catalogo", "Negozio",
            "Collezione", "Mazzi", "Amici", "Scambi", "Sfide", "Partite", "Backoffice: carte", "Backoffice: immagini",
            "Backoffice: pacchetti");
    private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete");
    private static final String ERROR_REF = "#/components/schemas/ErrorResponse";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void apiDocs_withoutLogin_declareTheBearerTokenForEveryApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Dreamscape API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray());
    }

    @Test
    void apiDocs_everyOperation_hasTitleItalianTagAndErrorFormat() throws Exception {
        // Given
        JsonNode paths = readApiDocs().path("paths");
        List<String> problems = new ArrayList<>();

        // When
        for (Map.Entry<String, JsonNode> path : paths.properties()) {
            for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
                if (!METHODS.contains(operation.getKey())) {
                    continue;
                }
                String name = operation.getKey().toUpperCase() + " " + path.getKey();
                JsonNode details = operation.getValue();
                if (details.path("summary").asString().isBlank()) {
                    problems.add(name + ": manca il titolo");
                }
                JsonNode tags = details.path("tags");
                if (tags.size() != 1 || !TAGS.contains(tags.get(0).asString())) {
                    problems.add(name + ": gruppo " + tags);
                }
                for (Map.Entry<String, JsonNode> response : details.path("responses").properties()) {
                    String ref = response.getValue().path("content").path("application/json").path("schema")
                            .path("$ref").asString();
                    if (response.getKey().matches("[45]\\d\\d") && !ERROR_REF.equals(ref)) {
                        problems.add(name + ": l'errore " + response.getKey() + " non usa ErrorResponse");
                    }
                }
            }
        }

        // Then
        assertThat(paths.size()).isGreaterThan(25);
        assertThat(problems).isEmpty();
    }

    @Test
    void apiDocs_publicAndProtectedApis_showTheRightLockAndErrors() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                // Pubbliche: niente lucchetto e niente 401 aggiunto
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['401']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/cards'].get.security").isEmpty())
                // Protette: il token globale e il 401
                .andExpect(jsonPath("$.paths['/api/v1/decks'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/decks'].get.responses['401']").exists())
                // Backoffice: anche il 403
                .andExpect(jsonPath("$.paths['/api/v1/admin/cards'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/decks'].get.responses['403']").doesNotExist())
                // Risposte di successo giuste: 201 per le creazioni, 202 per Google
                .andExpect(jsonPath("$.paths['/api/v1/decks'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/decks'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/auth/google'].post.responses['202']").exists());
    }

    @Test
    void swaggerUi_withoutLogin_isReachable() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    private JsonNode readApiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
