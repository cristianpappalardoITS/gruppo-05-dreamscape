package it.somnia.dreamscape.api.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

// La conversione in WebP senza Spring: un'immagine disegnata nel test fa la parte dell'illustrazione
class WebpImageWriterTest {

    private final WebpImageWriter writer = new WebpImageWriter();

    @Test
    void write_wideImage_resizesToMaxWidthKeepingProportions() throws IOException {
        // Given: un'illustrazione 3:2 da 1536 x 1024
        BufferedImage source = picture(1536, 1024, false);

        // When
        byte[] webp = writer.write(source, 400);

        // Then
        assertThat(isWebp(webp)).isTrue();
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(webp));
        assertThat(read.getWidth()).isEqualTo(400);
        assertThat(read.getHeight()).isEqualTo(267);
    }

    @Test
    void write_narrowImage_isNotEnlarged() throws IOException {
        // Given
        BufferedImage source = picture(300, 450, false);

        // When
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(writer.write(source, 1024)));

        // Then
        assertThat(read.getWidth()).isEqualTo(300);
        assertThat(read.getHeight()).isEqualTo(450);
    }

    @Test
    void write_transparentImage_writesWebp() {
        // Given: una cornice PNG con la finestra trasparente
        BufferedImage source = picture(800, 1200, true);

        // When
        byte[] webp = writer.write(source, 400);

        // Then
        assertThat(isWebp(webp)).isTrue();
    }

    // Il file WebP inizia con "RIFF", 4 byte di lunghezza e "WEBP"
    private static boolean isWebp(byte[] bytes) {
        return bytes.length > 12
                && new String(Arrays.copyOfRange(bytes, 0, 4), StandardCharsets.US_ASCII).equals("RIFF")
                && new String(Arrays.copyOfRange(bytes, 8, 12), StandardCharsets.US_ASCII).equals("WEBP");
    }

    private static BufferedImage picture(int width, int height, boolean transparent) {
        BufferedImage image = new BufferedImage(width, height,
                transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(60, 40, 120));
        graphics.fillRect(0, 0, width, height / 2);
        graphics.setColor(new Color(200, 160, 70, transparent ? 120 : 255));
        graphics.fillOval(width / 4, height / 4, width / 2, height / 2);
        graphics.dispose();
        return image;
    }
}
