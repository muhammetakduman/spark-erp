package com.electrician.tracker.ui.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

/**
 * Whether a logo is likely hard to see on the dark theme's background: it has
 * no transparent area (a light box would sit on the dark screen) or its
 * visible pixels are mostly dark. Only a rough, cheap check on a grid of
 * sample points.
 */
public final class LogoContrast {

    private static final int SAMPLES_PER_SIDE = 40;
    private static final double TRANSPARENT_BELOW = 0.1;
    private static final double DARK_BELOW = 0.35;
    private static final double RED_WEIGHT = 0.2126;
    private static final double GREEN_WEIGHT = 0.7152;
    private static final double BLUE_WEIGHT = 0.0722;

    private LogoContrast() {
    }

    public static boolean isPoorOnDark(Image image) {
        PixelReader reader = image == null ? null : image.getPixelReader();
        if (reader == null || image.getWidth() < 1 || image.getHeight() < 1) {
            return false;
        }
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int transparent = 0;
        int visible = 0;
        double luminance = 0;
        for (int i = 0; i < SAMPLES_PER_SIDE; i++) {
            for (int j = 0; j < SAMPLES_PER_SIDE; j++) {
                javafx.scene.paint.Color color = reader.getColor(i * (width - 1) / (SAMPLES_PER_SIDE - 1),
                        j * (height - 1) / (SAMPLES_PER_SIDE - 1));
                if (color.getOpacity() < TRANSPARENT_BELOW) {
                    transparent++;
                } else {
                    visible++;
                    luminance += RED_WEIGHT * color.getRed() + GREEN_WEIGHT * color.getGreen()
                            + BLUE_WEIGHT * color.getBlue();
                }
            }
        }
        boolean opaqueBox = transparent == 0;
        boolean mostlyDark = visible > 0 && luminance / visible < DARK_BELOW;
        return opaqueBox || mostlyDark;
    }
}
