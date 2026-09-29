package com.electrician.tracker.ui.util;

import java.util.Optional;
import java.util.function.Consumer;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.dto.ProductCreationResult;
import com.electrician.tracker.service.ProductService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * The product field of the material and quote dialogs: a small category
 * filter next to a type-to-search product list, and — when the typed product
 * is not in the catalog — an inline form (name, brand, category, unit) that
 * adds it on the spot. New brands and categories are suggested at once.
 */
public final class ProductSelector {

    private static final double SPACING = 8;
    private static final double CATEGORY_WIDTH = 150;
    private static final double UNIT_WIDTH = 100;
    private static final double SUGGESTION_WIDTH = 130;
    private static final String ADD_LINK_STYLE = "add-product-link";

    private final ProductService productService;
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final ComboBox<Product> productComboBox = new ComboBox<>();
    private final Hyperlink addProductLink = new Hyperlink();
    private final TextField newNameField = new TextField();
    private final ComboBox<String> newBrandComboBox = new ComboBox<>();
    private final ComboBox<String> newCategoryComboBox = new ComboBox<>();
    private final ComboBox<ProductUnit> newUnitComboBox = new ComboBox<>();
    private final HBox newProductForm;
    private final Label infoLabel = new Label();
    private final VBox view;
    private final ProductPicker productPicker;
    private final SuggestionPicker brandPicker;
    private final SuggestionPicker categoryPicker;
    private Consumer<Product> onProductChanged = product -> { };

    public ProductSelector(ProductService productService) {
        this.productService = productService;
        productComboBox.setEditable(true);
        productComboBox.setMaxWidth(Double.MAX_VALUE);
        productComboBox.setPromptText(DialogUtil.message("product.search.prompt"));
        HBox.setHgrow(productComboBox, Priority.ALWAYS);
        productPicker = new ProductPicker(productComboBox, productService.findAllActive());
        categoryFilter.setPrefWidth(CATEGORY_WIDTH);
        FilterChoices.setUp(categoryFilter, productService.findDistinctCategories());
        categoryFilter.valueProperty().addListener((obs, o, n) ->
                productPicker.setCategory(FilterChoices.selected(categoryFilter)));

        brandPicker = new SuggestionPicker(newBrandComboBox, productService.findDistinctBrands());
        categoryPicker = new SuggestionPicker(newCategoryComboBox, productService.findDistinctCategories());
        newProductForm = buildNewProductForm();
        setShown(newProductForm, false);
        addProductLink.getStyleClass().add(ADD_LINK_STYLE);
        addProductLink.setOnAction(e -> showNewProductForm());
        setShown(addProductLink, false);
        infoLabel.getStyleClass().add("info-note");
        infoLabel.setWrapText(true);
        setShown(infoLabel, false);

        HBox pickerRow = new HBox(SPACING, categoryFilter, productComboBox);
        pickerRow.setAlignment(Pos.CENTER_LEFT);
        view = new VBox(SPACING / 2, pickerRow, addProductLink, newProductForm, infoLabel);
        productComboBox.getEditor().textProperty().addListener((obs, o, n) -> refreshAddLink());
        productComboBox.valueProperty().addListener((obs, o, product) -> {
            refreshAddLink();
            onProductChanged.accept(product);
        });
    }

    public Node view() {
        return view;
    }

    /** Called with the newly chosen product (or {@code null}) whenever the choice changes. */
    public void setOnProductChanged(Consumer<Product> listener) {
        this.onProductChanged = listener == null ? product -> { } : listener;
    }

    public Optional<Product> resolveSelection() {
        return productPicker.resolveSelection();
    }

    public void select(Product product) {
        productPicker.addAndSelect(product);
    }

    public void clear() {
        productPicker.clear();
        setShown(infoLabel, false);
    }

    public void requestFocus() {
        productComboBox.requestFocus();
    }

    private HBox buildNewProductForm() {
        newNameField.setPromptText(DialogUtil.message("product.field.name"));
        HBox.setHgrow(newNameField, Priority.ALWAYS);
        newBrandComboBox.setEditable(true);
        newBrandComboBox.setPromptText(DialogUtil.message("product.field.brand"));
        newBrandComboBox.setPrefWidth(SUGGESTION_WIDTH);
        newCategoryComboBox.setEditable(true);
        newCategoryComboBox.setPromptText(DialogUtil.message("product.field.category"));
        newCategoryComboBox.setPrefWidth(SUGGESTION_WIDTH);
        newUnitComboBox.getItems().setAll(ProductUnit.values());
        newUnitComboBox.setConverter(new UnitConverter());
        newUnitComboBox.setValue(ProductUnit.PIECE);
        newUnitComboBox.setPrefWidth(UNIT_WIDTH);
        Button save = new Button(DialogUtil.message("action.save"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> saveNewProduct());
        Button cancel = new Button(DialogUtil.message("action.cancel"));
        cancel.setOnAction(e -> {
            setShown(newProductForm, false);
            refreshAddLink();
        });
        HBox form = new HBox(SPACING, newNameField, newBrandComboBox, newCategoryComboBox, newUnitComboBox, save,
                cancel);
        form.setAlignment(Pos.CENTER_LEFT);
        form.getStyleClass().add("inline-form");
        return form;
    }

    private void refreshAddLink() {
        String typed = productPicker.typedText();
        boolean unknown = productComboBox.getValue() == null && productPicker.isUnknownName(typed);
        setShown(addProductLink, unknown && !newProductForm.isVisible());
        if (unknown) {
            addProductLink.setText(DialogUtil.message("material.action.addNewProduct", typed));
        }
    }

    private void showNewProductForm() {
        newNameField.setText(productPicker.typedText());
        String category = FilterChoices.selected(categoryFilter);
        if (category != null) {
            categoryPicker.select(category);
        }
        setShown(newProductForm, true);
        setShown(addProductLink, false);
        setShown(infoLabel, false);
        newNameField.requestFocus();
    }

    private void saveNewProduct() {
        try {
            ProductCreationResult result = productService.createOrReuse(newNameField.getText(),
                    brandPicker.typedText(), categoryPicker.typedText(), newUnitComboBox.getValue());
            Product product = result.product();
            brandPicker.remember(product.getBrand());
            categoryPicker.remember(product.getCategory());
            FilterChoices.setUp(categoryFilter, productService.findDistinctCategories());
            productPicker.addAndSelect(product);
            showInfo(result);
            setShown(newProductForm, false);
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    private void showInfo(ProductCreationResult result) {
        String key = result.existing() ? "material.info.productReused" : "material.info.productCreated";
        infoLabel.setText(DialogUtil.message(key, result.product().getDisplayName()));
        setShown(infoLabel, true);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private static final class UnitConverter extends StringConverter<ProductUnit> {
        @Override
        public String toString(ProductUnit unit) {
            return EnumLabels.label(unit);
        }

        @Override
        public ProductUnit fromString(String text) {
            return null;
        }
    }
}
