package com.electrician.tracker.ui.util;

import java.math.BigDecimal;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * The one input for amounts and quantities. Accepts only digits and a single
 * decimal comma, groups thousands while typing and shows the final tr-TR form
 * ("1.000,00") when the field is left, so "1000", "1.000" and "1.000,00" are
 * always the same value and look the same everywhere. Usable from FXML:
 * {@code <DecimalField scale="3" padDecimals="false"/>} for a quantity.
 */
public class DecimalField extends TextField {

    private int scale = NumberInput.MONEY_SCALE;
    private boolean padDecimals = true;

    public DecimalField() {
        getStyleClass().add("decimal-field");
        setTextFormatter(new TextFormatter<>(this::filter));
        focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) {
                setValue(getValue());
            }
        });
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public boolean isPadDecimals() {
        return padDecimals;
    }

    public void setPadDecimals(boolean padDecimals) {
        this.padDecimals = padDecimals;
    }

    /** The entered number, or {@code null} when empty. */
    public BigDecimal getValue() {
        return Bicimlendirici.parseMoney(getText());
    }

    public void setValue(BigDecimal value) {
        setText(NumberInput.format(value, scale, padDecimals));
    }

    private TextFormatter.Change filter(TextFormatter.Change change) {
        if (!change.isContentChange()) {
            return change;
        }
        String current = change.getControlText();
        String inserted = change.getText().length() > 1 ? NumberInput.canonicalInsert(change.getText()) : change.getText();
        String before = current.substring(0, change.getRangeStart()) + inserted;
        String proposed = before + current.substring(change.getRangeEnd());
        String formatted = NumberInput.normalizeTyping(proposed, scale);
        if (formatted == null) {
            return null;
        }
        int caret = NumberInput.caretFor(formatted, NumberInput.significantCount(before));
        change.setRange(0, current.length());
        change.setText(formatted);
        change.selectRange(caret, caret);
        return change;
    }
}
