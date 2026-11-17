package it.somnia.dreamscape.api.common.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Dice a Swagger che le API usano il token di accesso JWT nell'intestazione {@code Authorization}.
 *
 * <p>Così in {@code /swagger-ui.html} compare il pulsante <b>Authorize</b>: si incolla l'{@code accessToken}
 * di {@code POST /auth/login} e Swagger lo manda a ogni chiamata. Le API pubbliche (catalogo, accesso)
 * funzionano anche senza. L'elenco completo e i testi di Swagger arrivano con la #99.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Dreamscape API", version = "v1"),
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
}
