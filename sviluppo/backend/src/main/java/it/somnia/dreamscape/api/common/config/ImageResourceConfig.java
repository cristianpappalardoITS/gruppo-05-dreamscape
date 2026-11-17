package it.somnia.dreamscape.api.common.config;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Serve le immagini ({@code /images/**}) con la cache per sempre, da due posti, in quest'ordine:
 * <ol>
 *   <li>la cartella di {@link StorageProperties} ({@code IMAGES_DIR}): le immagini caricate dal backoffice;</li>
 *   <li>{@code src/main/resources/images/}, dentro il jar: le copie WebP del catalogo ufficiale, create da
 *       {@code carte/compositore/crea_webp.py} (#165).</li>
 * </ol>
 *
 * <p>Ogni file ha la versione nel nome (es. {@code base-004-v1.webp}): se un'immagine cambia, cambia
 * anche il nome, quindi il browser la può tenere per un anno senza mai chiederla di nuovo
 * (guida per il frontend, capitolo 7). Un file che non c'è risponde {@code 404 NOT_FOUND}.
 */
@Configuration
@RequiredArgsConstructor
public class ImageResourceConfig implements WebMvcConfigurer {

    private static final Duration IMAGE_CACHE_DURATION = Duration.ofDays(365);

    // Le immagini del catalogo ufficiale, che arrivano con il backend
    private static final String CATALOG_IMAGES_LOCATION = "classpath:/images/";

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations(imagesLocation(), CATALOG_IMAGES_LOCATION)
                .setCacheControl(CacheControl.maxAge(IMAGE_CACHE_DURATION).cachePublic().immutable());
    }

    // Spring vuole la cartella come URL file: che finisce con /, anche se la cartella non esiste ancora
    private String imagesLocation() {
        String location = storageProperties.imagesDir().toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
