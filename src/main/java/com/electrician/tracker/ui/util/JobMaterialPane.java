package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.dto.MaterialGroup;
import com.electrician.tracker.dto.MaterialGroupBreakdown;
import com.electrician.tracker.dto.MaterialGroupTotal;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.MetinKarsilastirici;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Content of a job's "Malzemeler" tab, answering "which group did I spend
 * how much on" first: on top the group table (most spent first, "Toplam"
 * pinned below), under it the lines of the chosen group. "Tüm kalemler"
 * lists every line by group with a "Grup" column. A job with a single group
 * gets a one-line summary instead of the table. The chosen group survives a
 * refresh (e.g. after adding a line). Double click or "Düzenle" edits a line,
 * "Sil" deletes it, "Gruba Taşı" moves the ticked lines (or the selected one).
 * Cost figures only exist when the service filled them; only an ADMIN gets
 * the delete and move actions. Every change goes through
 * {@link MaterialService}; the caller refreshes the job.
 */
public final class JobMaterialPane {

    private static final double ROW_HEIGHT = 28;
    private static final double HEADER_HEIGHT = 30;
    private static final double TABLE_BORDER = 2;
    private static final int MAX_GROUP_ROWS = 9;
    private static final int MAX_LINE_ROWS = 12;
    private static final int DOUBLE_CLICK = 2;
    private static final double SPACING = 10;
    private static final double SECTION_SPACING = 6;
    private static final String UNCATEGORIZED_KEY = "";

    private final MaterialService materialService;
    private final AccessControl accessControl;
    private final Consumer<Long> onChanged;
    private final Consumer<MaterialItem> onEdit;
    /** Per job: the chosen group and whether all lines are listed, kept while the job's pane is rebuilt. */
    private final Map<Long, Choice> choices = new HashMap<>();

    /** {@code onChanged} receives the job id after a change; {@code onEdit} opens the edit dialog. */
    public JobMaterialPane(MaterialService materialService, AccessControl accessControl, Consumer<Long> onChanged,
            Consumer<MaterialItem> onEdit) {
        this.materialService = materialService;
        this.accessControl = accessControl;
        this.onChanged = onChanged;
        this.onEdit = onEdit;
    }

    /** The group a job's tab opens on next time ({@code null} = "Kategorisiz"). */
    public void selectGroup(Long jobId, String groupName) {
        choices.put(jobId, new Choice(keyOf(groupName), false));
    }

    public VBox build(Long jobId, List<MaterialItem> materials) {
        if (materials.isEmpty()) {
            return new VBox(SPACING, new Label(DialogUtil.message("material.empty")));
        }
        MaterialGroupBreakdown breakdown = materialService.groupBreakdown(materials);
        LinesView lines = new LinesView(jobId, breakdown);
        VBox content = new VBox(SPACING);
        if (breakdown.groups().size() == 1) {
            // The one-line summary already names the group: the lines follow without a second title.
            content.getChildren().add(singleGroupLine(breakdown.groups().get(0).total()));
            lines.showGroup(breakdown.groups().get(0));
        } else {
            content.getChildren().addAll(groupSection(jobId, breakdown, lines), new Separator(), lines.header());
            lines.restore(choiceOf(jobId, breakdown));
        }
        content.getChildren().addAll(lines.node(), buttons(lines));
        return content;
    }

    private Choice choiceOf(Long jobId, MaterialGroupBreakdown breakdown) {
        Choice stored = choices.get(jobId);
        boolean known = stored != null && (stored.all() || findGroup(breakdown, stored.groupKey()).isPresent());
        if (known) {
            return stored;
        }
        Choice first = new Choice(keyOf(breakdown.summary().get(0)), false);
        choices.put(jobId, first);
        return first;
    }

    private static Optional<MaterialGroup> findGroup(MaterialGroupBreakdown breakdown, String key) {
        return breakdown.groups().stream().filter(group -> keyOf(group.total()).equals(key)).findFirst();
    }

    private static String keyOf(MaterialGroupTotal total) {
        return total.isUncategorized() ? UNCATEGORIZED_KEY : keyOf(total.groupName());
    }

    private static String keyOf(String groupName) {
        return MetinKarsilastirici.normalize(groupName);
    }

    // ---- Group table ----------------------------------------------------------------

    /** "Grup dağılımı (KDV hariç)" and the group table; clicking a group lists its lines, "Toplam" lists all. */
    private VBox groupSection(Long jobId, MaterialGroupBreakdown breakdown, LinesView lines) {
        TableView<MaterialGroupTotal> table = MaterialGroupTable.build(breakdown, ROW_HEIGHT,
                HEADER_HEIGHT + TABLE_BORDER, MAX_GROUP_ROWS);
        lines.bindGroupTable(table);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row == null || lines.isUpdating()) {
                return;
            }
            if (row == breakdown.total()) {
                lines.showAll();
            } else {
                findGroup(breakdown, keyOf(row)).ifPresent(lines::showGroup);
            }
        });
        Label title = new Label(DialogUtil.message("material.groupTable.title"));
        title.getStyleClass().add("subsection-title");
        return new VBox(SECTION_SPACING, title, table);
    }

    /** "Pano — 2 kalem · Maliyet 46.000,00 ₺ · Satış 60.000,00 ₺ · Kâr 14.000,00 ₺ (KDV hariç)". */
    private static HBox singleGroupLine(MaterialGroupTotal total) {
        Label name = new Label(MaterialGroupTable.groupLabel(total));
        name.getStyleClass().add("material-group-title");
        Label figures = new Label(MaterialGroupTable.figuresText(total));
        HBox line = new HBox(SECTION_SPACING, name, figures);
        if (total.hasMissingPurchasePrice()) {
            line.getChildren().add(MaterialGroupTable.missingPurchaseBadge(total));
        }
        line.setAlignment(Pos.CENTER_LEFT);
        line.getStyleClass().add("group-line-summary");
        return line;
    }

    // ---- Lines of the chosen group --------------------------------------------------

    /** The line table under the group table, its title and the "Tüm kalemler" switch. */
    private final class LinesView {

        private final Long jobId;
        private final MaterialGroupBreakdown breakdown;
        private final TableView<MaterialItem> table = new TableView<>();
        private final TableColumn<MaterialItem, String> groupColumn = groupColumn();
        private final Label title = new Label();
        private final ToggleButton allButton = new ToggleButton(DialogUtil.message("material.lines.all"));
        private final VBox node = new VBox(table);
        private final BulkSelection<MaterialItem> selection;
        private TableView<MaterialGroupTotal> groupTable;
        private boolean updating;

        LinesView(Long jobId, MaterialGroupBreakdown breakdown) {
            this.jobId = jobId;
            this.breakdown = breakdown;
            table.getColumns().setAll(lineColumns(groupColumn));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            table.setFixedCellSize(ROW_HEIGHT);
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
            title.getStyleClass().add("subsection-title");
            allButton.setOnAction(e -> {
                if (allButton.isSelected()) {
                    showAll();
                } else {
                    restore(new Choice(choices.get(jobId).groupKey(), false));
                }
            });
            selection = accessControl.isAdmin()
                    ? BulkSelection.forTable(table, MaterialItem::getId, lines -> deleteLines(lines, jobId)) : null;
        }

        VBox node() {
            return node;
        }

        /** "Anahtar/Priz — 11 kalem" on the left, "Tüm kalemler" on the right. */
        HBox header() {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(SPACING, title, spacer, allButton);
            header.setAlignment(Pos.CENTER_LEFT);
            return header;
        }

        void bindGroupTable(TableView<MaterialGroupTotal> value) {
            this.groupTable = value;
        }

        void restore(Choice choice) {
            if (choice.all()) {
                showAll();
                return;
            }
            findGroup(breakdown, choice.groupKey()).or(() -> breakdown.groups().stream().findFirst())
                    .ifPresent(this::showGroup);
        }

        /** "Anahtar/Priz — 11 kalem" and its lines, product name A–Z; no group column. */
        void showGroup(MaterialGroup group) {
            choices.put(jobId, new Choice(keyOf(group.total()), false));
            title.setText(DialogUtil.message("material.lines.title", MaterialGroupTable.groupLabel(group.total()),
                    group.lines().size()));
            show(group.lines(), false);
            selectInGroupTable(group.total());
        }

        /** "Tüm kalemler — 24 kalem", by group then product, with the group column. */
        void showAll() {
            Choice current = choices.get(jobId);
            choices.put(jobId, new Choice(current == null ? UNCATEGORIZED_KEY : current.groupKey(), true));
            List<MaterialItem> all = breakdown.groups().stream().flatMap(group -> group.lines().stream()).toList();
            title.setText(DialogUtil.message("material.lines.allTitle", all.size()));
            show(all, true);
            selectInGroupTable(breakdown.total());
        }

        private void show(List<MaterialItem> lines, boolean all) {
            allButton.setSelected(all);
            allButton.setText(DialogUtil.message(all ? "material.lines.backToGroup" : "material.lines.all"));
            groupColumn.setVisible(all);
            table.setItems(TableSorting.sorted(lines, TableSorting.materialLines()));
            table.setPrefHeight(HEADER_HEIGHT + TABLE_BORDER
                    + Math.min(Math.max(lines.size(), 1), MAX_LINE_ROWS) * ROW_HEIGHT);
        }

        /** Marks the shown group in the table above without reacting to it again. */
        private void selectInGroupTable(MaterialGroupTotal row) {
            if (groupTable == null || groupTable.getSelectionModel().getSelectedItem() == row) {
                return;
            }
            updating = true;
            try {
                groupTable.getSelectionModel().select(row);
            } finally {
                updating = false;
            }
        }

        MaterialItem selectedLine() {
            return table.getSelectionModel().getSelectedItem();
        }

        /** The ticked lines, or the selected line when nothing is ticked. */
        List<MaterialItem> linesToMove() {
            List<MaterialItem> ticked = selection == null ? List.of() : selection.selected();
            if (!ticked.isEmpty()) {
                return ticked;
            }
            MaterialItem selected = selectedLine();
            return selected == null ? List.of() : List.of(selected);
        }

        boolean isUpdating() {
            return updating;
        }
    }

    private static TableColumn<MaterialItem, String> groupColumn() {
        TableColumn<MaterialItem, String> group = new TableColumn<>(DialogUtil.message("material.field.group"));
        ColumnWidth.NAME.applyTo(group);
        TableSorting.text(group, line -> line.getGroupName() == null
                ? DialogUtil.message("material.group.uncategorized") : line.getGroupName());
        group.setVisible(false);
        return group;
    }

    /** Tarih · [Grup] · Ürün · Miktar · [Alış · Alış KDV · Tedarikçi] · Satış · KDV · Toplam. */
    private List<TableColumn<MaterialItem, ?>> lineColumns(TableColumn<MaterialItem, String> groupColumn) {
        List<TableColumn<MaterialItem, ?>> columns = new ArrayList<>();
        TableColumn<MaterialItem, LocalDate> date = new TableColumn<>(DialogUtil.message("material.field.date"));
        TableSorting.date(date, MaterialItem::getItemDate);
        TableColumn<MaterialItem, String> product = new TableColumn<>(DialogUtil.message("material.field.product"));
        TableSorting.text(product, m -> m.getProduct().getDisplayName());
        TableColumn<MaterialItem, BigDecimal> quantity = new TableColumn<>(DialogUtil.message("material.field.quantity"));
        TableSorting.number(quantity, MaterialItem::getQuantity, value -> Bicimlendirici.quantity(value));
        columns.addAll(List.of(date, groupColumn, product, quantity));
        if (accessControl.canViewFinancials()) {
            columns.addAll(purchaseColumns());
        }
        TableColumn<MaterialItem, BigDecimal> sale = new TableColumn<>(DialogUtil.message("material.field.salePrice"));
        TableSorting.money(sale, MaterialItem::getSaleUnitPrice);
        TableColumn<MaterialItem, String> vat = MaterialColumns.vatColumn();
        TableColumn<MaterialItem, MaterialItem> total = MaterialColumns.totalColumn();
        columns.addAll(List.of(sale, vat, total));
        // Shares of the width: names get room, short figures do not.
        ColumnWidth.DATE.applyTo(date);
        ColumnWidth.PRODUCT.applyTo(product);
        ColumnWidth.SHORT.applyTo(quantity);
        ColumnWidth.MONEY.applyTo(sale);
        ColumnWidth.VAT.applyTo(vat);
        ColumnWidth.MONEY.applyTo(total);
        return columns;
    }

    /**
     * Relative column widths of the line table (the table fits them to its
     * width) and how far each may shrink: dates and amounts stay readable,
     * names and VAT texts give way first.
     */
    private enum ColumnWidth {
        DATE(80, 78), NAME(110, 56), PRODUCT(190, 96), SHORT(55, 42), MONEY(95, 82), VAT(80, 52);

        private final double width;
        private final double minWidth;

        ColumnWidth(double width, double minWidth) {
            this.width = width;
            this.minWidth = minWidth;
        }

        void applyTo(TableColumn<MaterialItem, ?> column) {
            column.setPrefWidth(width);
            column.setMinWidth(minWidth);
        }
    }

    private static List<TableColumn<MaterialItem, ?>> purchaseColumns() {
        TableColumn<MaterialItem, BigDecimal> purchase =
                new TableColumn<>(DialogUtil.message("material.field.purchasePrice"));
        TableSorting.money(purchase, MaterialItem::getPurchaseUnitPriceTl);
        ColumnWidth.MONEY.applyTo(purchase);
        purchase.setCellFactory(column -> new PurchasePriceCell<>(item -> item.isForeignCurrencyPurchase()
                ? new PurchasePriceCell.ForeignAmount(item.getPurchaseUnitPrice(), item.getPurchaseCurrency(),
                        item.getPurchaseExchangeRate())
                : null));
        TableColumn<MaterialItem, String> purchaseVat =
                new TableColumn<>(DialogUtil.message("material.field.purchaseVat"));
        TableSorting.text(purchaseVat, m -> m.getPurchaseUnitPrice() == null ? ""
                : VatLabels.describe(m.getPurchaseVatRate(), m.getPurchaseVatIncluded()));
        ColumnWidth.VAT.applyTo(purchaseVat);
        TableColumn<MaterialItem, String> supplier = new TableColumn<>(DialogUtil.message("material.field.supplier"));
        TableSorting.text(supplier, MaterialItem::getSupplierName);
        ColumnWidth.NAME.applyTo(supplier);
        return List.of(purchase, purchaseVat, supplier);
    }

    // ---- Actions --------------------------------------------------------------------

    /** Düzenle · Sil · Gruba Taşı under the line table. */
    private HBox buttons(LinesView lines) {
        Button editButton = new Button(DialogUtil.message("jobDetail.action.editMaterial"));
        editButton.setOnAction(e -> withSelection(lines.selectedLine(), onEdit));
        HBox buttons = new HBox(SPACING, editButton);
        if (accessControl.isAdmin()) {
            Button deleteButton = new Button(DialogUtil.message("jobDetail.action.deleteMaterial"));
            deleteButton.getStyleClass().add("danger-button");
            deleteButton.setOnAction(e -> withSelection(lines.selectedLine(), line -> deleteLine(line, lines.jobId)));
            Button moveButton = Icons.button(AppIcon.MOVE_TO_GROUP, "material.action.moveToGroup",
                    () -> moveToGroup(lines.linesToMove(), lines.jobId));
            buttons.getChildren().addAll(deleteButton, moveButton);
        }
        buttons.getStyleClass().add("table-actions");
        return buttons;
    }

    /** Asks for one group name (with this job's groups first) and moves the lines there. */
    private void moveToGroup(List<MaterialItem> lines, Long jobId) {
        if (lines.isEmpty()) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        ComboBox<String> groupBox = new ComboBox<>();
        groupBox.setEditable(true);
        groupBox.setMaxWidth(Double.MAX_VALUE);
        SuggestionPicker picker = SuggestionPicker.ordered(groupBox, materialService.findGroupSuggestions(jobId));
        boolean confirmed = SparkDialog.builder(SparkDialog.Kind.QUESTION)
                .title(DialogUtil.message("material.moveToGroup.title", lines.size()))
                .detail(DialogUtil.message("material.moveToGroup.detail"))
                .content(groupBox)
                .primary(DialogUtil.message("material.action.moveToGroup"))
                .secondary(DialogUtil.message("action.cancel"))
                .build().showAndWait();
        if (!confirmed) {
            return;
        }
        try {
            materialService.moveToGroup(lines.stream().map(MaterialItem::getId).toList(), picker.typedText());
            // The tab follows the lines to their new group.
            String moved = picker.typedText().isBlank() ? null : picker.typedText();
            if (moved != null) {
                selectGroup(jobId, moved);
            }
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        onChanged.accept(jobId);
    }

    /** "Seçilenleri Sil" for material lines. */
    private void deleteLines(List<MaterialItem> lines, Long jobId) {
        BulkDeleteFlow.of(lines, MaterialItem::getId, JobMaterialPane::lineLabel)
                .itemCount("bulk.count.materials")
                .delete(materialService::deleteAll)
                .afterwards(() -> onChanged.accept(jobId))
                .run();
    }

    private static String lineLabel(MaterialItem line) {
        return DialogUtil.message("delete.single.material", line.getProduct().getDisplayName(),
                Bicimlendirici.quantity(line.getQuantity()), EnumLabels.label(line.getProduct().getUnit()));
    }

    private static void withSelection(MaterialItem line, Consumer<MaterialItem> action) {
        if (line == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        action.accept(line);
    }

    private void deleteLine(MaterialItem line, Long jobId) {
        if (!DeleteConfirmation.confirmSingle(lineLabel(line))) {
            return;
        }
        try {
            materialService.delete(line.getId());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        onChanged.accept(jobId);
    }

    /** The group a job's tab shows, or all of its lines. */
    private record Choice(String groupKey, boolean all) {
    }
}
