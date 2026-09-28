package com.electrician.tracker.ui.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Gives every window of the application — main window, login, dialogs and
 * alerts — the electric panel icon (task bar, Alt+Tab) and the application's
 * stylesheets, by watching the list of open windows once at start-up.
 */
public final class WindowDecorations {

    private static final List<String> ICON_RESOURCES = List.of("/ikon/ikon-256.png", "/ikon/ikon-64.png",
            "/ikon/ikon-32.png");
    private static final String LARGE_ICON = "/ikon/ikon-256.png";
    private static List<Image> icons;

    private WindowDecorations() {
    }

    public static void install() {
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(WindowDecorations::decorate);
            }
        });
    }

    /** The application icon at its largest size (login band, about dialog). */
    public static Image largeIcon() {
        return load(LARGE_ICON);
    }

    public static void applyIcons(Stage stage) {
        if (stage.getIcons().isEmpty()) {
            stage.getIcons().addAll(icons());
        }
    }

    private static void decorate(Window window) {
        if (window instanceof Stage stage) {
            applyIcons(stage);
        }
        if (window.getScene() != null && window.getScene().getRoot() instanceof DialogPane pane
                && pane.getStylesheets().isEmpty()) {
            Stylesheets.apply(pane);
        }
    }

    private static synchronized List<Image> icons() {
        if (icons == null) {
            List<Image> loaded = new ArrayList<>();
            ICON_RESOURCES.forEach(path -> loaded.add(load(path)));
            icons = List.copyOf(loaded);
        }
        return icons;
    }

    private static Image load(String path) {
        InputStream stream = WindowDecorations.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing icon resource: " + path);
        }
        return new Image(stream);
    }
}
