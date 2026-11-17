package it.somnia.dreamscape.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.common.config.StorageProperties;

// Caricamento delle immagini dal backoffice (POST /admin/images): originale conservato e copie WebP (decisione 007)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "dreamscape.admin.username=admin_test",
        "dreamscape.admin.email=admin_test@example.test",
        "dreamscape.admin.password=password-admin-dei-test" })
@AutoConfigureMockMvc
@Transactional
class AdminImageApiTest extends AdminApiTestSupport {

    @Autowired
    private StorageProperties storageProperties;

    private String admin;

    // Un codice diverso per ogni test: i file restano su disco e la versione dipende da quelli già scritti
    private String code;

    @BeforeEach
    void logIn() throws Exception {
        admin = bearer(ADMIN_USERNAME, ADMIN_PASSWORD);
        code = "PROVA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @AfterEach
    void deleteFiles() throws IOException {
        String name = code.toLowerCase();
        for (Path root : new Path[] { storageProperties.imagesDir(), storageProperties.originalsDir() }) {
            for (String folder : new String[] { "cards", "packs" }) {
                Path dir = root.resolve(folder);
                if (Files.isDirectory(dir)) {
                    try (Stream<Path> files = Files.list(dir)) {
                        for (Path file : files.filter(f -> f.getFileName().toString().startsWith(name)).toList()) {
                            Files.delete(file);
                        }
                    }
                }
            }
        }
    }

    @Test
    void upload_illustration_savesOriginalAndTwoWebpCopies() throws Exception {
        // When
        upload(png(1536, 1024), code, "ILLUSTRATION", admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/images/cards/" + code.toLowerCase() + "-v1.webp"))
                .andExpect(jsonPath("$.thumbUrl").value("/images/cards/" + code.toLowerCase() + "-v1-thumb.webp"));

        // Then
        String name = code.toLowerCase();
        BufferedImage large = ImageIO.read(storageProperties.imagesDir().resolve("cards/" + name + "-v1.webp").toFile());
        BufferedImage thumb = ImageIO.read(
                storageProperties.imagesDir().resolve("cards/" + name + "-v1-thumb.webp").toFile());
        assertThat(large.getWidth()).isEqualTo(1024);
        assertThat(thumb.getWidth()).isEqualTo(400);
        assertThat(storageProperties.originalsDir().resolve("cards/" + name + "-v1.png")).exists();
    }

    @Test
    void upload_sameCardTwice_createsVersionTwo() throws Exception {
        // Given
        upload(png(600, 400), code, "ILLUSTRATION", admin).andExpect(status().isCreated());

        // When / Then: il file pubblicato non cambia mai, così la cache del browser resta valida
        upload(png(600, 400), code, "ILLUSTRATION", admin)
                .andExpect(jsonPath("$.url").value("/images/cards/" + code.toLowerCase() + "-v2.webp"));
    }

    @Test
    void upload_fullArtWithFaCode_usesTheFullartName() throws Exception {
        upload(png(400, 600), code + "-FA", "FULL_ART", admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/images/cards/" + code.toLowerCase() + "-fullart-v1.webp"));
    }

    @Test
    void upload_cover_createsOneCopyInPacks() throws Exception {
        upload(png(800, 1200), code, "COVER", admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/images/packs/" + code.toLowerCase() + "-v1.webp"))
                .andExpect(jsonPath("$.thumbUrl").doesNotExist());
    }

    @Test
    void upload_notAnImage_returnsInvalidImage() throws Exception {
        MockMultipartFile text = new MockMultipartFile("file", "carta.png", "image/png", "non sono un PNG".getBytes());
        mockMvc.perform(multipart("/api/v1/admin/images").file(text).param("code", code).param("type", "ILLUSTRATION")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
    }

    @Test
    void upload_invalidCode_returnsValidationError() throws Exception {
        upload(png(100, 100), "../segreti", "ILLUSTRATION", admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
    }

    @Test
    void upload_withoutFile_returnsMalformedRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/images").param("code", code).param("type", "ILLUSTRATION")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void upload_player_returnsForbidden() throws Exception {
        upload(png(100, 100), code, "ILLUSTRATION", bearer(PLAYER_USERNAME, PLAYER_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions upload(byte[] content, String cardCode, String type, String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "illustrazione.png", "image/png", content);
        return mockMvc.perform(multipart("/api/v1/admin/images").file(file).param("code", cardCode)
                .param("type", type).header(HttpHeaders.AUTHORIZATION, token));
    }

    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(40, 30, 90));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }
}
