package com.electrician.tracker.ui.util;

import java.util.List;
import java.util.Objects;

import com.electrician.tracker.service.CanonicalNames;
import com.electrician.tracker.service.MetinKarsilastirici;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;

/**
 * Turns an editable {@link ComboBox} into an optional free-text field with
 * suggestions (supplier, brand, category): earlier values are suggested while
 * typing (matching ignores case and Turkish letters), any new value can be
 * typed freely, and a value saved in this dialog is suggested right away.
 */
public final class SuggestionPicker {

    private final ComboBox<String> comboBox;
    private final ObservableList<String> names = FXCollections.observableArrayList();
    private final FilteredList<String> filtered = new FilteredList<>(names, name -> true);

    public SuggestionPicker(ComboBox<String> comboBox, List<String> knownNames) {
        this.comboBox = comboBox;
        names.setAll(CanonicalNames.distinct(knownNames));
        comboBox.setItems(filtered);
        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> onTyped(newText));
    }

    /** What the user typed (a new or an existing name); empty when left blank. */
    public String typedText() {
        String text = comboBox.getEditor().getText();
        return text == null ? "" : text.trim();
    }

    /** Shows {@code name} as the field's value (e.g. the product's last supplier). */
    public void select(String name) {
        comboBox.setValue(name);
        comboBox.getEditor().setText(name == null ? "" : name);
    }

    public void clear() {
        select(null);
        filtered.setPredicate(name -> true);
    }

    /** Adds a just-saved name to the suggestions unless the same supplier is already listed. */
    public void remember(String savedName) {
        if (savedName == null || savedName.isBlank()) {
            return;
        }
        if (CanonicalNames.findSame(savedName, names).isEmpty()) {
            names.setAll(CanonicalNames.distinct(concat(names, savedName)));
        }
    }

    private void onTyped(String text) {
        // Text written by the ComboBox itself for its selected value must not re-filter:
        // re-filtering re-selects the value, which rewrites the text in a loop.
        if (Objects.equals(text, comboBox.getValue())) {
            return;
        }
        // Deferred: never change the items while the ComboBox is still processing the edit.
        Platform.runLater(this::refreshForTypedText);
    }

    private void refreshForTypedText() {
        String text = comboBox.getEditor().getText();
        if (text == null || Objects.equals(text, comboBox.getValue())) {
            return;
        }
        if (comboBox.getValue() != null) {
            detachValueKeepingText(text);
        }
        String typed = MetinKarsilastirici.normalize(text);
        filtered.setPredicate(name -> MetinKarsilastirici.normalize(name).contains(typed));
    }

    /**
     * Once the typed text no longer is the selected name the value is cleared;
     * otherwise filtering the old value out of the list would wipe what the
     * user typed, so a name not yet in the list could never be entered.
     */
    private void detachValueKeepingText(String text) {
        comboBox.setValue(null);
        comboBox.getEditor().setText(text);
        comboBox.getEditor().positionCaret(text.length());
    }

    private static List<String> concat(List<String> names, String added) {
        List<String> all = new java.util.ArrayList<>(names);
        all.add(added);
        return all;
    }
}
