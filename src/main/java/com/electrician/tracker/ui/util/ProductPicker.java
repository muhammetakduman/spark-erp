package com.electrician.tracker.ui.util;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.service.MetinKarsilastirici;
import com.electrician.tracker.service.ProductFilter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

/**
 * Turns an editable {@link ComboBox} into a type-to-filter product picker.
 * Products show as "Marka — Ürün"; typing matches name and brand ignoring
 * case, Turkish characters and extra spaces, and an optional category
 * narrows long lists. Products created at runtime can be added and selected
 * without reloading the catalog.
 */
public final class ProductPicker {

    private final ComboBox<Product> comboBox;
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final FilteredList<Product> filtered;
    private String category;

    public ProductPicker(ComboBox<Product> comboBox, List<Product> catalog) {
        this.comboBox = comboBox;
        products.setAll(catalog);
        Comparator<Product> byName = Comparator.comparing(Product::getDisplayName, TableSorting.turkishText());
        filtered = new FilteredList<>(new SortedList<>(products, byName), p -> true);
        comboBox.setItems(filtered);
        comboBox.setConverter(new ProductNameConverter());
        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> onTyped(newText));
    }

    public String typedText() {
        String text = comboBox.getEditor().getText();
        return text == null ? "" : text.trim();
    }

    /** Limits the list to one category ({@code null} = all). */
    public void setCategory(String category) {
        this.category = category;
        applyFilter(valueMatches(comboBox.getEditor().getText()) ? "" : comboBox.getEditor().getText());
    }

    /**
     * The product the user means: the selected value if it still matches the
     * typed text, otherwise an exact (normalized) name match, else empty.
     */
    public Optional<Product> resolveSelection() {
        Product value = comboBox.getValue();
        if (value != null && sameName(value.getDisplayName(), typedText())) {
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
        applyFilter("");
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
        applyFilter(text);
        if (comboBox.isFocused() && !MetinKarsilastirici.normalize(text).isEmpty() && !filtered.isEmpty()) {
            comboBox.show();
        }
    }

    private void applyFilter(String text) {
        ProductFilter filter = new ProductFilter(text, category, null, null);
        filtered.setPredicate(filter::matches);
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
        return value != null && value.getDisplayName().equals(text);
    }

    /** "Marka — Ürün" exactly, or a plain name that only one product has. */
    private Optional<Product> findExactMatch(String text) {
        Optional<Product> byDisplayName = products.stream()
                .filter(p -> sameName(p.getDisplayName(), text))
                .findFirst();
        if (byDisplayName.isPresent()) {
            return byDisplayName;
        }
        List<Product> byName = products.stream().filter(p -> sameName(p.getName(), text)).toList();
        return byName.size() == 1 ? Optional.of(byName.get(0)) : Optional.empty();
    }

    private static boolean sameName(String left, String right) {
        return MetinKarsilastirici.normalize(left).equals(MetinKarsilastirici.normalize(right));
    }

    private final class ProductNameConverter extends StringConverter<Product> {
        @Override
        public String toString(Product product) {
            return product == null ? "" : product.getDisplayName();
        }

        @Override
        public Product fromString(String text) {
            return findExactMatch(text).orElse(null);
        }
    }
}
