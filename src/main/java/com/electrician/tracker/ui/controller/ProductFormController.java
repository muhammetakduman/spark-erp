package com.electrician.tracker.ui.controller;

import java.util.List;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.SuggestionPicker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Adds or edits a catalog product: name, optional brand and category (both
 * suggest what was entered before, and accept anything new), unit, and for
 * users who may see purchase prices the usual supplier and VAT-exclusive
 * purchase cost.
 */
@Component
@Scope("prototype")
public class ProductFormController {

    private final ProductService productService;
    private final MaterialService materialService;
    private final AccessControl accessControl;

    @FXML
    private CheckBox activeCheckBox;
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> brandComboBox;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private ComboBox<ProductUnit> unitComboBox;
    @FXML
    private Label supplierLabel;
    @FXML
    private ComboBox<String> supplierComboBox;
    @FXML
    private Label purchasePriceLabel;
    @FXML
    private DecimalField purchasePriceField;

    private SuggestionPicker brandPicker;
    private SuggestionPicker categoryPicker;
    private SuggestionPicker supplierPicker;
    private Long editingId;
    private boolean saved;

    public ProductFormController(ProductService productService, MaterialService materialService,
            AccessControl accessControl) {
        this.productService = productService;
        this.materialService = materialService;
        this.accessControl = accessControl;
    }

    @FXML
    private void initialize() {
        brandPicker = new SuggestionPicker(brandComboBox, productService.findDistinctBrands());
        categoryPicker = new SuggestionPicker(categoryComboBox, productService.findDistinctCategories());
        supplierPicker = new SuggestionPicker(supplierComboBox, materialService.findDistinctSupplierNames());
        boolean purchaseVisible = accessControl.canViewFinancials();
        for (Node node : List.of(supplierLabel, supplierComboBox, purchasePriceLabel,
                purchasePriceField)) {
            node.setVisible(purchaseVisible);
            node.setManaged(purchaseVisible);
        }
        unitComboBox.getItems().setAll(ProductUnit.values());
        unitComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductUnit unit) {
                return EnumLabels.label(unit);
            }

            @Override
            public ProductUnit fromString(String string) {
                return null;
            }
        });
    }

    public void editExisting(Product product) {
        this.editingId = product.getId();
        activeCheckBox.setSelected(product.isActive());
        nameField.setText(product.getName());
        brandPicker.select(product.getBrand());
        categoryPicker.select(product.getCategory());
        unitComboBox.setValue(product.getUnit());
        supplierPicker.select(product.getSupplierName());
        purchasePriceField.setValue(product.getPurchasePrice());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave(ActionEvent event) {
        Product product = new Product(nameField.getText(), unitComboBox.getValue(), brandPicker.typedText(),
                categoryPicker.typedText());
        try {
            product.setActive(activeCheckBox.isSelected());
            product.setSupplierName(supplierPicker.typedText());
            product.setPurchasePrice(purchasePriceField.getValue());
            if (editingId == null) {
                productService.create(product);
            } else {
                productService.update(editingId, product);
            }
            saved = true;
            closeStage(event);
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onCancel(ActionEvent event) {
        closeStage(event);
    }

    private void closeStage(ActionEvent event) {
        ((Stage) ((Button) event.getSource()).getScene().getWindow()).close();
    }
}
