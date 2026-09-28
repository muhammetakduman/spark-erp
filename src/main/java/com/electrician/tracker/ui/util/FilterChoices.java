package com.electrician.tracker.ui.util;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ComboBox;

/**
 * A drop-down filter whose first entry is "Tümü" (no filter), followed by the
 * values in Turkish alphabetical order.
 */
public final class FilterChoices {

    private FilterChoices() {
    }

    /** Fills the drop-down, keeping the current choice when it is still offered. */
    public static void setUp(ComboBox<String> comboBox, List<String> values) {
        String current = comboBox.getValue();
        List<String> items = new ArrayList<>();
        items.add(allLabel());
        items.addAll(values.stream().sorted(TableSorting.turkishText()).toList());
        comboBox.getItems().setAll(items);
        comboBox.setValue(current != null && items.contains(current) ? current : allLabel());
    }

    /** The chosen value, or {@code null} for "Tümü". */
    public static String selected(ComboBox<String> comboBox) {
        String value = comboBox.getValue();
        return value == null || value.equals(allLabel()) ? null : value;
    }

    public static void reset(ComboBox<String> comboBox) {
        comboBox.setValue(allLabel());
    }

    private static String allLabel() {
        return DialogUtil.message("filter.all");
    }
}
