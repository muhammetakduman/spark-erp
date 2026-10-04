package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.electrician.tracker.dto.MaterialGroupBreakdown;
import com.electrician.tracker.dto.MaterialGroupTotal;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

/**
 * The group table of a job's materials: Grup · Kalem · Maliyet · Satış · Kâr,
 * most spent first, "Kategorisiz" and then the bold "Toplam" row always at
 * the bottom whatever column is sorted. The cost and profit columns are only
 * created when the service filled them; a group with lines lacking a
 * purchase price gets a warning badge next to its cost.
 */
public final class MaterialGroupTable {

    private static final String TOTAL_ROW_STYLE = "total-row";
    private static final double BADGE_GAP = 6;
    private static final String SEPARATOR = " · ";
    private static final double NAME_WIDTH = 170;
    private static final double COUNT_WIDTH = 60;
    private static final double COST_WIDTH = 220;
    private static final double MONEY_WIDTH = 120;

    private MaterialGroupTable() {
    }

    public static TableView<MaterialGroupTotal> build(MaterialGroupBreakdown breakdown, double rowHeight,
            double headerHeight, int maxRows) {
        MaterialGroupTotal total = breakdown.total();
        List<MaterialGroupTotal> rows = new ArrayList<>(breakdown.summary());
        rows.add(total);
        TableView<MaterialGroupTotal> table = new TableView<>(FXCollections.observableArrayList(rows));
        table.getColumns().setAll(columns(total, breakdown.hasFinancials()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setFixedCellSize(rowHeight);
        table.setPrefHeight(headerHeight + Math.min(rows.size(), maxRows) * rowHeight);
        table.getStyleClass().add("group-table");
        table.setRowFactory(view -> new TotalRow(total));
        // "Kategorisiz" and "Toplam" stay last; the clicked column orders the groups above them.
        table.setSortPolicy(view -> {
            Comparator<MaterialGroupTotal> order = Comparator.comparingInt(row -> rank(row, total));
            if (view.getComparator() != null) {
                order = order.thenComparing(view.getComparator());
            }
            FXCollections.sort(view.getItems(), order);
            return true;
        });
        return table;
    }

    /** The group's name, or "Kategorisiz" for lines without a group. */
    public static String groupLabel(MaterialGroupTotal total) {
        return total.isUncategorized() ? DialogUtil.message("material.group.uncategorized") : total.groupName();
    }

    /** "— 2 kalem · Maliyet 46.000,00 ₺ · Satış 60.000,00 ₺ · Kâr 14.000,00 ₺ (KDV hariç)". */
    public static String figuresText(MaterialGroupTotal total) {
        List<String> parts = new ArrayList<>();
        parts.add(DialogUtil.message("material.groupLine.count", total.lineCount()));
        if (total.hasFinancials()) {
            parts.add(DialogUtil.message("material.groupLine.cost", Bicimlendirici.money(total.purchaseTotal())));
        }
        parts.add(DialogUtil.message("material.groupLine.sale", Bicimlendirici.money(total.saleTotal())));
        if (total.hasFinancials()) {
            parts.add(DialogUtil.message("material.groupLine.profit", Bicimlendirici.money(total.profit())));
        }
        return String.join(SEPARATOR, parts) + " " + DialogUtil.message("material.groupLine.vatNote");
    }

    /** "2 kalemin alış fiyatı yok". */
    public static Label missingPurchaseBadge(MaterialGroupTotal total) {
        Label badge = new Label(DialogUtil.message("material.groupTable.missingPurchase",
                total.purchasePriceMissingCount()));
        badge.getStyleClass().add("warning-badge");
        return badge;
    }

    private static int rank(MaterialGroupTotal row, MaterialGroupTotal total) {
        if (row == total) {
            return 2;
        }
        return row.isUncategorized() ? 1 : 0;
    }

    private static List<TableColumn<MaterialGroupTotal, ?>> columns(MaterialGroupTotal total, boolean financials) {
        List<TableColumn<MaterialGroupTotal, ?>> columns = new ArrayList<>();
        TableColumn<MaterialGroupTotal, String> name = new TableColumn<>(DialogUtil.message("material.groupTable.group"));
        TableSorting.text(name, row -> row == total ? DialogUtil.message("material.groupTable.total") : groupLabel(row));
        name.setPrefWidth(NAME_WIDTH);
        TableColumn<MaterialGroupTotal, Integer> count = new TableColumn<>(DialogUtil.message("material.groupTable.lines"));
        TableSorting.integer(count, MaterialGroupTotal::lineCount);
        count.getStyleClass().add("money-cell");
        count.setPrefWidth(COUNT_WIDTH);
        columns.addAll(List.of(name, count));
        if (financials) {
            columns.add(costColumn());
        }
        TableColumn<MaterialGroupTotal, BigDecimal> sale = new TableColumn<>(DialogUtil.message("material.groupTable.sale"));
        TableSorting.money(sale, MaterialGroupTotal::saleTotal);
        sale.setPrefWidth(MONEY_WIDTH);
        columns.add(sale);
        if (financials) {
            TableColumn<MaterialGroupTotal, BigDecimal> profit =
                    new TableColumn<>(DialogUtil.message("material.groupTable.profit"));
            TableSorting.money(profit, MaterialGroupTotal::profit);
            profit.setPrefWidth(MONEY_WIDTH);
            columns.add(profit);
        }
        return columns;
    }

    /** The cost, with the missing-purchase badge in front of it when needed; sorted by the amount. */
    private static TableColumn<MaterialGroupTotal, MaterialGroupTotal> costColumn() {
        TableColumn<MaterialGroupTotal, MaterialGroupTotal> cost =
                new TableColumn<>(DialogUtil.message("material.groupTable.cost"));
        cost.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        cost.setCellFactory(column -> new CostCell());
        cost.setPrefWidth(COST_WIDTH);
        cost.setComparator(Comparator.comparing(MaterialGroupTotal::purchaseTotal,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return cost;
    }

    private static final class CostCell extends TableCell<MaterialGroupTotal, MaterialGroupTotal> {
        private final Label amount = new Label();
        private final HBox box = new HBox(BADGE_GAP);

        private CostCell() {
            box.setAlignment(Pos.CENTER_RIGHT);
        }

        @Override
        protected void updateItem(MaterialGroupTotal row, boolean empty) {
            super.updateItem(row, empty);
            setText(null);
            if (empty || row == null) {
                setGraphic(null);
                return;
            }
            amount.setText(Bicimlendirici.money(row.purchaseTotal()));
            box.getChildren().setAll(amount);
            if (row.hasMissingPurchasePrice()) {
                box.getChildren().add(0, missingPurchaseBadge(row));
            }
            setGraphic(box);
        }
    }

    /** The "Toplam" row is bold on a light band. */
    private static final class TotalRow extends TableRow<MaterialGroupTotal> {
        private final MaterialGroupTotal total;

        private TotalRow(MaterialGroupTotal total) {
            this.total = total;
        }

        @Override
        protected void updateItem(MaterialGroupTotal row, boolean empty) {
            super.updateItem(row, empty);
            getStyleClass().remove(TOTAL_ROW_STYLE);
            if (!empty && row == total) {
                getStyleClass().add(TOTAL_ROW_STYLE);
            }
        }
    }
}
