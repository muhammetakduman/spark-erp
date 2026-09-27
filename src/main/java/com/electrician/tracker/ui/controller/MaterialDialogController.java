package com.electrician.tracker.ui.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.dto.PriceSuggestion;
import com.electrician.tracker.dto.ProductCreationResult;
import com.electrician.tracker.service.MaterialPriceCalculator;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.PriceHistoryService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.MaterialColumns;
import com.electrician.tracker.ui.util.ProductPicker;
import com.electrician.tracker.ui.util.SupplierPicker;
import com.electrician.tracker.ui.util.VatSelector;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The one material entry dialog used by both site and service jobs. With a
 * job set it saves each line immediately; in draft mode (service form, job
 * not created yet) it only validates the lines and hands them back.
 */
@Component
@Scope("prototype")
public class MaterialDialogController {

    private final MaterialService materialService;
    private final ProductService productService;
    private final PriceHistoryService priceHistoryService;

    @FXML
    private Node dateRow;
    @FXML
    private DatePicker itemDatePicker;
    @FXML
    private Button saveAndNewButton;
    @FXML
    private Button saveButton;
    @FXML
    private Label addedInSessionLabel;
    @FXML
    private ComboBox<Product> productComboBox;
    @FXML
    private Hyperlink addProductLink;
    @FXML
    private Node newProductForm;
    @FXML
    private TextField newProductNameField;
    @FXML
    private ComboBox<ProductUnit> newProductUnitComboBox;
    @FXML
    private Label productInfoLabel;
    @FXML
    private DecimalField quantityField;
    @FXML
    private DecimalField unitPriceField;
    @FXML
    private DecimalField totalPriceField;
    @FXML
    private DecimalField purchasePriceField;
    @FXML
    private ComboBox<String> supplierComboBox;
    @FXML
    private VatSelector vatSelector;
    @FXML
    private VatSelector purchaseVatSelector;
    @FXML
    private TextField noteField;
    @FXML
    private TableView<MaterialItem> addedTable;
    @FXML
    private TableColumn<MaterialItem, String> addedProductColumn;
    @FXML
    private TableColumn<MaterialItem, String> addedQuantityColumn;
    @FXML
    private TableColumn<MaterialItem, String> addedVatColumn;
    @FXML
    private TableColumn<MaterialItem, MaterialItem> addedTotalColumn;

    private final ObservableList<MaterialItem> addedItems = FXCollections.observableArrayList();
    private ProductPicker productPicker;
    private SupplierPicker supplierPicker;
    private Job job;
    private LocalDate draftDate;
    private boolean anyAdded;
    private PriceEntryType leadingPriceField = PriceEntryType.UNIT;
    private boolean syncingPrices;
    private PriceSuggestion appliedSuggestion = PriceSuggestion.empty();
    private Long editingId;
    private boolean loadingExisting;

    public MaterialDialogController(MaterialService materialService, ProductService productService,
            PriceHistoryService priceHistoryService) {
        this.materialService = materialService;
        this.productService = productService;
        this.priceHistoryService = priceHistoryService;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    /**
     * Switches to draft mode: lines are validated but not saved. Draft lines
     * take the service date, so the date field is hidden.
     */
    public void startDraft(LocalDate itemDate) {
        this.draftDate = itemDate;
        setShown(dateRow, false);
    }

    /**
     * Edits one saved line: fields are filled from it (no price suggestions),
     * and saving updates it instead of adding a new line.
     */
    public void editExisting(MaterialItem item) {
        this.job = item.getJob();
        this.editingId = item.getId();
        loadingExisting = true;
        try {
            fillFrom(item);
        } finally {
            loadingExisting = false;
        }
        setShown(saveAndNewButton, false);
        setShown(addedInSessionLabel, false);
        setShown(addedTable, false);
        saveButton.setDefaultButton(true);
    }

    private void fillFrom(MaterialItem item) {
        itemDatePicker.setValue(item.getItemDate());
        productPicker.addAndSelect(item.getProduct());
        quantityField.setValue(item.getQuantity());
        if (item.getPriceEntryType() == PriceEntryType.TOTAL) {
            totalPriceField.setValue(item.getSaleTotalAmount());
        } else {
            unitPriceField.setValue(item.getSaleUnitPrice());
        }
        purchasePriceField.setValue(item.getPurchaseUnitPrice());
        purchaseVatSelector.setValue(item.getPurchaseVatRate(), item.getPurchaseVatIncluded());
        supplierPicker.select(item.getSupplierName());
        vatSelector.setValue(item.getVatRate(), item.getVatIncluded());
        noteField.setText(item.getNote());
    }

    public boolean isAnyAdded() {
        return anyAdded;
    }

    public List<MaterialItem> getAddedItems() {
        return List.copyOf(addedItems);
    }

    @FXML
    private void initialize() {
        itemDatePicker.setValue(LocalDate.now());
        setUpProductPicker();
        setUpNewProductForm();
        setUpSupplierCombo();
        setUpPriceSync();
        setUpAddedTable();
        vatSelector.bindAmount(totalPriceField);
        purchaseVatSelector.bindAmount(purchasePriceField);
    }

    private void setUpProductPicker() {
        productPicker = new ProductPicker(productComboBox, productService.findAll());
        productComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> refreshAddProductLink());
        productComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            refreshAddProductLink();
            applySuggestion(newValue);
        });
    }

    private void setUpNewProductForm() {
        newProductUnitComboBox.getItems().setAll(ProductUnit.values());
        newProductUnitComboBox.setConverter(new EnumLabelConverter<>());
        newProductUnitComboBox.setValue(ProductUnit.PIECE);
    }

    private void setUpSupplierCombo() {
        supplierPicker = new SupplierPicker(supplierComboBox, materialService.findDistinctSupplierNames());
    }

    private void setUpPriceSync() {
        unitPriceField.textProperty().addListener((obs, o, n) -> onPriceEdited(PriceEntryType.UNIT));
        totalPriceField.textProperty().addListener((obs, o, n) -> onPriceEdited(PriceEntryType.TOTAL));
        quantityField.textProperty().addListener((obs, o, n) -> syncDerivedPrice());
    }

    private void setUpAddedTable() {
        addedTable.setItems(addedItems);
        addedProductColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct().getName()));
        addedQuantityColumn.setCellValueFactory(d -> new SimpleStringProperty(
                Bicimlendirici.quantity(d.getValue().getQuantity()) + " " + EnumLabels.label(d.getValue().getProduct().getUnit())));
        MaterialColumns.bindVat(addedVatColumn);
        MaterialColumns.bindTotal(addedTotalColumn);
    }

    /** The field the user typed into last leads; the other one is derived from it. */
    private void onPriceEdited(PriceEntryType editedField) {
        if (syncingPrices) {
            return;
        }
        leadingPriceField = editedField;
        syncDerivedPrice();
    }

    private void syncDerivedPrice() {
        if (syncingPrices) {
            return;
        }
        syncingPrices = true;
        try {
            BigDecimal quantity = quantityField.getValue();
            if (leadingPriceField == PriceEntryType.UNIT) {
                totalPriceField.setValue(MaterialPriceCalculator.totalFromUnitPrice(quantity, unitPriceField.getValue()));
            } else {
                unitPriceField.setValue(MaterialPriceCalculator.unitPriceFromTotal(totalPriceField.getValue(), quantity));
            }
        } finally {
            syncingPrices = false;
        }
    }

    private void refreshAddProductLink() {
        String typed = productPicker.typedText();
        boolean unknown = productComboBox.getValue() == null && productPicker.isUnknownName(typed);
        setShown(addProductLink, unknown && !newProductForm.isVisible());
        if (unknown) {
            addProductLink.setText(DialogUtil.message("material.action.addNewProduct", typed));
        }
    }

    /**
     * Fills the chosen product's last prices, supplier and VAT. A value that
     * was only suggested for the previously chosen product (and not changed by
     * the user since) is cleared when the new product has no such history, so
     * one product's VAT or price never silently carries over to another.
     */
    private void applySuggestion(Product product) {
        if (loadingExisting) {
            return;
        }
        PriceSuggestion previous = appliedSuggestion;
        PriceSuggestion next = product == null ? PriceSuggestion.empty() : priceHistoryService.suggestFor(product.getId());
        replaceSuggestedPrice(purchasePriceField, previous.lastPurchaseUnitPrice(), next.lastPurchaseUnitPrice());
        replaceSuggestedPrice(unitPriceField, previous.lastSaleUnitPrice(), next.lastSaleUnitPrice());
        replaceSuggestedSupplier(previous.lastSupplierName(), next.lastSupplierName());
        replaceSuggestedVat(vatSelector, previous.lastVatRate(), previous.lastVatIncluded(),
                next.lastVatRate(), next.lastVatIncluded());
        replaceSuggestedVat(purchaseVatSelector, previous.lastPurchaseVatRate(), previous.lastPurchaseVatIncluded(),
                next.lastPurchaseVatRate(), next.lastPurchaseVatIncluded());
        appliedSuggestion = next;
    }

    private void replaceSuggestedPrice(DecimalField field, BigDecimal previous, BigDecimal next) {
        if (next != null) {
            field.setValue(next);
        } else if (previous != null && field.getValue() != null && previous.compareTo(field.getValue()) == 0) {
            field.clear();
        }
    }

    private void replaceSuggestedSupplier(String previous, String next) {
        if (next != null) {
            supplierPicker.select(next);
        } else if (previous != null && previous.equals(supplierPicker.typedText())) {
            supplierPicker.clear();
        }
    }

    private static void replaceSuggestedVat(VatSelector selector, Integer previousRate, Boolean previousIncluded,
            Integer nextRate, Boolean nextIncluded) {
        if (nextRate != null) {
            selector.setValue(nextRate, nextIncluded);
        } else if (previousRate != null && previousRate.equals(selector.getRate())
                && Boolean.TRUE.equals(previousIncluded) == Boolean.TRUE.equals(selector.getIncluded())) {
            selector.clear();
        }
    }

    @FXML
    private void onShowNewProductForm() {
        newProductNameField.setText(productPicker.typedText());
        setShown(newProductForm, true);
        setShown(addProductLink, false);
        setShown(productInfoLabel, false);
        newProductNameField.requestFocus();
    }

    @FXML
    private void onSaveNewProduct() {
        try {
            ProductCreationResult result = productService.createOrReuse(
                    newProductNameField.getText(), newProductUnitComboBox.getValue());
            productPicker.addAndSelect(result.product());
            showProductInfo(result);
            setShown(newProductForm, false);
            quantityField.requestFocus();
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onCancelNewProduct() {
        setShown(newProductForm, false);
        refreshAddProductLink();
    }

    private void showProductInfo(ProductCreationResult result) {
        String key = result.existing() ? "material.info.productReused" : "material.info.productCreated";
        productInfoLabel.setText(DialogUtil.message(key, result.product().getName()));
        setShown(productInfoLabel, true);
    }

    @FXML
    private void onSaveAndNew() {
        if (saveCurrentItem()) {
            clearItemFields();
            productComboBox.requestFocus();
        }
    }

    @FXML
    private void onSave() {
        if (saveCurrentItem()) {
            onClose();
        }
    }

    private boolean saveCurrentItem() {
        MaterialItem item;
        try {
            item = buildItem();
        } catch (NumberFormatException e) {
            DialogUtil.showErrorMessage("error.materialItem.number.invalid");
            return false;
        }
        try {
            MaterialItem saved = persist(item);
            supplierPicker.remember(saved.getSupplierName());
            addedItems.add(saved);
            anyAdded = true;
            return true;
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
            return false;
        }
    }

    private MaterialItem persist(MaterialItem item) {
        if (isDraftMode()) {
            return materialService.prepareDraft(item);
        }
        return editingId == null ? materialService.addItem(item) : materialService.update(editingId, item);
    }

    private MaterialItem buildItem() {
        boolean totalLeads = leadingPriceField == PriceEntryType.TOTAL;
        MaterialItem item = new MaterialItem(
                job,
                productPicker.resolveSelection().orElse(null),
                isDraftMode() ? draftDate : itemDatePicker.getValue(),
                quantityField.getValue(),
                purchasePriceField.getValue(),
                supplierPicker.typedText(),
                totalLeads ? null : unitPriceField.getValue(),
                leadingPriceField,
                totalLeads ? totalPriceField.getValue() : null,
                vatSelector.getRate(),
                noteField.getText());
        item.setVatIncluded(vatSelector.getIncluded());
        item.setPurchaseVat(purchaseVatSelector.getRate(), purchaseVatSelector.getIncluded());
        return item;
    }

    private boolean isDraftMode() {
        return job == null;
    }

    private void clearItemFields() {
        productPicker.clear();
        setShown(productInfoLabel, false);
        quantityField.clear();
        leadingPriceField = PriceEntryType.UNIT;
        unitPriceField.clear();
        totalPriceField.clear();
        purchasePriceField.clear();
        supplierPicker.clear();
        vatSelector.clear();
        purchaseVatSelector.clear();
        noteField.clear();
    }

    @FXML
    private void onClose() {
        ((Stage) addedTable.getScene().getWindow()).close();
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private static final class EnumLabelConverter<E extends Enum<E>> extends StringConverter<E> {
        @Override
        public String toString(E value) {
            return EnumLabels.label(value);
        }

        @Override
        public E fromString(String string) {
            return null;
        }
    }
}
