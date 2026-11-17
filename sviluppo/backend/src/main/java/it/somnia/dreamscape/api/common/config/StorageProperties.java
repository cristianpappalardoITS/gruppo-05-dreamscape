package it.somnia.dreamscape.api.common.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dove stanno i file del backend (application.properties, prefisso dreamscape.storage).
 *
 * @param imagesDir    cartella delle immagini servite su {@code /images/**}: illustrazioni e copertine in WebP.
 *                     Arriva dalla variabile d'ambiente {@code IMAGES_DIR}
 * @param originalsDir cartella dei PNG e JPEG originali caricati dal backoffice, conservati intatti e mai serviti
 *                     al pubblico (decisione 007). Arriva da {@code ORIGINALS_DIR}
 */
@ConfigurationProperties(prefix = "dreamscape.storage")
public record StorageProperties(Path imagesDir, Path originalsDir) {
}
