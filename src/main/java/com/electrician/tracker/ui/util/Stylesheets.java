package com.electrician.tracker.ui.util;

import javafx.scene.Scene;

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
        scene.getStylesheets().addAll(
                Stylesheets.class.getResource(THEME_CSS).toExternalForm(),
                Stylesheets.class.getResource(APP_CSS).toExternalForm());
    }
}
