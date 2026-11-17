package it.somnia.dreamscape.api.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.common.config.StorageProperties;

// Immagini su /images/**, senza accesso e con la cache per sempre (guida per il frontend, capitolo 7):
// quelle di IMAGES_DIR e quelle del catalogo dentro il backend
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ImageResourceTest {

    private static final String IMAGE_PATH = "cards/test-001-v1.webp";
    private static final byte[] IMAGE_BYTES = { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P' };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StorageProperties storageProperties;

    private Path image;

    @BeforeEach
    void writeImage() throws IOException {
        image = storageProperties.imagesDir().resolve(IMAGE_PATH);
        Files.createDirectories(image.getParent());
        Files.write(image, IMAGE_BYTES);
    }

    @AfterEach
    void deleteImage() throws IOException {
        Files.deleteIfExists(image);
    }

    @Test
    void getImage_existingFile_returnsItWithoutLogin() throws Exception {
        mockMvc.perform(get("/images/" + IMAGE_PATH))
                .andExpect(status().isOk())
                .andExpect(content().bytes(IMAGE_BYTES))
                .andExpect(content().contentType("image/webp"));
    }

    @Test
    void getImage_existingFile_isCachedForever() throws Exception {
        mockMvc.perform(get("/images/" + IMAGE_PATH))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=31536000")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("public")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("immutable")))
                .andExpect(header().doesNotExist(HttpHeaders.PRAGMA));
    }

    @Test
    void getImage_catalogCard_isServedFromTheBackendWithTheCache() throws Exception {
        // Given: le copie WebP del set base stanno in src/main/resources/images/cards (V102)

        // When / Then
        mockMvc.perform(get("/images/cards/base-004-fullart-v1-thumb.webp"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("immutable")));
    }

    @Test
    void getImage_missingFile_returnsNotFound() throws Exception {
        mockMvc.perform(get("/images/cards/non-esiste-v1.webp"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getImage_pathOutsideImagesDir_isRefused() throws Exception {
        // Given: un percorso che prova a uscire dalla cartella delle immagini

        // When / Then
        mockMvc.perform(get("/images/%2e%2e/application.properties"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/images/../application.properties"))
                .andExpect(status().is4xxClientError());
    }
}
