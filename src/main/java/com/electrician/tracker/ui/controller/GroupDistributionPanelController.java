package com.electrician.tracker.ui.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.GroupJobBreakdown;
import com.electrician.tracker.dto.GroupJobRow;
import com.electrician.tracker.dto.MaterialGroupTotal;
import com.electrician.tracker.service.ProductCatalogService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.JobNavigator;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.TaskRunner;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Şantiye bazında dağılım" on the product screen: for the chosen material
 * group, a row per job (most spent first, services with a badge) and a bold
 * "Toplam" row, between two optional dates. Double click opens the job — a
 * site on its materials with this group chosen. Cost and profit columns
 * exist only when the service filled them.
 */
@Component
@Scope("prototype")
public class GroupDistributionPanelController {

    private static final double BADGE_SPACING = 6;
    private static final int DOUBLE_CLICK = 2;
    private static final String TOTAL_ROW_STYLE = "total-row";
    private static final double JOB_WIDTH = 260;
    private static final double COUNT_WIDTH = 55;
    private static final double MONEY_WIDTH = 105;
    private static final double MONEY_MIN_WIDTH = 85;

    private final ProductCatalogService productCatalogService;
    private final TaskRunner taskRunner;
    private final JobNavigator jobNavigator;

    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private Label headerLabel;
    @FXML
    private TableView<GroupJobRow> jobTable;

    private boolean groupChosen;
    private String groupName;
    private Boolean columnsWithFinancials;
    private Consumer<GroupJobBreakdown> onLoaded = breakdown -> {
    };

    public GroupDistributionPanelController(ProductCatalogService productCatalogService, TaskRunner taskRunner,
            JobNavigator jobNavigator) {
        this.productCatalogService = productCatalogService;
        this.taskRunner = taskRunner;
        this.jobNavigator = jobNavigator;
    }

    @FXML
    private void initialize() {
        jobTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        jobTable.setPlaceholder(new Label(DialogUtil.message("groupDistribution.empty")));
        jobTable.setRowFactory(table -> new JobRow());
        TableSorting.pinLast(jobTable, GroupDistributionPanelController::isTotal);
        fromDatePicker.valueProperty().addListener((obs, o, n) -> reload());
        toDatePicker.valueProperty().addListener((obs, o, n) -> reload());
    }

    /** Receives each loaded breakdown (the product list shows only the group's products). */
    public void setOnLoaded(Consumer<GroupJobBreakdown> value) {
        this.onLoaded = value;
    }

    /** Shows {@code name}'s spending per job ({@code null} = "Kategorisiz"). */
    public void showGroup(String name) {
        this.groupChosen = true;
        this.groupName = name;
        reload();
    }

    @FXML
    private void onClearDates() {
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
    }

    private void reload() {
        if (!groupChosen) {
            return;
        }
        String name = groupName;
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        taskRunner.run(() -> productCatalogService.groupDistribution(name, from, to), this::show,
                loadingIndicator, jobTable);
    }

    private void show(GroupJobBreakdown breakdown) {
        if (columnsWithFinancials == null || columnsWithFinancials != breakdown.hasFinancials()) {
            columnsWithFinancials = breakdown.hasFinancials();
            jobTable.getColumns().setAll(columns(columnsWithFinancials));
        }
        List<GroupJobRow> rows = new ArrayList<>(breakdown.rows());
        if (!rows.isEmpty()) {
            rows.add(new GroupJobRow(null, null, DialogUtil.message("groupDistribution.total"), null,
                    breakdown.total()));
        }
        jobTable.setItems(FXCollections.observableArrayList(rows));
        jobTable.sort();
        headerLabel.setText(headerText(breakdown));
        onLoaded.accept(breakdown);
    }

    /** "Sigorta grubu — toplam gider: 77.200,00 ₺ · 5 iş · 14 alış". */
    private static String headerText(GroupJobBreakdown breakdown) {
        String name = breakdown.groupName() == null ? DialogUtil.message("material.group.uncategorized")
                : breakdown.groupName();
        MaterialGroupTotal total = breakdown.total();
        return total.hasFinancials()
                ? DialogUtil.message("groupDistribution.header", name, Bicimlendirici.money(total.purchaseTotal()),
                        breakdown.jobCount(), total.lineCount())
                : DialogUtil.message("groupDistribution.headerCountOnly", name, breakdown.jobCount(),
                        total.lineCount());
    }

    private static boolean isTotal(GroupJobRow row) {
        return row.jobId() == null;
    }

    private static List<TableColumn<GroupJobRow, ?>> columns(boolean financials) {
        List<TableColumn<GroupJobRow, ?>> columns = new ArrayList<>();
        TableColumn<GroupJobRow, GroupJobRow> job = new TableColumn<>(DialogUtil.message("groupDistribution.job"));
        job.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        job.setCellFactory(column -> new JobCell());
        job.setComparator(Comparator.comparing(GroupJobRow::label, TableSorting.turkishText()));
        job.setPrefWidth(JOB_WIDTH);
        TableColumn<GroupJobRow, Integer> lines = new TableColumn<>(DialogUtil.message("groupDistribution.lines"));
        TableSorting.integer(lines, row -> row.total().lineCount());
        lines.getStyleClass().add("money-cell");
        lines.setPrefWidth(COUNT_WIDTH);
        columns.addAll(List.of(job, lines));
        if (financials) {
            columns.add(moneyColumn("groupDistribution.cost", MaterialGroupTotal::purchaseTotal));
        }
        columns.add(moneyColumn("groupDistribution.sale", MaterialGroupTotal::saleTotal));
        if (financials) {
            columns.add(moneyColumn("groupDistribution.profit", MaterialGroupTotal::profit));
        }
        return columns;
    }

    private static TableColumn<GroupJobRow, BigDecimal> moneyColumn(String titleKey,
            java.util.function.Function<MaterialGroupTotal, BigDecimal> amount) {
        TableColumn<GroupJobRow, BigDecimal> column = new TableColumn<>(DialogUtil.message(titleKey));
        TableSorting.money(column, row -> amount.apply(row.total()));
        column.setPrefWidth(MONEY_WIDTH);
        column.setMinWidth(MONEY_MIN_WIDTH);
        return column;
    }

    /** Bold "Toplam" band; double click opens the job. */
    private final class JobRow extends TableRow<GroupJobRow> {

        private JobRow() {
            setOnMouseClicked(event -> {
                GroupJobRow row = getItem();
                if (event.getClickCount() == DOUBLE_CLICK && row != null && !isTotal(row)) {
                    open(row);
                }
            });
        }

        @Override
        protected void updateItem(GroupJobRow row, boolean empty) {
            super.updateItem(row, empty);
            getStyleClass().remove(TOTAL_ROW_STYLE);
            if (!empty && row != null && isTotal(row)) {
                getStyleClass().add(TOTAL_ROW_STYLE);
            }
        }
    }

    private void open(GroupJobRow row) {
        if (row.jobType() == JobType.SITE) {
            jobNavigator.openSiteGroup(row.jobId(), groupName);
        } else {
            jobNavigator.open(row.jobId(), row.jobType());
        }
    }

    /** "Customer – Job", with a "SERVİS" badge in front of services. */
    private static final class JobCell extends TableCell<GroupJobRow, GroupJobRow> {
        @Override
        protected void updateItem(GroupJobRow row, boolean empty) {
            super.updateItem(row, empty);
            setText(null);
            if (empty || row == null) {
                setGraphic(null);
                return;
            }
            Label name = new Label(row.label());
            if (row.jobType() != JobType.SERVICE) {
                setGraphic(name);
                return;
            }
            Label badge = new Label(DialogUtil.message("employeeAttendance.badge.service"));
            badge.getStyleClass().addAll("badge", "badge-service");
            setGraphic(new HBox(BADGE_SPACING, badge, name));
        }
    }
}
