package com.electrician.tracker.ui.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javafx.scene.control.ComboBox;

/**
 * Keeps picker lists (customers, employees, …) sorted by Turkish name and
 * lets a record created from a "+ Yeni" dialog appear and be selected at
 * once, without reloading the list or restarting the app.
 */
public final class SelectionLists {


    private SelectionLists() {
    }

    public static <T> Comparator<T> byName(Function<T, String> name) {
        Collator collator = Collator.getInstance(Bicimlendirici.TURKISH);
        return Comparator.comparing(item -> Objects.requireNonNullElse(name.apply(item), ""), collator);
    }

    public static <T> void setSorted(ComboBox<T> comboBox, List<T> items, Function<T, String> name) {
        comboBox.getItems().setAll(items.stream().sorted(byName(name)).toList());
    }

    /**
     * Adds the saved record (replacing a stale copy with the same id),
     * re-sorts by name and selects it. Returns the instance now in the list.
     */
    public static <T> T addSortedAndSelect(ComboBox<T> comboBox, T saved, Function<T, String> name,
            Function<T, Object> id) {
        comboBox.getItems().removeIf(item -> Objects.equals(id.apply(item), id.apply(saved)));
        comboBox.getItems().add(saved);
        comboBox.getItems().sort(byName(name));
        comboBox.setValue(saved);
        return saved;
    }
}
