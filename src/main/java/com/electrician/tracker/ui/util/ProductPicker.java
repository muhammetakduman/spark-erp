package com.electrician.tracker.ui.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.service.MetinKarsilastirici;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

/**
 * Turns an editable {@link ComboBox} into a type-to-filter product picker.
 * Matching ignores case, Turkish characters and extra spaces; the list stays
 * sorted by name, and products created at runtime can be added and selected
 * without reloading the catalog.
 */
public final class ProductPicker {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    private final ComboBox<Product> comboBox;
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final FilteredList<Product> filtered;

    public ProductPicker(ComboBox<Product> comboBox, List<Product> catalog) {
        this.comboBox = comboBox;
        products.setAll(catalog);
        Comparator<Product> byName = Comparator.comparing(Product::getName, Collator.getInstance(TURKISH));
        filtered = new FilteredList<>(new SortedList<>(products, byName), p -> true);
        comboBox.setItems(filtered);
        comboBox.setConverter(new ProductNameConverter());
        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> onTyped(newText));
    }

    public String typedText() {
        String text = comboBox.getEditor().getText();
        return text == null ? "" : text.trim();
    }

    /**
     * The product the user means: the selected value if it still matches the
     * typed text, otherwise an exact (normalized) name match, else empty.
     */
    public Optional<Product> resolveSelection() {
        Product value = comboBox.getValue();
        if (value != null && sameName(value.getName(), typedText())) {
            return Optional.of(value);
        }
        return findExactMatch(typedText());
    }

    public boolean isUnknownName(String text) {
        return text != null && !text.isBlank() && findExactMatch(text).isEmpty();
    }

    public void addAndSelect(Product product) {
        Product listed = products.stream()
                .filter(p -> p.getId().equals(product.getId()))
                .findFirst()
                .orElseGet(() -> {
                    products.add(product);
                    return product;
                });
        filtered.setPredicate(p -> true);
        comboBox.setValue(listed);
    }

    public void clear() {
        comboBox.setValue(null);
        comboBox.getEditor().clear();
    }

    private void onTyped(String text) {
        if (valueMatches(text)) {
            return;
        }
        // Deferred: changing the value or predicate while the ComboBox is still
        // processing the same edit event can corrupt its selection model.
        Platform.runLater(this::refreshForTypedText);
    }

    private void refreshForTypedText() {
        String text = comboBox.getEditor().getText();
        if (text == null || valueMatches(text)) {
            return;
        }
        if (comboBox.getValue() != null) {
            detachValueKeepingText(text);
        }
        String normalized = MetinKarsilastirici.normalize(text);
        filtered.setPredicate(p -> MetinKarsilastirici.normalize(p.getName()).contains(normalized));
        if (comboBox.isFocused() && !normalized.isEmpty() && !filtered.isEmpty()) {
            comboBox.show();
        }
    }

    /**
     * Once the typed text no longer names the selected product the value is
     * cleared, otherwise committing the edit (e.g. on focus loss) would turn
     * the value to null and the ComboBox would wipe what the user typed.
     */
    private void detachValueKeepingText(String text) {
        comboBox.setValue(null);
        comboBox.getEditor().setText(text);
        comboBox.getEditor().positionCaret(text.length());
    }

    private boolean valueMatches(String text) {
        Product value = comboBox.getValue();
        return value != null && value.getName().equals(text);
    }

    private Optional<Product> findExactMatch(String text) {
        return products.stream().filter(p -> sameName(p.getName(), text)).findFirst();
    }

    private static boolean sameName(String left, String right) {
        return MetinKarsilastirici.normalize(left).equals(MetinKarsilastirici.normalize(right));
    }

    private final class ProductNameConverter extends StringConverter<Product> {
        @Override
        public String toString(Product product) {
            return product == null ? "" : product.getName();
        }

        @Override
        public Product fromString(String text) {
            return findExactMatch(text).orElse(null);
        }
    }
}
