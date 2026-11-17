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
    // Limiti dell'immagine caricata: bastano per un'illustrazione stampabile, e la decodifica resta sotto i 200 MB
    static final int MAX_SIDE = 8000;
    static final long MAX_PIXELS = 40_000_000L;

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
     * @throws InvalidImageException       se il file non è un PNG o un JPEG leggibile, o se ha troppi pixel
     * @throws InvalidAdminFieldException  se il codice non è valido
     */
    public ImageUploadResponse upload(byte[] content, String code, ImageType type) {
        if (code == null || !CODE.matcher(code.strip()).matches()) {
            throw new InvalidAdminFieldException("code", "Il codice può avere solo lettere, numeri e trattini.");
        }
        DecodedImage image = decode(content);
        byte[] large = webpImageWriter.write(image.pixels(), LARGE_WIDTH);
        byte[] thumb = type == ImageType.COVER ? null : webpImageWriter.write(image.pixels(), THUMB_WIDTH);

        // La copia grande "prenota" la versione: se un altro caricamento l'ha appena presa, si passa alla successiva
        String folder = type == ImageType.COVER ? "packs" : "cards";
        String name = fileName(code.strip(), type);
        int version = nextVersion(folder, name, 1);
        while (!fileStorage.createImage(folder + "/" + name + "-v" + version + ".webp", large)) {
            version = nextVersion(folder, name, version + 1);
        }
        String base = folder + "/" + name + "-v" + version;

        fileStorage.saveOriginal(base + "." + EXTENSIONS.get(image.format()), content);
        if (thumb == null) {
            return new ImageUploadResponse("/images/" + base + ".webp", null);
        }
        fileStorage.saveImage(base + "-thumb.webp", thumb);
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
    private int nextVersion(String folder, String name, int from) {
        int version = from;
        while (versionTaken(folder + "/" + name + "-v" + version + ".webp")) {
            version++;
        }
        return version;
    }

    private boolean versionTaken(String path) {
        return fileStorage.imageExists(path) || new ClassPathResource(BUNDLED_IMAGES + path).exists();
    }

    // Una sola lettura del file: il formato si prende dal contenuto, non dal nome ("png" o "jpeg"), e le
    // dimensioni si controllano prima di decodificare. Un PNG di pochi KB può dichiarare 50.000 x 50.000 pixel:
    // decodificarlo vorrebbe dire chiedere gigabyte di memoria
    private static DecodedImage decode(byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidImageException();
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = input != null ? ImageIO.getImageReaders(input) : null;
            while (readers != null && readers.hasNext()) {
                ImageReader reader = readers.next();
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!EXTENSIONS.containsKey(format)) {
                    continue;
                }
                try {
                    reader.setInput(input, true, true);
                    checkSize(reader.getWidth(0), reader.getHeight(0));
                    return new DecodedImage(format, reader.read(0));
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException e) {
            throw new InvalidImageException();
        }
        throw new InvalidImageException();
    }

    private static void checkSize(int width, int height) {
        if (width > MAX_SIDE || height > MAX_SIDE || (long) width * height > MAX_PIXELS) {
            throw new InvalidImageException("L'immagine è troppo grande: al massimo " + MAX_SIDE
                    + " pixel per lato e " + MAX_PIXELS / 1_000_000 + " milioni di pixel in tutto.");
        }
    }

    // Formato ("png" o "jpeg") e pixel dell'immagine caricata
    private record DecodedImage(String format, BufferedImage pixels) {
    }
}
