package com.electrician.tracker.ui.util;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Creates the vector icons (Font Awesome through Ikonli). Size and colour
 * come from style classes only, so they follow the theme and stay sharp at
 * any screen scaling.
 */
public final class Icons {

    private static final String ICON_CLASS = "app-icon";

    private Icons() {
    }

    public static FontIcon of(AppIcon icon, IconSize size) {
        FontIcon fontIcon = new FontIcon(icon.ikon());
        fontIcon.getStyleClass().addAll(ICON_CLASS, size.styleClass());
        if (icon.tone() != null) {
            fontIcon.getStyleClass().add(icon.tone());
        }
        return fontIcon;
    }

    /** Puts a button-sized icon in front of the button's text. */
    public static <T extends ButtonBase> T decorate(T button, AppIcon icon) {
        button.setGraphic(of(icon, IconSize.BUTTON));
        button.setContentDisplay(ContentDisplay.LEFT);
        return button;
    }

    public static MenuItem decorate(MenuItem item, AppIcon icon) {
        item.setGraphic(of(icon, IconSize.BUTTON));
        return item;
    }

    /** A button with icon and translated text. */
    public static Button button(AppIcon icon, String messageKey, Runnable action) {
        Button button = decorate(new Button(DialogUtil.message(messageKey)), icon);
        button.setOnAction(event -> action.run());
        return button;
    }

    /** A square button with only an icon; the text becomes its tooltip. */
    public static Button iconButton(AppIcon icon, String tooltipKey, Runnable action) {
        Button button = new Button();
        button.setGraphic(of(icon, IconSize.BUTTON));
        button.getStyleClass().add("icon-only-button");
        button.setTooltip(new javafx.scene.control.Tooltip(DialogUtil.message(tooltipKey)));
        button.setOnAction(event -> action.run());
        return button;
    }
}
