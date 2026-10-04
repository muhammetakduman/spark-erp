package com.electrician.tracker.ui.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.GroupJobBreakdown;
import com.electrician.tracker.dto.ProductCatalog;
import com.electrician.tracker.dto.ProductOverview;
import com.electrician.tracker.dto.SupplierPrice;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.ProductCatalogService;
import com.electrician.tracker.service.ProductFilter;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.BulkDeleteFlow;
import com.electrician.tracker.ui.util.BulkSelection;
import com.electrician.tracker.ui.util.Debouncer;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EmptyState;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.FilterChoices;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.TaskRunner;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Ürünler": the catalog with purchase statistics. One search box (name and
 * brand, filtered while typing) plus category, brand and supplier filters
 * that work together; the material groups used on job lines on the left;
 * below, the selected product's detail. Choosing a group lists only the
 * products used in it and opens "Şantiye bazında dağılım" (the group's
 * spending per job) between the products and the product detail. Inside a
 * category the cheapest product comes first. Purchase columns and the
 * supplier filter exist only for users who may see purchase prices.
 */
@Component
@Scope("prototype")
public class ProductListController {

    private static final int DISTRIBUTION_INDEX = 1;
    private static final double[] DIVIDERS_WITH_DISTRIBUTION = { 0.26, 0.68 };

    private final ProductService productService;
    private final ProductCatalogService productCatalogService;
    private final AccessControl accessControl;
    private final ModalStageOpener modalStageOpener;
    private final TaskRunner taskRunner;

    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ComboBox<String> brandFilter;
    @FXML
    private ComboBox<String> supplierFilter;
    @FXML
    private Label resultLabel;
    @FXML
    private ListView<String> groupList;
    @FXML
    private SplitPane detailSplit;
    @FXML
    private TableView<ProductOverview> productTable;
    @FXML
    private ProductUsagePanelController usagePanelController;
    @FXML
    private VBox distributionPanel;
    @FXML
    private GroupDistributionPanelController distributionPanelController;

    private final ObservableList<ProductOverview> products = FXCollections.observableArrayList();
    private FilteredList<ProductOverview> filteredProducts;
    /** Products used in the chosen group; {@code null} while no group is chosen. */
    private Set<Long> groupProductIds;

    public ProductListController(ProductService productService, ProductCatalogService productCatalogService,
            AccessControl accessControl, ModalStageOpener modalStageOpener, TaskRunner taskRunner) {
        this.productService = productService;
        this.productCatalogService = productCatalogService;
        this.accessControl = accessControl;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
    }

    @FXML
    private void initialize() {
        products.clear();
        filteredProducts = new FilteredList<>(products);
        TableSorting.bindSorted(productTable, filteredProducts);
        productTable.getColumns().setAll(columns());
        if (accessControl.isAdmin()) {
            BulkSelection.forTable(productTable, overview -> overview.product().getId(), this::deleteSelected);
        }
        productTable.setPlaceholder(EmptyState.of(AppIcon.PRODUCTS, "product.empty",
                "product.action.new", this::onNew));
        boolean purchaseVisible = accessControl.canViewFinancials();
        supplierFilter.setVisible(purchaseVisible);
        supplierFilter.setManaged(purchaseVisible);

        Debouncer searchDebouncer = new Debouncer(Debouncer.TYPING_PAUSE, this::applyFilters);
        searchField.textProperty().addListener((obs, o, n) -> searchDebouncer.trigger());
        categoryFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        brandFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        supplierFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        groupList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> onGroupChosen(n));
        distributionPanelController.setOnLoaded(this::onDistributionLoaded);
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) ->
                usagePanelController.showProduct(selected == null ? null : selected.product()));
        refresh();
    }

    private List<TableColumn<ProductOverview, ?>> columns() {
        List<TableColumn<ProductOverview, ?>> columns = new ArrayList<>();
        columns.add(textColumn("product.field.name", overview -> nameWithState(overview.product())));
        columns.add(textColumn("product.field.brand", overview -> overview.product().getBrand()));
        columns.add(textColumn("product.field.category", overview -> overview.product().getCategory()));
        columns.add(textColumn("product.field.unit", overview -> EnumLabels.label(overview.product().getUnit())));
        TableColumn<ProductOverview, Integer> count = new TableColumn<>(DialogUtil.message("product.stats.count"));
        TableSorting.integer(count, ProductOverview::purchaseCount);
        TableColumn<ProductOverview, BigDecimal> quantity =
                new TableColumn<>(DialogUtil.message("product.stats.quantity"));
        TableSorting.number(quantity, ProductOverview::totalQuantity, Bicimlendirici::quantity);
        columns.addAll(List.of(count, quantity));
        if (accessControl.canViewFinancials()) {
            columns.add(supplierPriceColumn("product.stats.lowest", ProductOverview::lowestPurchase));
            columns.add(supplierPriceColumn("product.stats.highest", ProductOverview::highestPurchase));
            TableColumn<ProductOverview, BigDecimal> last = new TableColumn<>(DialogUtil.message("product.stats.last"));
            TableSorting.money(last, ProductOverview::lastPurchasePrice);
            TableColumn<ProductOverview, BigDecimal> expense =
                    new TableColumn<>(DialogUtil.message("product.stats.expense"));
            TableSorting.money(expense, ProductOverview::totalExpense);
            columns.addAll(List.of(last, expense));
        }
        return columns;
    }

    private static TableColumn<ProductOverview, String> textColumn(String titleKey,
            java.util.function.Function<ProductOverview, String> value) {
        TableColumn<ProductOverview, String> column = new TableColumn<>(DialogUtil.message(titleKey));
        TableSorting.text(column, value);
        return column;
    }

    /** "12,50 ₺ (Elektrik Market)", sorted by the price. */
    private static TableColumn<ProductOverview, SupplierPrice> supplierPriceColumn(String titleKey,
            java.util.function.Function<ProductOverview, SupplierPrice> value) {
        TableColumn<ProductOverview, SupplierPrice> column = new TableColumn<>(DialogUtil.message(titleKey));
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(value.apply(data.getValue())));
        column.setCellFactory(col -> new SupplierPriceCell());
        column.setComparator(Comparator.nullsLast(Comparator.comparing(SupplierPrice::unitPrice)));
        column.getStyleClass().add("money-cell");
        return column;
    }

    private void refresh() {
        taskRunner.run(productCatalogService::loadCatalog, this::showCatalog, loadingIndicator, productTable);
    }

    private void showCatalog(ProductCatalog catalog) {
        FilterChoices.setUp(categoryFilter, catalog.categories());
        FilterChoices.setUp(brandFilter, catalog.brands());
        FilterChoices.setUp(supplierFilter, catalog.suppliers());
        products.setAll(catalog.products());
        showGroups(catalog);
        applyFilters();
    }

    /** "Tüm gruplar", the groups A–Z, "Kategorisiz" last when some lines have no group; the choice is kept. */
    private void showGroups(ProductCatalog catalog) {
        String chosen = groupList.getSelectionModel().getSelectedItem();
        List<String> items = new ArrayList<>();
        items.add(allGroupsLabel());
        items.addAll(catalog.groups());
        if (catalog.hasUngroupedLines()) {
            items.add(uncategorizedLabel());
        }
        groupList.getItems().setAll(items);
        groupList.getSelectionModel().select(chosen != null && items.contains(chosen) ? chosen : allGroupsLabel());
    }

    private void onGroupChosen(String label) {
        boolean chosen = label != null && !label.equals(allGroupsLabel());
        showDistribution(chosen);
        if (!chosen) {
            groupProductIds = null;
            applyFilters();
            return;
        }
        distributionPanelController.showGroup(label.equals(uncategorizedLabel()) ? null : label);
    }

    /** The product list keeps only the products used in the chosen group (in the chosen dates). */
    private void onDistributionLoaded(GroupJobBreakdown breakdown) {
        groupProductIds = breakdown.productIds();
        applyFilters();
    }

    /** The distribution sits between the product list and the product detail while a group is chosen. */
    private void showDistribution(boolean shown) {
        boolean present = detailSplit.getItems().contains(distributionPanel);
        if (shown && !present) {
            detailSplit.getItems().add(DISTRIBUTION_INDEX, distributionPanel);
            detailSplit.setDividerPositions(DIVIDERS_WITH_DISTRIBUTION);
        } else if (!shown && present) {
            detailSplit.getItems().remove(distributionPanel);
        }
    }

    private static String allGroupsLabel() {
        return DialogUtil.message("product.groups.all");
    }

    private static String uncategorizedLabel() {
        return DialogUtil.message("material.group.uncategorized");
    }

    private void applyFilters() {
        if (filteredProducts == null) {
            return;
        }
        String category = FilterChoices.selected(categoryFilter);
        // Inside one category the cheapest product comes first; otherwise category, brand, name.
        FXCollections.sort(products, category == null ? TableSorting.productOverviews() : TableSorting.cheapestFirst());
        ProductFilter filter = new ProductFilter(searchField.getText(), category, FilterChoices.selected(brandFilter),
                FilterChoices.selected(supplierFilter));
        Set<Long> inGroup = groupProductIds;
        filteredProducts.setPredicate(overview -> filter.matches(overview)
                && (inGroup == null || inGroup.contains(overview.product().getId())));
        showResultLine(filter);
    }

    /** "24 ürün · Kategori: Kablo · Tedarikçi: Elektrik Market". */
    private void showResultLine(ProductFilter filter) {
        List<String> parts = new ArrayList<>();
        parts.add(DialogUtil.message("product.result.count", String.valueOf(filteredProducts.size())));
        addPart(parts, "product.result.search", filter.text());
        addPart(parts, "product.result.category", filter.category());
        addPart(parts, "product.result.brand", filter.brand());
        addPart(parts, "product.result.supplier", filter.supplier());
        String group = groupList.getSelectionModel().getSelectedItem();
        if (groupProductIds != null && group != null) {
            addPart(parts, "product.result.group", group);
        }
        resultLabel.setText(String.join(" · ", parts));
    }

    private static void addPart(List<String> parts, String key, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(DialogUtil.message(key, value.trim()));
        }
    }

    @FXML
    private void onClearFilters() {
        searchField.clear();
        FilterChoices.reset(categoryFilter);
        FilterChoices.reset(brandFilter);
        FilterChoices.reset(supplierFilter);
        groupList.getSelectionModel().select(allGroupsLabel());
        applyFilters();
    }

    @FXML
    private void onNew() {
        ProductFormController controller = modalStageOpener.openAndWait(ViewPaths.PRODUCT_FORM, "product.dialog.new",
                productTable.getScene().getWindow());
        if (controller.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onEdit() {
        Product selected = selectedProduct();
        if (selected == null) {
            return;
        }
        ProductFormController controller = modalStageOpener.openAndWait(ViewPaths.PRODUCT_FORM, "product.dialog.edit",
                productTable.getScene().getWindow(), c -> c.editExisting(selected));
        if (controller.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onDelete() {
        Product selected = selectedProduct();
        if (selected == null || !DialogUtil.confirm("product.confirm.delete")) {
            return;
        }
        try {
            productService.delete(selected.getId());
            refresh();
        } catch (ReferencedEntityException ex) {
            DialogUtil.showError(ex);
        }
    }

    /** "Kablo 3x2,5 (pasif)" for a product no longer offered in pickers. */
    private static String nameWithState(Product product) {
        return product.isActive() ? product.getName() : DialogUtil.message("common.inactiveName", product.getName());
    }

    /** "Seçilenleri Sil": products used on material or quote lines are skipped and can be made inactive. */
    private void deleteSelected(List<ProductOverview> overviews) {
        BulkDeleteFlow.of(overviews, overview -> overview.product().getId(),
                        overview -> overview.product().getDisplayName())
                .itemCount("bulk.count.products")
                .delete(productService::deleteAll)
                .deactivate(productService::deactivateAll)
                .afterwards(this::refresh)
                .run();
    }

    private Product selectedProduct() {
        ProductOverview selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return null;
        }
        return selected.product();
    }

    private static final class SupplierPriceCell extends TableCell<ProductOverview, SupplierPrice> {
        @Override
        protected void updateItem(SupplierPrice price, boolean empty) {
            super.updateItem(price, empty);
            if (empty || price == null) {
                setText(null);
                return;
            }
            String money = Bicimlendirici.money(price.unitPrice());
            setText(price.supplierName() == null ? money
                    : DialogUtil.message("productUsage.supplierPrice", price.supplierName(), money));
        }
    }
}
