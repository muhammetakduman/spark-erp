package com.electrician.tracker.ui.controller;

import java.math.BigDecimal;
import java.util.Optional;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.service.PriceHistoryService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.service.QuoteCalculator;
import com.electrician.tracker.service.QuoteFieldLimits;
import com.electrician.tracker.service.QuoteService;
import com.electrician.tracker.service.exception.ValidationException;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.ProductSelector;
import com.electrician.tracker.ui.util.TextLimits;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * One quote line: a catalog product (searchable, filterable by category, and
 * addable on the spot like in the material dialog; its unit and brand are
 * filled in, the brand stays editable and may be left empty) or a free item such as "Sarf malzeme", 1 × lump sum. The unit
 * price is always typed by the user; the product's last sale price is only
 * offered as a suggestion. The line total is calculated, never typed.
 */
@Component
@Scope("prototype")
public class QuoteItemDialogController {

    private final ProductService productService;
    private final PriceHistoryService priceHistoryService;
    private final QuoteService quoteService;

    @FXML
    private RadioButton catalogRadio;
    @FXML
    private RadioButton freeRadio;
    @FXML
    private VBox productHolder;
    @FXML
    private Node freeItemRow;
    @FXML
    private TextField freeNameField;
    @FXML
    private Label freeNameCounter;
    @FXML
    private TextField brandField;
    @FXML
    private Label brandCounter;
    @FXML
    private Label descriptionCounter;
    @FXML
    private DecimalField quantityField;
    @FXML
    private ComboBox<ProductUnit> unitComboBox;
    @FXML
    private DecimalField unitPriceField;
    @FXML
    private Label suggestionLabel;
    @FXML
    private TextField descriptionField;
    @FXML
    private Label lineTotalLabel;
    @FXML
    private Button saveButton;

    private ProductSelector productSelector;
    private QuoteLine result;
    private int editingLineNo;

    public QuoteItemDialogController(ProductService productService, PriceHistoryService priceHistoryService,
            QuoteService quoteService) {
        this.productService = productService;
        this.priceHistoryService = priceHistoryService;
        this.quoteService = quoteService;
    }

    @FXML
    private void initialize() {
        productSelector = new ProductSelector(productService);
        productSelector.setOnProductChanged(this::onProductChosen);
        productHolder.getChildren().setAll(productSelector.view());
        unitComboBox.getItems().setAll(ProductUnit.values());
        unitComboBox.setConverter(new UnitConverter());
        unitComboBox.setValue(ProductUnit.PIECE);
        quantityField.setValue(BigDecimal.ONE);
        catalogRadio.selectedProperty().addListener((obs, o, catalog) -> applyMode(catalog));
        quantityField.textProperty().addListener((obs, o, n) -> refreshTotal());
        unitPriceField.textProperty().addListener((obs, o, n) -> refreshTotal());
        TextLimits.attach(freeNameField, freeNameCounter, QuoteFieldLimits.PRODUCT_NAME);
        TextLimits.attach(brandField, brandCounter, QuoteFieldLimits.BRAND);
        TextLimits.attach(descriptionField, descriptionCounter, QuoteFieldLimits.DESCRIPTION);
        refreshTotal();
    }

    /** Opens the dialog on an existing line so it can be changed (its own brand is kept). */
    public void editLine(QuoteLine line) {
        this.editingLineNo = line.lineNo();
        if (line.isFreeItem()) {
            freeRadio.setSelected(true);
            freeNameField.setText(line.productName());
        } else {
            productSelector.select(productService.findById(line.productId()));
        }
        brandField.setText(line.brand());
        quantityField.setValue(line.quantity());
        unitComboBox.setValue(line.unit());
        unitPriceField.setValue(line.unitPrice());
        descriptionField.setText(line.description());
    }

    /** The line built from the dialog, or empty when it was cancelled. */
    public Optional<QuoteLine> getResult() {
        return Optional.ofNullable(result);
    }

    private void applyMode(boolean catalog) {
        setShown(productHolder, catalog);
        setShown(freeItemRow, !catalog);
        setShown(suggestionLabel, catalog && !suggestionLabel.getText().isEmpty());
    }

    /**
     * Unit and brand from the product (the brand stays editable); its last
     * sale price is shown as a suggestion and pre-filled when empty.
     */
    private void onProductChosen(Product product) {
        if (product == null) {
            suggestionLabel.setText("");
            setShown(suggestionLabel, false);
            return;
        }
        unitComboBox.setValue(product.getUnit());
        brandField.setText(product.getBrand());
        BigDecimal lastSalePrice = priceHistoryService.suggestFor(product.getId()).lastSaleUnitPrice();
        suggestionLabel.setText(lastSalePrice == null ? ""
                : DialogUtil.message("quote.item.suggestion", Bicimlendirici.moneyTl(lastSalePrice)));
        setShown(suggestionLabel, lastSalePrice != null);
        if (lastSalePrice != null && unitPriceField.getText().isBlank()) {
            unitPriceField.setValue(lastSalePrice);
        }
    }

    private void refreshTotal() {
        try {
            BigDecimal total = QuoteCalculator.lineTotal(quantityField.getValue(), unitPriceField.getValue());
            lineTotalLabel.setText(total == null ? "—" : Bicimlendirici.moneyTl(total));
        } catch (NumberFormatException e) {
            lineTotalLabel.setText("—");
        }
    }

    @FXML
    private void onSave() {
        try {
            result = buildLine();
            ((Stage) saveButton.getScene().getWindow()).close();
        } catch (NumberFormatException e) {
            DialogUtil.showErrorMessage("error.materialItem.number.invalid");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    private QuoteLine buildLine() {
        if (!catalogRadio.isSelected()) {
            return quoteService.checkLine(new QuoteLine(editingLineNo, null, freeNameField.getText(),
                    brandField.getText(), quantityField.getValue(), unitComboBox.getValue(), unitPriceField.getValue(),
                    descriptionField.getText()));
        }
        Product product = productSelector.resolveSelection()
                .orElseThrow(() -> new ValidationException("error.materialItem.product.required"));
        return quoteService.checkLine(new QuoteLine(editingLineNo, product.getId(), product.getName(),
                brandField.getText(), quantityField.getValue(), unitComboBox.getValue(), unitPriceField.getValue(),
                descriptionField.getText()));
    }

    @FXML
    private void onCancel() {
        result = null;
        ((Stage) saveButton.getScene().getWindow()).close();
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
