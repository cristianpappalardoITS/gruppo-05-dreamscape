package it.somnia.dreamscape.api.admin.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Component;

/**
 * Ridimensiona un'immagine e la scrive in WebP (decisione 007: copia grande da 1024 px e miniatura da 400 px).
 *
 * <p>Usa il writer WebP che la libreria {@code webp-imageio} registra in {@link ImageIO}. Le immagini più
 * strette della larghezza chiesta non si allargano: si scrivono così come sono.
 */
@Component
public class WebpImageWriter {

    // Qualità della compressione con perdita: abbastanza alta per le illustrazioni, file leggeri
    private static final float QUALITY = 0.85f;

    /**
     * Restituisce l'immagine in WebP, larga al massimo {@code maxWidth} pixel, con le proporzioni originali.
     *
     * @param source   l'immagine letta dal file caricato
     * @param maxWidth la larghezza massima, es. 1024 o 400
     * @return i byte del file WebP
     * @throws IllegalStateException se nel sistema non c'è il writer WebP
     */
    public byte[] write(BufferedImage source, int maxWidth) {
        BufferedImage image = resize(source, maxWidth);
        ImageWriter writer = webpWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType(param.getCompressionTypes()[0]);
            param.setCompressionQuality(QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new UncheckedIOException("Conversione in WebP non riuscita", e);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    // Ridimensiona con l'interpolazione bicubica; i PNG con trasparenza restano trasparenti
    private static BufferedImage resize(BufferedImage source, int maxWidth) {
        int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        int width = Math.min(maxWidth, source.getWidth());
        int height = Math.max(1, Math.round(source.getHeight() * (width / (float) source.getWidth())));
        BufferedImage resized = new BufferedImage(width, height, type);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private static ImageWriter webpWriter() {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new IllegalStateException("Manca il writer WebP: controllare la dipendenza webp-imageio");
        }
        return writers.next();
    }
}
