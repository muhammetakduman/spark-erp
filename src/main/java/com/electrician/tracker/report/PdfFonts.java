package com.electrician.tracker.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.pdf.BaseFont;

/**
 * Fonts for PDF documents. The built-in Helvetica only covers Latin-1, so
 * Turkish letters (ş, ğ, ı, İ) and the lira sign (₺) would be dropped; the
 * Windows Arial font is embedded instead, with Helvetica as a last resort
 * when Arial is not installed.
 */
final class PdfFonts {

    private static final String WINDOWS_DIR_ENV = "WINDIR";
    private static final String DEFAULT_WINDOWS_DIR = "C:\\Windows";
    private static final String REGULAR_FILE = "arial.ttf";
    private static final String BOLD_FILE = "arialbd.ttf";

    private static final BaseFont REGULAR = load(REGULAR_FILE);
    private static final BaseFont BOLD = load(BOLD_FILE);

    private PdfFonts() {
    }

    static Font regular(float size) {
        return REGULAR == null ? FontFactory.getFont(FontFactory.HELVETICA, size) : new Font(REGULAR, size);
    }

    static Font bold(float size) {
        return BOLD == null ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, size) : new Font(BOLD, size);
    }

    private static BaseFont load(String fileName) {
        String windowsDir = Objects.requireNonNullElse(System.getenv(WINDOWS_DIR_ENV), DEFAULT_WINDOWS_DIR);
        Path file = Path.of(windowsDir, "Fonts", fileName);
        if (!Files.isReadable(file)) {
            return null;
        }
        try {
            return BaseFont.createFont(file.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
