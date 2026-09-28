package com.electrician.tracker.ui.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.CurrencyCode;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.ExchangeRate;
import com.electrician.tracker.dto.PriceSuggestion;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.CurrencyConverter;
import com.electrician.tracker.service.ExchangeRateService;
import com.electrician.tracker.service.MaterialPriceCalculator;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.PriceHistoryService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.MaterialColumns;
import com.electrician.tracker.ui.util.ProductSelector;
import com.electrician.tracker.ui.util.SuggestionPicker;
import com.electrician.tracker.ui.util.VatSelector;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The one material entry dialog used by both site and service jobs. With a
 * job set it saves each line immediately; in draft mode (service form, job
 * not created yet) it only validates the lines and hands them back. For a
 * MANAGER the purchase price and supplier fields do not exist at all. A
 * purchase price can be entered in dollars or euros: the rate field (with
 * the last rate as suggestion) and the live lira value appear; the sale
 * price is always in lira.
 */
@Component
@Scope("prototype")
public class MaterialDialogController {

    private final MaterialService materialService;
    private final ProductService productService;
    private final PriceHistoryService priceHistoryService;
    private final AccessControl accessControl;
    private final ExchangeRateService exchangeRateService;

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
    private VBox productHolder;
    @FXML
    private DecimalField quantityField;
    @FXML
    private DecimalField unitPriceField;
    @FXML
    private DecimalField totalPriceField;
    @FXML
    private Label purchaseLabel;
    @FXML
    private Node purchaseRow;
    @FXML
    private DecimalField purchasePriceField;
    @FXML
    private ComboBox<CurrencyCode> currencyComboBox;
    @FXML
    private Label rateLabel;
    @FXML
    private Node rateRow;
    @FXML
    private DecimalField exchangeRateField;
    @FXML
    private Label conversionLabel;
    @FXML
    private Label supplierLabel;
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
    private ProductSelector productSelector;
    private SuggestionPicker supplierPicker;
    private Job job;
    private LocalDate draftDate;
    private boolean anyAdded;
    private PriceEntryType leadingPriceField = PriceEntryType.UNIT;
    private boolean syncingPrices;
    private PriceSuggestion appliedSuggestion = PriceSuggestion.empty();
    private Long editingId;
    private boolean loadingExisting;

    public MaterialDialogController(MaterialService materialService, ProductService productService,
            PriceHistoryService priceHistoryService, AccessControl accessControl,
            ExchangeRateService exchangeRateService) {
        this.materialService = materialService;
        this.productService = productService;
        this.priceHistoryService = priceHistoryService;
        this.accessControl = accessControl;
        this.exchangeRateService = exchangeRateService;
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
        productSelector.select(item.getProduct());
        quantityField.setValue(item.getQuantity());
        if (item.getPriceEntryType() == PriceEntryType.TOTAL) {
            totalPriceField.setValue(item.getSaleTotalAmount());
        } else {
            unitPriceField.setValue(item.getSaleUnitPrice());
        }
        purchasePriceField.setValue(item.getPurchaseUnitPrice());
        currencyComboBox.setValue(CurrencyConverter.orLira(item.getPurchaseCurrency()));
        exchangeRateField.setValue(item.isForeignCurrencyPurchase() ? item.getPurchaseExchangeRate() : null);
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
        productSelector = new ProductSelector(productService);
        productSelector.setOnProductChanged(this::applySuggestion);
        productHolder.getChildren().setAll(productSelector.view());
        supplierPicker = new SuggestionPicker(supplierComboBox, materialService.findDistinctSupplierNames());
        setUpCurrency();
        showPurchaseFields(accessControl.canViewFinancials());
        setUpPriceSync();
        setUpAddedTable();
        vatSelector.bindAmount(totalPriceField);
        purchaseVatSelector.bindAmount(purchasePriceField);
    }

    /** Purchase price and supplier only exist for users who may see them (not merely disabled). */
    private void showPurchaseFields(boolean shown) {
        for (Node node : List.of(purchaseLabel, purchaseRow, supplierLabel, supplierComboBox)) {
            setShown(node, shown);
        }
        refreshRateRow();
    }

    /** ₺ / $ / € next to the purchase price; lira by default. */
    private void setUpCurrency() {
        currencyComboBox.getItems().setAll(CurrencyCode.values());
        currencyComboBox.setConverter(new CurrencyConverterLabel());
        currencyComboBox.setValue(CurrencyCode.TRY);
        currencyComboBox.valueProperty().addListener((obs, old, currency) -> onCurrencyChanged(currency));
        purchasePriceField.textProperty().addListener((obs, o, n) -> refreshConversion());
        exchangeRateField.textProperty().addListener((obs, o, n) -> refreshConversion());
    }

    /** A foreign currency opens the rate field, pre-filled with the last known rate. */
    private void onCurrencyChanged(CurrencyCode currency) {
        if (currency != null && currency.isForeign() && !loadingExisting) {
            exchangeRateField.setValue(exchangeRateService.find(currency)
                    .map(ExchangeRate::rate).orElse(null));
        }
        refreshRateRow();
        refreshConversion();
    }

    private void refreshRateRow() {
        boolean shown = accessControl.canViewFinancials() && currencyComboBox.getValue() != null
                && currencyComboBox.getValue().isForeign();
        setShown(rateLabel, shown);
        setShown(rateRow, shown);
    }

    /** "120,00 $ × 34,25 = 4.110,00 ₺" while typing. */
    private void refreshConversion() {
        CurrencyCode currency = currencyComboBox.getValue();
        try {
            BigDecimal price = purchasePriceField.getValue();
            BigDecimal rate = exchangeRateField.getValue();
            if (currency == null || !currency.isForeign() || price == null || rate == null || rate.signum() <= 0) {
                conversionLabel.setText("");
                return;
            }
            conversionLabel.setText(DialogUtil.message("material.conversion", Bicimlendirici.money(price, currency),
                    Bicimlendirici.exchangeRate(rate), Bicimlendirici.money(CurrencyConverter.toLira(price, currency,
                            rate))));
        } catch (NumberFormatException e) {
            conversionLabel.setText("");
        }
    }

    private void setUpPriceSync() {
        unitPriceField.textProperty().addListener((obs, o, n) -> onPriceEdited(PriceEntryType.UNIT));
        totalPriceField.textProperty().addListener((obs, o, n) -> onPriceEdited(PriceEntryType.TOTAL));
        quantityField.textProperty().addListener((obs, o, n) -> syncDerivedPrice());
    }

    private void setUpAddedTable() {
        addedTable.setItems(addedItems);
        MaterialColumns.bindProduct(addedProductColumn);
        addedQuantityColumn.setCellValueFactory(d -> new SimpleStringProperty(
                Bicimlendirici.quantity(d.getValue().getQuantity()) + " "
                        + EnumLabels.label(d.getValue().getProduct().getUnit())));
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
        if (next.lastPurchaseUnitPrice() != null) {
            currencyComboBox.setValue(CurrencyConverter.orLira(next.lastPurchaseCurrency()));
        }
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
    private void onSaveAndNew() {
        if (saveCurrentItem()) {
            clearItemFields();
            productSelector.requestFocus();
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
                productSelector.resolveSelection().orElse(null),
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
        CurrencyCode currency = CurrencyConverter.orLira(currencyComboBox.getValue());
        item.setPurchaseCurrency(currency, currency.isForeign() ? exchangeRateField.getValue() : null, null);
        return item;
    }

    private boolean isDraftMode() {
        return job == null;
    }

    private void clearItemFields() {
        productSelector.clear();
        quantityField.clear();
        leadingPriceField = PriceEntryType.UNIT;
        unitPriceField.clear();
        totalPriceField.clear();
        purchasePriceField.clear();
        currencyComboBox.setValue(CurrencyCode.TRY);
        exchangeRateField.clear();
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

    /** "₺ TL", "$ USD", "€ EUR". */
    private static final class CurrencyConverterLabel extends StringConverter<CurrencyCode> {
        @Override
        public String toString(CurrencyCode currency) {
            return currency == null ? "" : Bicimlendirici.currencySymbol(currency) + " " + currency.name();
        }

        @Override
        public CurrencyCode fromString(String text) {
            return null;
        }
    }
}
