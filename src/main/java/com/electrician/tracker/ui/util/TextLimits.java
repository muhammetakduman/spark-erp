package com.electrician.tracker.ui.util;

import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

/**
 * Stops typing (and pasting) beyond a maximum length and keeps a small
 * counter such as "48 / 60" under the field: grey, orange from 90 %, red when
 * full. No warning is shown on save; the text simply cannot get longer.
 */
public final class TextLimits {

    private static final double NEAR_LIMIT_RATIO = 0.9;
    private static final String NEAR = "char-counter-near";
    private static final String FULL = "char-counter-full";

    private TextLimits() {
    }

    public static void attach(TextInputControl field, Label counter, int maxLength) {
        field.setTextFormatter(new TextFormatter<String>(change -> {
            if (change.getControlNewText().length() <= maxLength) {
                return change;
            }
            int room = maxLength - (change.getControlText().length() - (change.getRangeEnd() - change.getRangeStart()));
            if (room <= 0) {
                return null;
            }
            change.setText(change.getText().substring(0, Math.min(room, change.getText().length())));
            return change;
        }));
        if (counter != null) {
            counter.getStyleClass().add("char-counter");
            field.textProperty().addListener((obs, old, text) -> refresh(counter, text, maxLength));
            refresh(counter, field.getText(), maxLength);
        }
    }

    /** Limit without a visible counter (e.g. fields in a table-like row). */
    public static void attach(TextInputControl field, int maxLength) {
        attach(field, null, maxLength);
    }

    private static void refresh(Label counter, String text, int maxLength) {
        int length = text == null ? 0 : text.length();
        counter.setText(DialogUtil.message("limit.counter", length, maxLength));
        counter.getStyleClass().removeAll(List.of(NEAR, FULL));
        if (length >= maxLength) {
            counter.getStyleClass().add(FULL);
        } else if (length >= maxLength * NEAR_LIMIT_RATIO) {
            counter.getStyleClass().add(NEAR);
        }
    }
}
