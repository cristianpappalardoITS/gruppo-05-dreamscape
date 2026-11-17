package it.somnia.dreamscape.api.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import it.somnia.dreamscape.api.common.config.StorageProperties;

// Salvataggio dei file su disco, senza Spring: cartelle temporanee al posto di IMAGES_DIR e ORIGINALS_DIR
class LocalFileStorageTest {

    @TempDir
    private Path root;

    @Test
    void createImage_freePath_createsTheFile() throws Exception {
        // Given
        LocalFileStorage storage = storage();

        // When
        boolean created = storage.createImage("cards/base-001-v1.webp", new byte[] { 1, 2, 3 });

        // Then
        assertThat(created).isTrue();
        assertThat(Files.readAllBytes(root.resolve("images/cards/base-001-v1.webp"))).containsExactly(1, 2, 3);
    }

    @Test
    void createImage_pathAlreadyTaken_leavesTheFirstFileUntouched() throws Exception {
        // Given: un altro caricamento ha appena preso la v1
        LocalFileStorage storage = storage();
        storage.createImage("cards/base-001-v1.webp", new byte[] { 1 });

        // When
        boolean created = storage.createImage("cards/base-001-v1.webp", new byte[] { 2 });

        // Then
        assertThat(created).isFalse();
        assertThat(Files.readAllBytes(root.resolve("images/cards/base-001-v1.webp"))).containsExactly(1);
    }

    @Test
    void createImage_pathOutsideTheFolder_isRefused() {
        assertThatThrownBy(() -> storage().createImage("../fuori.webp", new byte[] { 1 }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private LocalFileStorage storage() {
        return new LocalFileStorage(new StorageProperties(root.resolve("images"), root.resolve("originals")));
    }
}
