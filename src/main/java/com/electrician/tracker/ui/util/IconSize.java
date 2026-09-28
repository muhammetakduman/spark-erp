package com.electrician.tracker.ui.util;

/**
 * The four icon sizes of the application; the pixel sizes are set in
 * app.css through these style classes (menu 18, button 16, dialog title 24,
 * empty screen 48).
 */
public enum IconSize {
    MENU("icon-menu"),
    BUTTON("icon-button"),
    TITLE("icon-title"),
    EMPTY("icon-empty");

    private final String styleClass;

    IconSize(String styleClass) {
        this.styleClass = styleClass;
    }

    public String styleClass() {
        return styleClass;
    }
}
