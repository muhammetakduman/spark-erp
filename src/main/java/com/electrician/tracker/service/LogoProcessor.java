package com.electrician.tracker.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;

import com.electrician.tracker.service.exception.ValidationException;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.stereotype.Component;

/**
 * Turns an uploaded logo (PNG, JPG or SVG) into a PNG whose longest edge is
 * at most {@link #MAX_EDGE} pixels, keeping its proportions and transparency.
 * Only this small copy is stored, never the original file.
 */
@Component
public class LogoProcessor {

    public static final int MAX_EDGE = 400;
    public static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;
    private static final Set<String> RASTER_EXTENSIONS = Set.of("png", "jpg", "jpeg");
    private static final String SVG_EXTENSION = "svg";
    private static final String PNG_FORMAT = "png";

    /** PNG, JPG or SVG, judged by the file name. */
    public boolean isAccepted(String fileName) {
        String extension = extension(fileName);
        return RASTER_EXTENSIONS.contains(extension) || SVG_EXTENSION.equals(extension);
    }

    public byte[] prepare(byte[] content, String fileName) {
        if (!isAccepted(fileName)) {
            throw new ValidationException("error.company.logo.type");
        }
        if (content == null || content.length == 0) {
            throw new ValidationException("error.company.logo.unreadable");
        }
        if (content.length > MAX_UPLOAD_BYTES) {
            throw new ValidationException("error.company.logo.tooLarge");
        }
        BufferedImage image = SVG_EXTENSION.equals(extension(fileName)) ? rasterizeSvg(content) : readRaster(content);
        return writePng(shrink(image));
    }

    /** The image scaled down so its longest edge is {@link #MAX_EDGE}; smaller images stay as they are. */
    static BufferedImage shrink(BufferedImage image) {
        int longest = Math.max(image.getWidth(), image.getHeight());
        if (longest <= MAX_EDGE) {
            return image;
        }
        double scale = (double) MAX_EDGE / longest;
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private static BufferedImage readRaster(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw new ValidationException("error.company.logo.unreadable");
            }
            return image;
        } catch (IOException e) {
            throw new ValidationException("error.company.logo.unreadable");
        }
    }

    private static BufferedImage rasterizeSvg(byte[] content) {
        PNGTranscoder transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(PNGTranscoder.KEY_MAX_WIDTH, (float) MAX_EDGE);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_MAX_HEIGHT, (float) MAX_EDGE);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            transcoder.transcode(new TranscoderInput(new ByteArrayInputStream(content)), new TranscoderOutput(png));
        } catch (TranscoderException | RuntimeException e) {
            throw new ValidationException("error.company.logo.unreadable");
        }
        return readRaster(png.toByteArray());
    }

    private static byte[] writePng(BufferedImage image) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, PNG_FORMAT, out);
        } catch (IOException e) {
            throw new ValidationException("error.company.logo.unreadable");
        }
        return out.toByteArray();
    }

    private static String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
