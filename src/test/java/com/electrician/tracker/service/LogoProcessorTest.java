package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class LogoProcessorTest {

    private final LogoProcessor processor = new LogoProcessor();

    @Test
    void largeImageIsShrunkTo400PixelsKeepingProportions() throws IOException {
        byte[] png = processor.prepare(png(2000, 1000), "logo.png");

        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(stored.getWidth()).isEqualTo(LogoProcessor.MAX_EDGE);
        assertThat(stored.getHeight()).isEqualTo(200);
    }

    @Test
    void smallImageKeepsItsSize() throws IOException {
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(processor.prepare(png(120, 60), "a.PNG")));

        assertThat(stored.getWidth()).isEqualTo(120);
    }

    @Test
    void svgIsRasterized() throws IOException {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='800' height='200'>"
                + "<rect width='800' height='200' fill='#1F4E79'/></svg>";

        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(
                processor.prepare(svg.getBytes(StandardCharsets.UTF_8), "logo.svg")));

        assertThat(stored.getWidth()).isEqualTo(LogoProcessor.MAX_EDGE);
        assertThat(stored.getHeight()).isEqualTo(100);
    }

    @Test
    void otherFileTypesAreRefused() {
        assertThat(processor.isAccepted("logo.gif")).isFalse();
        assertThatThrownBy(() -> processor.prepare(new byte[] { 1 }, "logo.bmp"))
                .hasMessage("error.company.logo.type");
    }

    private static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }
}
