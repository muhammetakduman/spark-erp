package com.electrician.tracker.ui.controller;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.ProductUsageReport;
import com.electrician.tracker.dto.ProductUsageRow;
import com.electrician.tracker.dto.ProductUsageSummary;
import com.electrician.tracker.dto.SupplierPriceComparison;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.PriceHistoryService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.JobNavigator;
import com.electrician.tracker.ui.util.PurchasePriceCell;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.TaskRunner;
import com.electrician.tracker.ui.util.VatLabels;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The product detail under the product list: a two-line summary, then two
 * tabs — every use of the product (newest first) and the supplier price
 * comparison (cheapest average first and highlighted); with a date range and
 * Excel export. Purchase prices and suppliers are not built
 * at all for users who may not see them. Double click opens the job.
 */
@Component
@Scope("prototype")
public class ProductUsagePanelController {

    private static final double BADGE_SPACING = 6;
    private static final int DOUBLE_CLICK = 2;
    private static final String CHEAPEST_ROW_STYLE = "cheapest-row";

    private final PriceHistoryService priceHistoryService;
    private final AccessControl accessControl;
    private final TaskRunner taskRunner;
    private final JobNavigator jobNavigator;

    @FXML
    private Label usageTitleLabel;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private VBox summaryBox;
    @FXML
    private TabPane detailTabs;
    @FXML
    private Tab supplierTab;
    @FXML
    private TableView<SupplierPriceComparison> supplierTable;
    @FXML
    private TableView<ProductUsageRow> usageTable;

    private Product product;

    public ProductUsagePanelController(PriceHistoryService priceHistoryService, AccessControl accessControl,
            TaskRunner taskRunner, JobNavigator jobNavigator) {
        this.priceHistoryService = priceHistoryService;
        this.accessControl = accessControl;
        this.taskRunner = taskRunner;
        this.jobNavigator = jobNavigator;
    }

    @FXML
    private void initialize() {
        boolean purchaseVisible = accessControl.canViewFinancials();
        if (purchaseVisible) {
            setUpSupplierTable();
        } else {
            detailTabs.getTabs().remove(supplierTab);
        }
        setUpUsageTable(purchaseVisible);
        fromDatePicker.valueProperty().addListener((obs, o, n) -> reload());
        toDatePicker.valueProperty().addListener((obs, o, n) -> reload());
        showProduct(null);
    }

    /** Shows the detail of {@code selected} ({@code null} clears the panel). */
    public void showProduct(Product selected) {
        this.product = selected;
        reload();
    }

    private void reload() {
        if (product == null) {
            usageTitleLabel.setText(DialogUtil.message("productUsage.title"));
            usageTable.getItems().clear();
            supplierTable.getItems().clear();
            summaryBox.getChildren().setAll(new Label(DialogUtil.message("productUsage.summary.none")));
            return;
        }
        Product shown = product;
        usageTitleLabel.setText(shown.getDisplayName());
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        taskRunner.run(() -> priceHistoryService.usageReport(shown.getId(), from, to),
                report -> show(report, shown), loadingIndicator, usageTable);
    }

    private void show(ProductUsageReport report, Product shown) {
        usageTable.setItems(TableSorting.sorted(report.rows(), TableSorting.usageRows()));
        supplierTable.setItems(TableSorting.sorted(report.suppliers(), TableSorting.supplierComparison()));
        showSummary(report.summary(), shown);
    }

    private void showSummary(ProductUsageSummary summary, Product shown) {
        if (summary.usageCount() == 0) {
            summaryBox.getChildren().setAll(new Label(DialogUtil.message("productUsage.empty")));
            return;
        }
        List<Label> lines = new ArrayList<>();
        lines.add(new Label(DialogUtil.message("productUsage.summary.usage", String.valueOf(summary.usageCount()),
                Bicimlendirici.quantity(summary.totalQuantity()), EnumLabels.label(shown.getUnit()),
                summary.lastCustomerName(), Bicimlendirici.date(summary.lastDate()))));
        lines.add(new Label(accessControl.canViewFinancials()
                ? DialogUtil.message("productUsage.summary.prices", moneyOrDash(summary.lastSaleUnitPrice()),
                        moneyOrDash(summary.lastPurchaseUnitPrice()), moneyOrDash(summary.totalCost()))
                : DialogUtil.message("productUsage.summary.salePrice", moneyOrDash(summary.lastSaleUnitPrice()))));
        summaryBox.getChildren().setAll(lines);
    }

    private static String moneyOrDash(BigDecimal value) {
        return value == null ? "—" : Bicimlendirici.money(value);
    }

    private void setUpSupplierTable() {
        TableColumn<SupplierPriceComparison, String> supplier =
                new TableColumn<>(DialogUtil.message("material.field.supplier"));
        TableSorting.text(supplier, SupplierPriceComparison::supplierName);
        TableColumn<SupplierPriceComparison, Integer> count =
                new TableColumn<>(DialogUtil.message("productUsage.suppliers.count"));
        TableSorting.integer(count, SupplierPriceComparison::purchaseCount);
        TableColumn<SupplierPriceComparison, BigDecimal> lowest =
                new TableColumn<>(DialogUtil.message("productUsage.suppliers.lowest"));
        TableSorting.money(lowest, SupplierPriceComparison::lowestPrice);
        TableColumn<SupplierPriceComparison, BigDecimal> highest =
                new TableColumn<>(DialogUtil.message("productUsage.suppliers.highest"));
        TableSorting.money(highest, SupplierPriceComparison::highestPrice);
        TableColumn<SupplierPriceComparison, BigDecimal> average =
                new TableColumn<>(DialogUtil.message("productUsage.suppliers.average"));
        TableSorting.money(average, SupplierPriceComparison::averagePrice);
        TableColumn<SupplierPriceComparison, LocalDate> last =
                new TableColumn<>(DialogUtil.message("productUsage.suppliers.lastDate"));
        TableSorting.date(last, SupplierPriceComparison::lastPurchaseDate);
        supplierTable.getColumns().setAll(List.of(supplier, count, lowest, highest, average, last));
        supplierTable.setPlaceholder(new Label(DialogUtil.message("productUsage.summary.noSupplier")));
        supplierTable.setRowFactory(table -> new CheapestRow());
    }

    private void setUpUsageTable(boolean purchaseVisible) {
        List<TableColumn<ProductUsageRow, ?>> columns = new ArrayList<>();
        TableColumn<ProductUsageRow, LocalDate> date = new TableColumn<>(DialogUtil.message("productUsage.field.date"));
        TableSorting.date(date, ProductUsageRow::date);
        TableColumn<ProductUsageRow, String> customer =
                new TableColumn<>(DialogUtil.message("productUsage.field.customer"));
        TableSorting.text(customer, ProductUsageRow::customerName);
        TableColumn<ProductUsageRow, ProductUsageRow> job = new TableColumn<>(DialogUtil.message("productUsage.field.job"));
        job.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        job.setCellFactory(column -> new JobCell());
        job.setComparator(java.util.Comparator.comparing(ProductUsageRow::jobName, TableSorting.turkishText()));
        TableColumn<ProductUsageRow, BigDecimal> quantity =
                new TableColumn<>(DialogUtil.message("productUsage.field.quantity"));
        TableSorting.number(quantity, ProductUsageRow::quantity, Bicimlendirici::quantity);
        columns.addAll(List.of(date, customer, job, quantity));
        if (purchaseVisible) {
            TableColumn<ProductUsageRow, BigDecimal> purchase =
                    new TableColumn<>(DialogUtil.message("productUsage.field.purchasePrice"));
            TableSorting.money(purchase, ProductUsageRow::purchaseUnitPrice);
            purchase.setCellFactory(column -> new PurchasePriceCell<>(row -> row.isForeignCurrencyPurchase()
                    ? new PurchasePriceCell.ForeignAmount(row.purchaseOriginalUnitPrice(), row.purchaseCurrency(),
                            row.purchaseExchangeRate())
                    : null));
            TableColumn<ProductUsageRow, String> supplier =
                    new TableColumn<>(DialogUtil.message("productUsage.field.supplier"));
            TableSorting.text(supplier, ProductUsageRow::supplierName);
            columns.addAll(List.of(purchase, supplier));
        }
        TableColumn<ProductUsageRow, BigDecimal> sale =
                new TableColumn<>(DialogUtil.message("productUsage.field.salePrice"));
        TableSorting.money(sale, ProductUsageRow::saleUnitPrice);
        TableColumn<ProductUsageRow, String> vat = new TableColumn<>(DialogUtil.message("productUsage.field.vat"));
        TableSorting.text(vat, row -> VatLabels.describe(row.vatRate(), row.vatIncluded()));
        TableColumn<ProductUsageRow, BigDecimal> total = new TableColumn<>(DialogUtil.message("productUsage.field.total"));
        TableSorting.money(total, ProductUsageRow::saleTotal);
        columns.addAll(List.of(sale, vat, total));
        usageTable.getColumns().setAll(columns);
        usageTable.setPlaceholder(new Label(DialogUtil.message("productUsage.empty")));
        usageTable.setRowFactory(table -> {
            TableRow<ProductUsageRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == DOUBLE_CLICK && !row.isEmpty()) {
                    jobNavigator.open(row.getItem().jobId(), row.getItem().jobType());
                }
            });
            return row;
        });
    }

    @FXML
    private void onClearDates() {
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
    }

    @FXML
    private void onExport() {
        if (product == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message("productUsage.export.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName(product.getDisplayName() + ".xlsx");
        File selected = chooser.showSaveDialog(usageTable.getScene().getWindow());
        if (selected == null) {
            return;
        }
        Product exported = product;
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        taskRunner.run(() -> {
            priceHistoryService.exportUsage(exported, from, to, selected.toPath());
            return selected;
        }, file -> DialogUtil.showInfo("productUsage.export.success"), loadingIndicator, usageTable);
    }

    /** The cheapest supplier (first by the default order) is highlighted. */
    private final class CheapestRow extends TableRow<SupplierPriceComparison> {
        @Override
        protected void updateItem(SupplierPriceComparison item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove(CHEAPEST_ROW_STYLE);
            if (!empty && item != null && isCheapest(item)) {
                getStyleClass().add(CHEAPEST_ROW_STYLE);
            }
        }

        private boolean isCheapest(SupplierPriceComparison item) {
            return supplierTable.getItems().stream()
                    .map(SupplierPriceComparison::averagePrice)
                    .min(java.util.Comparator.naturalOrder())
                    .map(lowest -> lowest.compareTo(item.averagePrice()) == 0)
                    .orElse(false);
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
