package it.somnia.dreamscape.api.common.config;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Serve le immagini ({@code /images/**}) dalla cartella di {@link StorageProperties}, con la cache per sempre.
 *
 * <p>Ogni file ha la versione nel nome (es. {@code base-004-v1.webp}): se un'immagine cambia, cambia
 * anche il nome, quindi il browser la può tenere per un anno senza mai chiederla di nuovo
 * (guida per il frontend, capitolo 7). Un file che non c'è risponde {@code 404 NOT_FOUND}.
 */
@Configuration
@RequiredArgsConstructor
public class ImageResourceConfig implements WebMvcConfigurer {

    private static final Duration IMAGE_CACHE_DURATION = Duration.ofDays(365);

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations(imagesLocation())
                .setCacheControl(CacheControl.maxAge(IMAGE_CACHE_DURATION).cachePublic().immutable());
    }

    // Spring vuole la cartella come URL file: che finisce con /, anche se la cartella non esiste ancora
    private String imagesLocation() {
        String location = storageProperties.imagesDir().toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
