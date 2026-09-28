package com.electrician.tracker.ui.util;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

/**
 * The application's stylesheets, applied to the main window and to every
 * dialog so all windows share the same look.
 */
public final class Stylesheets {

    private static final String THEME_CSS = "/css/theme.css";
    private static final String APP_CSS = "/css/app.css";

    private Stylesheets() {
    }

    public static void apply(Scene scene) {
        scene.getStylesheets().addAll(urls());
    }

    /** For {@link javafx.scene.control.Alert}s, whose scene is created by JavaFX. */
    public static void apply(DialogPane pane) {
        pane.getStylesheets().addAll(urls());
    }

    private static List<String> urls() {
        return List.of(
                Stylesheets.class.getResource(THEME_CSS).toExternalForm(),
                Stylesheets.class.getResource(APP_CSS).toExternalForm());
    }
}
