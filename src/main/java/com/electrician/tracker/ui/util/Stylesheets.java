package com.electrician.tracker.ui.util;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

/**
 * The application's stylesheets: one theme file (light or dark, the same
 * variable names with different values) plus app.css, which only uses those
 * variables. Applied to the main window and every dialog; switching the theme
 * swaps the theme file in every open window, without a restart.
 */
public final class Stylesheets {

    private static final String LIGHT_CSS = "/css/theme-light.css";
    private static final String DARK_CSS = "/css/theme-dark.css";
    private static final String APP_CSS = "/css/app.css";

    private static volatile boolean dark;

    private Stylesheets() {
    }

    public static void apply(Scene scene) {
        scene.getStylesheets().addAll(urls());
    }

    /** For panes whose scene is created by JavaFX. */
    public static void apply(DialogPane pane) {
        pane.getStylesheets().addAll(urls());
    }

    public static boolean isDark() {
        return dark;
    }

    /** Switches every open window (and the ones opened later) to the dark or light theme. */
    public static void useDark(boolean value) {
        if (dark == value) {
            return;
        }
        String old = themeUrl();
        dark = value;
        String current = themeUrl();
        for (Window window : List.copyOf(Window.getWindows())) {
            Scene scene = window.getScene();
            if (scene != null) {
                scene.getStylesheets().replaceAll(url -> url.equals(old) ? current : url);
            }
        }
    }

    private static List<String> urls() {
        return List.of(themeUrl(), url(APP_CSS));
    }

    private static String themeUrl() {
        return url(dark ? DARK_CSS : LIGHT_CSS);
    }

    private static String url(String path) {
        return Stylesheets.class.getResource(path).toExternalForm();
    }
}
