package it.somnia.dreamscape.api.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.stereotype.Component;

import it.somnia.dreamscape.api.common.config.StorageProperties;
import lombok.RequiredArgsConstructor;

/**
 * Salva i file nelle cartelle di {@link StorageProperties} ({@code IMAGES_DIR} e {@code ORIGINALS_DIR}).
 */
@Component
@RequiredArgsConstructor
public class LocalFileStorage implements FileStorage {

    private final StorageProperties storageProperties;

    @Override
    public void saveImage(String path, byte[] content) {
        write(storageProperties.imagesDir(), path, content);
    }

    // CREATE_NEW: il sistema operativo crea il file solo se non c'è, senza spazio tra controllo e scrittura
    @Override
    public boolean createImage(String path, byte[] content) {
        Path target = resolve(storageProperties.imagesDir(), path);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException("Salvataggio del file " + path + " non riuscito", e);
        }
    }

    @Override
    public boolean imageExists(String path) {
        return Files.exists(resolve(storageProperties.imagesDir(), path));
    }

    @Override
    public void saveOriginal(String path, byte[] content) {
        write(storageProperties.originalsDir(), path, content);
    }

    private static void write(Path root, String path, byte[] content) {
        Path target = resolve(root, path);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Salvataggio del file " + path + " non riuscito", e);
        }
    }

    // Il percorso deve restare dentro la cartella: niente ".." o percorsi assoluti
    private static Path resolve(Path root, String path) {
        Path base = root.toAbsolutePath().normalize();
        Path target = base.resolve(path).normalize();
        if (!target.startsWith(base) || target.equals(base)) {
            throw new IllegalArgumentException("Percorso non valido: " + path);
        }
        return target;
    }
}
