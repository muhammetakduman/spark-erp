package com.electrician.tracker.ui.controller;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ProductFormController {

    private final ProductService productService;

    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<ProductUnit> unitComboBox;

    private Long editingId;
    private boolean saved;

    public ProductFormController(ProductService productService) {
        this.productService = productService;
    }

    @FXML
    private void initialize() {
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
        nameField.setText(product.getName());
        unitComboBox.setValue(product.getUnit());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave(ActionEvent event) {
        Product product = new Product(nameField.getText(), unitComboBox.getValue());
        try {
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
