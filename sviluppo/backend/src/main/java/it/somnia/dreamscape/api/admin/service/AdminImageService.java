package it.somnia.dreamscape.api.admin.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import it.somnia.dreamscape.api.admin.dto.response.ImageUploadResponse;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.InvalidAdminFieldException;
import it.somnia.dreamscape.api.admin.service.AdminExceptions.InvalidImageException;
import it.somnia.dreamscape.api.common.storage.FileStorage;
import lombok.RequiredArgsConstructor;

/**
 * Carica le immagini del backoffice ({@code POST /admin/images}, decisione 007).
 *
 * <p>Conserva intatto il file originale e crea le copie WebP servite su {@code /images/}: grande (1024 px) e
 * miniatura (400 px) per le carte, una sola copia per i pacchetti. Ogni caricamento ha un numero di versione nel
 * nome ({@code -v1}, {@code -v2}...): un file pubblicato non cambia mai, così la cache del browser resta valida.
 * Le copie non si collegano da sole alla carta o al pacchetto: gli indirizzi si salvano poi con la modifica.
 */
@Service
@RequiredArgsConstructor
public class AdminImageService {

    static final int LARGE_WIDTH = 1024;
    static final int THUMB_WIDTH = 400;

    // Codice della carta o nome del pacchetto: lettere, numeri e trattini, come BASE-004 o dream-pack
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9-]{0,39}$");
    private static final String FULL_ART_SUFFIX = "-fa";
    private static final Map<String, String> EXTENSIONS = Map.of("png", "png", "jpeg", "jpg");
    // Le immagini servite anche da dentro il jar (ImageResourceConfig)
    private static final String BUNDLED_IMAGES = "images/";

    private final FileStorage fileStorage;
    private final WebpImageWriter webpImageWriter;

    /**
     * Salva l'originale e crea le copie WebP con il primo numero di versione libero.
     *
     * @param content i byte del file caricato (PNG o JPEG)
     * @param code    il codice della carta (es. {@code BASE-004}; per la full art va bene anche {@code BASE-004-FA})
     *                oppure il nome del pacchetto per le copertine (es. {@code dream-pack})
     * @param type    illustrazione, full art o copertina
     * @return gli indirizzi delle copie, da salvare nella carta o nel pacchetto
     * @throws InvalidImageException       se il file non è un PNG o un JPEG leggibile
     * @throws InvalidAdminFieldException  se il codice non è valido
     */
    public ImageUploadResponse upload(byte[] content, String code, ImageType type) {
        if (code == null || !CODE.matcher(code.strip()).matches()) {
            throw new InvalidAdminFieldException("code", "Il codice può avere solo lettere, numeri e trattini.");
        }
        String format = imageFormat(content);
        BufferedImage image = read(content);

        String folder = type == ImageType.COVER ? "packs" : "cards";
        String name = fileName(code.strip(), type);
        int version = nextVersion(folder, name);
        String base = folder + "/" + name + "-v" + version;

        fileStorage.saveOriginal(base + "." + EXTENSIONS.get(format), content);
        fileStorage.saveImage(base + ".webp", webpImageWriter.write(image, LARGE_WIDTH));
        if (type == ImageType.COVER) {
            return new ImageUploadResponse("/images/" + base + ".webp", null);
        }
        fileStorage.saveImage(base + "-thumb.webp", webpImageWriter.write(image, THUMB_WIDTH));
        return new ImageUploadResponse("/images/" + base + ".webp", "/images/" + base + "-thumb.webp");
    }

    // Come i file del catalogo: base-004, base-004-fullart, dream-pack
    private static String fileName(String code, ImageType type) {
        String name = code.toLowerCase(Locale.ROOT);
        if (type == ImageType.FULL_ART) {
            if (name.endsWith(FULL_ART_SUFFIX)) {
                name = name.substring(0, name.length() - FULL_ART_SUFFIX.length());
            }
            return name + "-fullart";
        }
        return name;
    }

    // Una versione è occupata se il file c'è in IMAGES_DIR o tra le immagini che arrivano con il backend
    // (classpath images/, il catalogo ufficiale della #165): stesso indirizzo vorrebbe dire coprire quella vecchia
    private int nextVersion(String folder, String name) {
        int version = 1;
        while (versionTaken(folder + "/" + name + "-v" + version + ".webp")) {
            version++;
        }
        return version;
    }

    private boolean versionTaken(String path) {
        return fileStorage.imageExists(path) || new ClassPathResource(BUNDLED_IMAGES + path).exists();
    }

    // Il formato si legge dal contenuto, non dal nome del file: "png" o "jpeg"
    private static String imageFormat(byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidImageException();
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            while (readers.hasNext()) {
                String format = readers.next().getFormatName().toLowerCase(Locale.ROOT);
                if (EXTENSIONS.containsKey(format)) {
                    return format;
                }
            }
        } catch (IOException e) {
            throw new InvalidImageException();
        }
        throw new InvalidImageException();
    }

    private static BufferedImage read(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw new InvalidImageException();
            }
            return image;
        } catch (IOException e) {
            throw new InvalidImageException();
        }
    }
}
