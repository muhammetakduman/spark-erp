package com.electrician.tracker.ui.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.service.MaterialService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Content of a job's "Malzemeler" tab: the material lines (double click or
 * "Düzenle" to edit, "Sil" to delete). The job's totals and VAT split are
 * shown once above the tabs. Every change goes through
 * {@link MaterialService}; the caller refreshes the job.
 */
public final class JobMaterialPane {

    private static final double ROW_HEIGHT = 28;
    private static final double HEADER_HEIGHT = 40;
    private static final double MAX_TABLE_HEIGHT = 220;
    private static final int DOUBLE_CLICK = 2;
    private static final double SPACING = 10;

    private final MaterialService materialService;
    private final Consumer<Long> onChanged;
    private final Consumer<MaterialItem> onEdit;

    /** {@code onChanged} receives the job id after a delete; {@code onEdit} opens the edit dialog. */
    public JobMaterialPane(MaterialService materialService, Consumer<Long> onChanged, Consumer<MaterialItem> onEdit) {
        this.materialService = materialService;
        this.onChanged = onChanged;
        this.onEdit = onEdit;
    }

    public VBox build(Long jobId, List<MaterialItem> materials) {
        TableView<MaterialItem> table = buildTable(materials);
        Button editButton = new Button(DialogUtil.message("jobDetail.action.editMaterial"));
        editButton.setOnAction(e -> editSelected(table));
        Button deleteButton = new Button(DialogUtil.message("jobDetail.action.deleteMaterial"));
        deleteButton.setOnAction(e -> deleteSelected(table, jobId));
        HBox buttons = new HBox(SPACING, editButton, deleteButton);
        buttons.getStyleClass().add("table-actions");
        return new VBox(SPACING, table, buttons);
    }

    private TableView<MaterialItem> buildTable(List<MaterialItem> materials) {
        TableView<MaterialItem> table = new TableView<>(FXCollections.observableArrayList(materials));
        table.getColumns().setAll(List.of(
                column("material.field.date", m -> Bicimlendirici.date(m.getItemDate())),
                column("material.field.product", m -> m.getProduct().getName()),
                column("material.field.quantity", m -> Bicimlendirici.quantity(m.getQuantity()) + " "
                        + EnumLabels.label(m.getProduct().getUnit())),
                column("material.field.purchasePrice", JobMaterialPane::priceWithVat),
                column("material.field.supplier", m -> m.getSupplierName() == null ? "" : m.getSupplierName()),
                column("material.field.salePrice", m -> Bicimlendirici.money(m.getSaleUnitPrice())),
                MaterialColumns.vatColumn(),
                MaterialColumns.totalColumn()));
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

    /** Purchase price with its own VAT, e.g. "80,00 ₺ · %20 hariç". */
    private static String priceWithVat(MaterialItem item) {
        String price = Bicimlendirici.money(item.getPurchaseUnitPrice());
        if (item.getPurchaseUnitPrice() == null || item.getPurchaseVatRate() == null) {
            return price;
        }
        return price + " · " + VatLabels.describe(item.getPurchaseVatRate(), item.getPurchaseVatIncluded());
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
        if (!DialogUtil.confirm("material.confirm.delete")) {
            return;
        }
        try {
            materialService.delete(selected.getId());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        onChanged.accept(jobId);
    }

    private static TableColumn<MaterialItem, String> column(String titleKey, Function<MaterialItem, String> value) {
        TableColumn<MaterialItem, String> column = new TableColumn<>(DialogUtil.message(titleKey));
        column.setCellValueFactory(d -> new SimpleStringProperty(value.apply(d.getValue())));
        return column;
    }
}
