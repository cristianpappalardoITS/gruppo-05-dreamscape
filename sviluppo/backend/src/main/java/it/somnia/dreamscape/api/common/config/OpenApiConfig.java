package it.somnia.dreamscape.api.common.config;

import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import it.somnia.dreamscape.api.common.exception.ErrorResponse;

/**
 * La descrizione OpenAPI letta da Swagger ({@code /swagger-ui.html}): l'elenco definitivo delle API (guida SWD).
 *
 * <p>Dichiara il token di accesso JWT nell'intestazione {@code Authorization}, così compare il pulsante
 * <b>Authorize</b>; le API pubbliche lo tolgono con {@code @SecurityRequirements}. Ogni risposta di errore usa lo
 * stesso formato, {@link ErrorResponse}, e le API protette mostrano da sole il {@code 401} (e il {@code 403} quelle
 * del backoffice). Titoli, descrizioni e risposte di ogni endpoint stanno nei controller.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Dreamscape API", version = "v1", description = OpenApiConfig.DESCRIPTION),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
@SecurityScheme(
        name = OpenApiConfig.BEARER_SCHEME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Il campo accessToken della risposta di POST /api/v1/auth/login, senza la parola Bearer")
public class OpenApiConfig {

    // Il nome dello schema del token, usato da Swagger per collegare il pulsante alle API
    public static final String BEARER_SCHEME = "bearerAuth";

    static final String DESCRIPTION = """
            API REST del portale di **Dreamscape**, il gioco di carte collezionabili del gruppo Somnia.

            **Come provarle**
            1. `POST /api/v1/auth/login` con un utente di prova, es. \
            `{ "login": "anna_test", "password": "prova-dreamscape" }` (solo nel profilo `dev`).
            2. Copia `accessToken`, premi **Authorize** in alto a destra e incollalo, senza la parola Bearer.
            3. Da qui le API con il lucchetto rispondono; il token dura 15 minuti.

            Le API del backoffice (`/api/v1/admin/...`) vogliono un utente con `role: ADMIN`.

            **Regole comuni:** JSON con nomi in camelCase, id UUID, date ISO 8601 in UTC, liste lunghe a pagine \
            (`page` da 0 e `size`). Ogni errore ha la forma `{ status, code, message, fieldErrors }`: \
            `code` dice cosa è successo, `message` è già in italiano.

            La spiegazione pagina per pagina, con gli esempi in JavaScript, è in `docs/api/guida-frontend.md`.""";

    private static final String ERROR_SCHEMA = "ErrorResponse";
    private static final String ADMIN_PATH = "/api/v1/admin/";

    /**
     * Completa le risposte di errore: il formato {@link ErrorResponse} per tutte, {@code 401} sulle API protette e
     * {@code 403} sul backoffice, così non si ripetono in ogni controller.
     *
     * @return il customizer che springdoc applica alla descrizione OpenAPI
     */
    @Bean
    public OpenApiCustomizer errorResponsesCustomizer() {
        return openApi -> {
            Map<String, Schema> schemas = ModelConverters.getInstance().read(ErrorResponse.class);
            schemas.forEach(openApi.getComponents()::addSchemas);
            openApi.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
                boolean secured = operation.getSecurity() == null || !operation.getSecurity().isEmpty();
                if (secured) {
                    addIfMissing(operation, "401",
                            "UNAUTHORIZED: token mancante o scaduto. Si rinnova con /auth/refresh");
                }
                if (path.startsWith(ADMIN_PATH)) {
                    addIfMissing(operation, "403", "FORBIDDEN: serve un utente con role ADMIN");
                }
                // springdoc darebbe agli errori lo schema della risposta giusta: si mette quello dell'errore
                operation.getResponses().forEach((code, response) -> {
                    if (code.startsWith("4") || code.startsWith("5")) {
                        response.setContent(errorContent());
                    }
                });
            }));
        };
    }

    private static void addIfMissing(Operation operation, String code, String description) {
        ApiResponses responses = operation.getResponses();
        if (!responses.containsKey(code)) {
            responses.addApiResponse(code, new ApiResponse().description(description));
        }
    }

    private static Content errorContent() {
        return new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA)));
    }
}
