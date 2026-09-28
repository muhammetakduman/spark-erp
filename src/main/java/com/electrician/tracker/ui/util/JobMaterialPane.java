package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.MaterialService;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Content of a job's "Malzemeler" tab: the material lines, category then
 * product name (double click or "Düzenle" to edit, "Sil" to delete). Purchase
 * price and supplier columns exist only for users who may see them; only an
 * ADMIN gets the delete button. Every change goes through
 * {@link MaterialService}; the caller refreshes the job.
 */
public final class JobMaterialPane {

    private static final double ROW_HEIGHT = 28;
    private static final double HEADER_HEIGHT = 40;
    private static final double MAX_TABLE_HEIGHT = 220;
    private static final int DOUBLE_CLICK = 2;
    private static final double SPACING = 10;

    private final MaterialService materialService;
    private final AccessControl accessControl;
    private final Consumer<Long> onChanged;
    private final Consumer<MaterialItem> onEdit;

    /** {@code onChanged} receives the job id after a delete; {@code onEdit} opens the edit dialog. */
    public JobMaterialPane(MaterialService materialService, AccessControl accessControl, Consumer<Long> onChanged,
            Consumer<MaterialItem> onEdit) {
        this.materialService = materialService;
        this.accessControl = accessControl;
        this.onChanged = onChanged;
        this.onEdit = onEdit;
    }

    public VBox build(Long jobId, List<MaterialItem> materials) {
        TableView<MaterialItem> table = buildTable(materials);
        Button editButton = new Button(DialogUtil.message("jobDetail.action.editMaterial"));
        editButton.setOnAction(e -> editSelected(table));
        HBox buttons = new HBox(SPACING, editButton);
        if (accessControl.isAdmin()) {
            Button deleteButton = new Button(DialogUtil.message("jobDetail.action.deleteMaterial"));
            deleteButton.getStyleClass().add("danger-button");
            deleteButton.setOnAction(e -> deleteSelected(table, jobId));
            buttons.getChildren().add(deleteButton);
        }
        buttons.getStyleClass().add("table-actions");
        return new VBox(SPACING, table, buttons);
    }

    private TableView<MaterialItem> buildTable(List<MaterialItem> materials) {
        TableView<MaterialItem> table = new TableView<>(TableSorting.sorted(materials, TableSorting.materialLines()));
        table.getColumns().setAll(columns());
        table.setPlaceholder(new Label(DialogUtil.message("material.empty")));
        table.setRowFactory(tv -> {
            TableRow<MaterialItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == DOUBLE_CLICK && !row.isEmpty()) {
                    onEdit.accept(row.getItem());
                }
            });
            return row;
        });
        table.setPrefHeight(Math.min(MAX_TABLE_HEIGHT, HEADER_HEIGHT + Math.max(materials.size(), 1) * ROW_HEIGHT));
        return table;
    }

    private List<TableColumn<MaterialItem, ?>> columns() {
        List<TableColumn<MaterialItem, ?>> columns = new ArrayList<>();
        TableColumn<MaterialItem, LocalDate> date = new TableColumn<>(DialogUtil.message("material.field.date"));
        TableSorting.date(date, MaterialItem::getItemDate);
        TableColumn<MaterialItem, String> category = new TableColumn<>(DialogUtil.message("product.field.category"));
        TableSorting.text(category, m -> m.getProduct().getCategory());
        TableColumn<MaterialItem, String> product = new TableColumn<>(DialogUtil.message("material.field.product"));
        TableSorting.text(product, m -> m.getProduct().getDisplayName());
        TableColumn<MaterialItem, BigDecimal> quantity = new TableColumn<>(DialogUtil.message("material.field.quantity"));
        TableSorting.number(quantity, MaterialItem::getQuantity, value -> Bicimlendirici.quantity(value));
        columns.addAll(List.of(date, category, product, quantity));
        if (accessControl.canViewFinancials()) {
            columns.addAll(purchaseColumns());
        }
        TableColumn<MaterialItem, BigDecimal> sale = new TableColumn<>(DialogUtil.message("material.field.salePrice"));
        TableSorting.money(sale, MaterialItem::getSaleUnitPrice);
        columns.addAll(List.of(sale, MaterialColumns.vatColumn(), MaterialColumns.totalColumn()));
        return columns;
    }

    private static List<TableColumn<MaterialItem, ?>> purchaseColumns() {
        TableColumn<MaterialItem, BigDecimal> purchase =
                new TableColumn<>(DialogUtil.message("material.field.purchasePrice"));
        TableSorting.money(purchase, MaterialItem::getPurchaseUnitPriceTl);
        purchase.setCellFactory(column -> new PurchasePriceCell<>(item -> item.isForeignCurrencyPurchase()
                ? new PurchasePriceCell.ForeignAmount(item.getPurchaseUnitPrice(), item.getPurchaseCurrency(),
                        item.getPurchaseExchangeRate())
                : null));
        TableColumn<MaterialItem, String> purchaseVat =
                new TableColumn<>(DialogUtil.message("material.field.purchaseVat"));
        TableSorting.text(purchaseVat, m -> m.getPurchaseUnitPrice() == null ? ""
                : VatLabels.describe(m.getPurchaseVatRate(), m.getPurchaseVatIncluded()));
        TableColumn<MaterialItem, String> supplier = new TableColumn<>(DialogUtil.message("material.field.supplier"));
        TableSorting.text(supplier, MaterialItem::getSupplierName);
        return List.of(purchase, purchaseVat, supplier);
    }

    private void editSelected(TableView<MaterialItem> table) {
        MaterialItem selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        onEdit.accept(selected);
    }

    private void deleteSelected(TableView<MaterialItem> table, Long jobId) {
        MaterialItem selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        String description = DialogUtil.message("delete.single.material", selected.getProduct().getDisplayName(),
                Bicimlendirici.quantity(selected.getQuantity()), EnumLabels.label(selected.getProduct().getUnit()));
        if (!DeleteConfirmation.confirmSingle(description)) {
            return;
        }
        try {
            materialService.delete(selected.getId());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        onChanged.accept(jobId);
    }
}
