package com.electrician.tracker.ui.controller;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.ProductUsageReport;
import com.electrician.tracker.dto.ProductUsageRow;
import com.electrician.tracker.dto.ProductUsageSummary;
import com.electrician.tracker.service.MetinKarsilastirici;
import com.electrician.tracker.service.PriceHistoryService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.ContentNavigator;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.TaskRunner;
import com.electrician.tracker.ui.util.VatLabels;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.springframework.stereotype.Component;

/**
 * Product catalog plus "Ürün Kullanım Geçmişi": for the selected product,
 * to whom, when and at what price it was given, with a summary. Double
 * clicking a usage row opens that job on the main screen.
 */
@Component
public class ProductListController {

    private static final String FORM_FXML = "/fxml/product_form.fxml";
    private static final String HOME_FXML = "/fxml/home_screen.fxml";
    private static final int DOUBLE_CLICK = 2;
    private static final double BADGE_SPACING = 6;

    private final ProductService productService;
    private final PriceHistoryService priceHistoryService;
    private final ModalStageOpener modalStageOpener;
    private final TaskRunner taskRunner;
    private final ContentNavigator contentNavigator;

    @FXML
    private TextField searchField;
    @FXML
    private TableView<Product> table;
    @FXML
    private TableColumn<Product, String> nameColumn;
    @FXML
    private TableColumn<Product, String> unitColumn;
    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Label usageTitleLabel;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private ProgressIndicator usageLoadingIndicator;
    @FXML
    private TableView<ProductUsageRow> usageTable;
    @FXML
    private TableColumn<ProductUsageRow, String> usageDateColumn;
    @FXML
    private TableColumn<ProductUsageRow, String> usageCustomerColumn;
    @FXML
    private TableColumn<ProductUsageRow, ProductUsageRow> usageJobColumn;
    @FXML
    private TableColumn<ProductUsageRow, String> usageQuantityColumn;
    @FXML
    private TableColumn<ProductUsageRow, String> usagePurchaseColumn;
    @FXML
    private TableColumn<ProductUsageRow, String> usageSupplierColumn;
    @FXML
    private TableColumn<ProductUsageRow, String> usageSaleColumn;
    @FXML
    private TableColumn<ProductUsageRow, String> usageVatColumn;
    @FXML
    private TableColumn<ProductUsageRow, String> usageTotalColumn;
    @FXML
    private VBox usageSummaryBox;

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final FilteredList<Product> filteredProducts = new FilteredList<>(products);

    public ProductListController(ProductService productService, PriceHistoryService priceHistoryService,
            ModalStageOpener modalStageOpener, TaskRunner taskRunner, ContentNavigator contentNavigator) {
        this.productService = productService;
        this.priceHistoryService = priceHistoryService;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
        this.contentNavigator = contentNavigator;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        unitColumn.setCellValueFactory(data -> new SimpleStringProperty(EnumLabels.label(data.getValue().getUnit())));
        table.setItems(filteredProducts);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applySearch(newValue));

        setUpUsageColumns();
        usageTable.setPlaceholder(new Label(DialogUtil.message("productUsage.empty")));
        usageTable.setRowFactory(tv -> {
            TableRow<ProductUsageRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == DOUBLE_CLICK && !row.isEmpty()) {
                    openJob(row.getItem());
                }
            });
            return row;
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadUsage());
        fromDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> loadUsage());
        toDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> loadUsage());

        showSummary(ProductUsageSummary.empty(), null);
        refresh();
    }

    private void setUpUsageColumns() {
        usageDateColumn.setCellValueFactory(text(row -> Bicimlendirici.date(row.date())));
        usageCustomerColumn.setCellValueFactory(text(ProductUsageRow::customerName));
        usageJobColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        usageJobColumn.setCellFactory(column -> new JobCell());
        usageQuantityColumn.setCellValueFactory(text(row -> Bicimlendirici.quantity(row.quantity())));
        usagePurchaseColumn.setCellValueFactory(text(row -> priceWithVat(row.purchaseUnitPrice(),
                row.purchaseVatRate(), row.purchaseVatIncluded())));
        usageSupplierColumn.setCellValueFactory(text(ProductUsageRow::supplierName));
        usageSaleColumn.setCellValueFactory(text(row -> Bicimlendirici.money(row.saleUnitPrice())));
        usageVatColumn.setCellValueFactory(text(row -> VatLabels.describe(row.vatRate(), row.vatIncluded())));
        usageTotalColumn.setCellValueFactory(text(row -> Bicimlendirici.money(row.saleTotal())));
    }

    private static <T> Callback<CellDataFeatures<T, String>, ObservableValue<String>> text(Function<T, String> value) {
        return data -> new SimpleStringProperty(value.apply(data.getValue()));
    }

    /** "10,00 ₺ · %20 hariç"; the VAT part is left out when the price has none. */
    private static String priceWithVat(BigDecimal price, Integer vatRate, Boolean vatIncluded) {
        if (price == null || vatRate == null) {
            return Bicimlendirici.money(price);
        }
        return Bicimlendirici.money(price) + " · " + VatLabels.describe(vatRate, vatIncluded);
    }

    private void applySearch(String query) {
        String normalizedQuery = MetinKarsilastirici.normalize(query);
        filteredProducts.setPredicate(product -> normalizedQuery.isEmpty()
                || MetinKarsilastirici.normalize(product.getName()).contains(normalizedQuery));
    }

    private void refresh() {
        taskRunner.run(productService::findAll, this::showProducts, loadingIndicator, table);
    }

    private void showProducts(List<Product> loaded) {
        products.setAll(loaded);
        applySearch(searchField.getText());
    }

    private void loadUsage() {
        Product product = table.getSelectionModel().getSelectedItem();
        if (product == null) {
            usageTitleLabel.setText(DialogUtil.message("productUsage.title"));
            usageTable.getItems().clear();
            showSummary(ProductUsageSummary.empty(), null);
            return;
        }
        usageTitleLabel.setText(DialogUtil.message("productUsage.titleFor", product.getName()));
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        taskRunner.run(() -> priceHistoryService.usageReport(product.getId(), from, to),
                report -> showUsage(report, product), usageLoadingIndicator, usageTable);
    }

    private void showUsage(ProductUsageReport report, Product product) {
        usageTable.setItems(FXCollections.observableArrayList(report.rows()));
        showSummary(report.summary(), product);
    }

    private void showSummary(ProductUsageSummary summary, Product product) {
        if (summary.usageCount() == 0) {
            usageSummaryBox.getChildren().setAll(new Label(DialogUtil.message("productUsage.summary.none")));
            return;
        }
        usageSummaryBox.getChildren().setAll(
                new Label(DialogUtil.message("productUsage.summary.count", String.valueOf(summary.usageCount()),
                        Bicimlendirici.quantity(summary.totalQuantity()), EnumLabels.label(product.getUnit()))),
                new Label(DialogUtil.message("productUsage.summary.last", summary.lastCustomerName(),
                        Bicimlendirici.date(summary.lastDate()))),
                new Label(DialogUtil.message("productUsage.summary.salePrices", moneyOrDash(summary.minSaleUnitPrice()),
                        moneyOrDash(summary.maxSaleUnitPrice()), moneyOrDash(summary.lastSaleUnitPrice()))),
                new Label(summary.cheapestSupplierName() == null
                        ? DialogUtil.message("productUsage.summary.noSupplier")
                        : DialogUtil.message("productUsage.summary.cheapestSupplier", summary.cheapestSupplierName(),
                                Bicimlendirici.money(summary.cheapestPurchaseUnitPrice()))));
    }

    private static String moneyOrDash(BigDecimal value) {
        return value == null ? "—" : Bicimlendirici.money(value);
    }

    private void openJob(ProductUsageRow row) {
        contentNavigator.<HomeController>show(HOME_FXML, home -> home.focusJob(row.jobId()));
    }

    @FXML
    private void onClearDates() {
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
    }

    @FXML
    private void onExport() {
        Product product = table.getSelectionModel().getSelectedItem();
        if (product == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message("productUsage.export.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName(product.getName() + ".xlsx");
        File selected = chooser.showSaveDialog(table.getScene().getWindow());
        if (selected == null) {
            return;
        }
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        taskRunner.run(() -> {
            priceHistoryService.exportUsage(product, from, to, selected.toPath());
            return selected;
        }, file -> DialogUtil.showInfo("productUsage.export.success"), usageLoadingIndicator, usageTable);
    }

    @FXML
    private void onNew() {
        ProductFormController controller = modalStageOpener.openAndWait(
                FORM_FXML, "product.dialog.new", table.getScene().getWindow());
        if (controller.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onEdit() {
        Product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        ProductFormController controller = modalStageOpener.openAndWait(
                FORM_FXML, "product.dialog.edit", table.getScene().getWindow(),
                c -> c.editExisting(selected));
        if (controller.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onDelete() {
        Product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        if (!DialogUtil.confirm("product.confirm.delete")) {
            return;
        }
        try {
            productService.delete(selected.getId());
            refresh();
        } catch (ReferencedEntityException ex) {
            DialogUtil.showError(ex);
        }
    }

    /** "ŞANTİYE" / "SERVİS" badge followed by the job name. */
    private static final class JobCell extends TableCell<ProductUsageRow, ProductUsageRow> {
        @Override
        protected void updateItem(ProductUsageRow row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setGraphic(null);
                return;
            }
            boolean site = row.jobType() == JobType.SITE;
            Label badge = new Label(DialogUtil.message(
                    site ? "employeeAttendance.badge.site" : "employeeAttendance.badge.service"));
            badge.getStyleClass().addAll("badge", site ? "badge-site" : "badge-service");
            Label name = new Label(row.jobName() == null ? "" : row.jobName());
            setGraphic(new HBox(BADGE_SPACING, badge, name));
        }
    }
}
