package com.electrician.tracker.ui.util;

import java.util.Comparator;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.service.MaterialPriceCalculator;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;

/**
 * Material table columns shared by the site and service screens, so both
 * show VAT the same way: a "KDV" column and a grey "(KDV dahil)" note next
 * to the line total of VAT-inclusive lines.
 */
public final class MaterialColumns {

    private static final double NOTE_GAP = 6;

    private MaterialColumns() {
    }

    public static TableColumn<MaterialItem, String> vatColumn() {
        TableColumn<MaterialItem, String> column = new TableColumn<>(DialogUtil.message("material.field.vatRate"));
        bindVat(column);
        return column;
    }

    public static void bindVat(TableColumn<MaterialItem, String> column) {
        TableSorting.text(column, item -> VatLabels.describe(item.getVatRate(), item.getVatIncluded()));
    }

    public static TableColumn<MaterialItem, MaterialItem> totalColumn() {
        TableColumn<MaterialItem, MaterialItem> column = new TableColumn<>(DialogUtil.message("material.field.total"));
        bindTotal(column);
        return column;
    }

    /** Sorted by the line total as a number. */
    public static void bindTotal(TableColumn<MaterialItem, MaterialItem> column) {
        column.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        column.setCellFactory(col -> new TotalCell());
        column.setComparator(Comparator.comparing(MaterialPriceCalculator::saleTotal,
                Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /** A product column showing "Marka — Ürün", sorted in Turkish order. */
    public static void bindProduct(TableColumn<MaterialItem, String> column) {
        TableSorting.text(column, item -> item.getProduct().getDisplayName());
    }

    private static final class TotalCell extends TableCell<MaterialItem, MaterialItem> {
        private final Label amountLabel = new Label();
        private final Label noteLabel = new Label(DialogUtil.message("vat.note.included"));
        private final HBox box = new HBox(NOTE_GAP, amountLabel, noteLabel);

        private TotalCell() {
            noteLabel.getStyleClass().add("vat-note");
        }

        @Override
        protected void updateItem(MaterialItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            amountLabel.setText(Bicimlendirici.money(MaterialPriceCalculator.saleTotal(item)));
            boolean included = item.getVatRate() != null && Boolean.TRUE.equals(item.getVatIncluded());
            noteLabel.setVisible(included);
            noteLabel.setManaged(included);
            setGraphic(box);
        }
    }
}
