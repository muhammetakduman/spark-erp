package com.electrician.tracker.ui.util;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * What an empty list shows in its middle: a large faint icon, one line of
 * text and, when useful, the screen's primary action
 * ("Henüz şantiye eklenmemiş" · [+ Yeni Şantiye]).
 */
public final class EmptyState {

    private static final double SPACING = 10;

    private EmptyState() {
    }

    public static VBox of(AppIcon icon, String messageKey) {
        Label text = new Label(DialogUtil.message(messageKey));
        text.getStyleClass().add("empty-state-text");
        text.setWrapText(true);
        VBox box = new VBox(SPACING, Icons.of(icon, IconSize.EMPTY), text);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("empty-state");
        return box;
    }

    public static VBox of(AppIcon icon, String messageKey, String actionKey, Runnable action) {
        VBox box = of(icon, messageKey);
        Button button = Icons.button(AppIcon.ADD, actionKey, action);
        button.getStyleClass().add("primary-button");
        box.getChildren().add(button);
        return box;
    }
}
