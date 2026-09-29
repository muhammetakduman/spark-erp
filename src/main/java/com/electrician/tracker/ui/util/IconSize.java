package com.electrician.tracker.ui.util;

/**
 * The icon sizes of the application; the pixel sizes are set in app.css
 * through these style classes (menu 18, button 16, dialog title 24, message
 * window 28, empty screen 48, status badge 11, urgent mark 12).
 */
public enum IconSize {
    MENU("icon-menu"),
    BUTTON("icon-button"),
    TITLE("icon-title"),
    EMPTY("icon-empty"),
    DIALOG("icon-dialog"),
    BADGE("icon-badge"),
    MARK("icon-mark");

    private final String styleClass;

    IconSize(String styleClass) {
        this.styleClass = styleClass;
    }

    public String styleClass() {
        return styleClass;
    }
}
