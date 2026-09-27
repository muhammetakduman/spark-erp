package com.electrician.tracker.ui.controller;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.EmployeeWageSummary;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.MaterialSummaryLine;
import com.electrician.tracker.dto.MonthlyReport;
import com.electrician.tracker.dto.MonthlyReportRow;
import com.electrician.tracker.dto.ReportJobFilter;
import com.electrician.tracker.dto.ReportTotals;
import com.electrician.tracker.dto.YearlyReport;
import com.electrician.tracker.service.AylikRaporService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.ContentNavigator;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.StatBoxes;
import com.electrician.tracker.ui.util.TaskRunner;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

/**
 * "Aylık Rapor": a month's revenue, cost, profit and uncollected money with
 * its jobs, products and attendance, plus a yearly overview with a monthly
 * profit chart. The board is loaded once in the background; switching month,
 * year or filter recalculates from it without querying again.
 */
@Component
public class MonthlyReportController {

    private static final String HOME_FXML = "/fxml/home_screen.fxml";
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;
    private static final int DOUBLE_CLICK = 2;
    private static final double BADGE_SPACING = 6;
    private static final String TOTAL_ROW_STYLE = "total-row";

    private final AylikRaporService aylikRaporService;
    private final TaskRunner taskRunner;
    private final ContentNavigator contentNavigator;

    @FXML
    private ToggleGroup filterGroup;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private VBox monthlyContent;
    @FXML
    private ComboBox<Month> monthCombo;
    @FXML
    private Spinner<Integer> monthYearSpinner;
    @FXML
    private HBox monthlyCards;
    @FXML
    private TableView<MonthlyReportRow> jobTable;
    @FXML
    private TableColumn<MonthlyReportRow, String> dateColumn;
    @FXML
    private TableColumn<MonthlyReportRow, String> customerColumn;
    @FXML
    private TableColumn<MonthlyReportRow, MonthlyReportRow> jobColumn;
    @FXML
    private TableColumn<MonthlyReportRow, String> materialColumn;
    @FXML
    private TableColumn<MonthlyReportRow, String> wageColumn;
    @FXML
    private TableColumn<MonthlyReportRow, String> revenueColumn;
    @FXML
    private TableColumn<MonthlyReportRow, String> costColumn;
    @FXML
    private TableColumn<MonthlyReportRow, String> profitColumn;
    @FXML
    private TableColumn<MonthlyReportRow, String> paymentColumn;
    @FXML
    private TableView<MaterialSummaryLine> materialTable;
    @FXML
    private TableColumn<MaterialSummaryLine, String> productColumn;
    @FXML
    private TableColumn<MaterialSummaryLine, String> quantityColumn;
    @FXML
    private TableColumn<MaterialSummaryLine, String> purchaseColumn;
    @FXML
    private TableColumn<MaterialSummaryLine, String> saleColumn;
    @FXML
    private TableView<EmployeeWageSummary> wageTable;
    @FXML
    private TableColumn<EmployeeWageSummary, String> employeeColumn;
    @FXML
    private TableColumn<EmployeeWageSummary, String> dayCountColumn;
    @FXML
    private TableColumn<EmployeeWageSummary, String> wageTotalColumn;
    @FXML
    private Spinner<Integer> yearSpinner;
    @FXML
    private HBox yearlyCards;
    @FXML
    private TableView<YearlyReport.MonthTotals> yearTable;
    @FXML
    private TableColumn<YearlyReport.MonthTotals, String> monthColumn;
    @FXML
    private TableColumn<YearlyReport.MonthTotals, String> yearRevenueColumn;
    @FXML
    private TableColumn<YearlyReport.MonthTotals, String> yearCostColumn;
    @FXML
    private TableColumn<YearlyReport.MonthTotals, String> yearProfitColumn;
    @FXML
    private TableColumn<YearlyReport.MonthTotals, String> yearUncollectedColumn;
    @FXML
    private BarChart<String, Number> profitChart;

    private JobBoard board;
    private MonthlyReport monthlyReport;
    private ReportJobFilter filter = ReportJobFilter.ALL;
    private boolean updatingSelectors;

    public MonthlyReportController(AylikRaporService aylikRaporService, TaskRunner taskRunner,
            ContentNavigator contentNavigator) {
        this.aylikRaporService = aylikRaporService;
        this.taskRunner = taskRunner;
        this.contentNavigator = contentNavigator;
    }

    @FXML
    private void initialize() {
        setUpSelectors();
        setUpJobTable();
        setUpSummaryTables();
        setUpYearTable();
        taskRunner.run(aylikRaporService::loadBoard, this::onBoardLoaded, loadingIndicator, monthlyContent);
    }

    private void onBoardLoaded(JobBoard loaded) {
        this.board = loaded;
        refreshMonthly();
        refreshYearly();
    }

    // ---- Selectors ----------------------------------------------------------

    private void setUpSelectors() {
        YearMonth now = YearMonth.now();
        monthCombo.getItems().setAll(Month.values());
        monthCombo.setConverter(new MonthConverter());
        monthCombo.setValue(now.getMonth());
        monthYearSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_YEAR, MAX_YEAR,
                now.getYear()));
        yearSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_YEAR, MAX_YEAR,
                now.getYear()));

        monthCombo.valueProperty().addListener((obs, o, n) -> onMonthSelectorChanged());
        monthYearSpinner.valueProperty().addListener((obs, o, n) -> onMonthSelectorChanged());
        yearSpinner.valueProperty().addListener((obs, o, n) -> refreshYearly());
        filterGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                // A filter is always active: clicking the selected one again keeps it selected.
                filterGroup.selectToggle(oldToggle);
                return;
            }
            filter = ReportJobFilter.valueOf((String) newToggle.getUserData());
            refreshMonthly();
            refreshYearly();
        });
    }

    private YearMonth selectedMonth() {
        return YearMonth.of(monthYearSpinner.getValue(), monthCombo.getValue());
    }

    private void showMonth(YearMonth month) {
        updatingSelectors = true;
        try {
            monthYearSpinner.getValueFactory().setValue(month.getYear());
            monthCombo.setValue(month.getMonth());
        } finally {
            updatingSelectors = false;
        }
        refreshMonthly();
    }

    private void onMonthSelectorChanged() {
        if (!updatingSelectors) {
            refreshMonthly();
        }
    }

    @FXML
    private void onPreviousMonth() {
        showMonth(selectedMonth().minusMonths(1));
    }

    @FXML
    private void onNextMonth() {
        showMonth(selectedMonth().plusMonths(1));
    }

    @FXML
    private void onThisMonth() {
        showMonth(YearMonth.now());
    }

    @FXML
    private void onPreviousYear() {
        yearSpinner.decrement();
    }

    @FXML
    private void onNextYear() {
        yearSpinner.increment();
    }

    // ---- Monthly --------------------------------------------------------------

    private void refreshMonthly() {
        if (board == null) {
            return;
        }
        monthlyReport = aylikRaporService.monthly(board, selectedMonth(), filter);
        showCards(monthlyCards, monthlyReport.totals());
        List<MonthlyReportRow> rows = new ArrayList<>(monthlyReport.rows());
        if (!rows.isEmpty()) {
            rows.add(new MonthlyReportRow(null, null, DialogUtil.message("report.total"), null, null,
                    monthlyReport.totals()));
        }
        jobTable.setItems(FXCollections.observableArrayList(rows));
        materialTable.setItems(FXCollections.observableArrayList(monthlyReport.materials()));
        wageTable.setItems(FXCollections.observableArrayList(monthlyReport.wages()));
    }

    private void showCards(HBox cards, ReportTotals totals) {
        String profitCaption = totals.isProfitEstimated()
                ? DialogUtil.message("report.card.profitEstimated", totals.missingPurchasePriceCount()) : null;
        cards.getChildren().setAll(
                card("report.card.revenue", totals.revenue(),
                        DialogUtil.message("report.card.revenueCaption", Bicimlendirici.money(totals.vatAmount()))),
                card("report.card.cost", totals.cost(),
                        DialogUtil.message("report.card.costCaption", Bicimlendirici.money(totals.wageTotal()))),
                card("report.card.profit", totals.profit(), profitCaption),
                card("report.card.uncollected", totals.uncollected(), null));
    }

    private VBox card(String titleKey, BigDecimal value, String caption) {
        VBox card = StatBoxes.box(DialogUtil.message(titleKey), Bicimlendirici.money(value), caption);
        card.getStyleClass().add("summary-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void setUpJobTable() {
        bind(dateColumn, row -> Bicimlendirici.date(row.date()));
        bind(customerColumn, MonthlyReportRow::customerName);
        jobColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        jobColumn.setCellFactory(column -> new JobCell());
        bind(materialColumn, row -> Bicimlendirici.money(row.figures().materialCost()));
        bind(wageColumn, row -> Bicimlendirici.money(row.figures().wageTotal()));
        bind(revenueColumn, row -> Bicimlendirici.money(row.figures().revenue()));
        bind(costColumn, row -> Bicimlendirici.money(row.figures().cost()));
        bind(profitColumn, row -> Bicimlendirici.money(row.figures().profit())
                + (row.figures().isProfitEstimated() ? " *" : ""));
        bind(paymentColumn, row -> row.isFullyPaid() ? DialogUtil.message("report.payment.paid")
                : DialogUtil.message("report.payment.remaining", Bicimlendirici.money(row.figures().uncollected())));
        jobTable.setRowFactory(table -> {
            TableRow<MonthlyReportRow> row = new TableRow<>() {
                @Override
                protected void updateItem(MonthlyReportRow item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove(TOTAL_ROW_STYLE);
                    if (!empty && item != null && item.jobId() == null) {
                        getStyleClass().add(TOTAL_ROW_STYLE);
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == DOUBLE_CLICK && !row.isEmpty() && row.getItem().jobId() != null) {
                    Long jobId = row.getItem().jobId();
                    contentNavigator.<HomeController>show(HOME_FXML, home -> home.focusJob(jobId));
                }
            });
            return row;
        });
    }

    private void setUpSummaryTables() {
        bind(productColumn, MaterialSummaryLine::productName);
        bind(quantityColumn, line -> Bicimlendirici.quantity(line.quantity()) + " " + EnumLabels.label(line.unit()));
        bind(purchaseColumn, line -> Bicimlendirici.money(line.purchaseTotal()));
        bind(saleColumn, line -> Bicimlendirici.money(line.saleTotal()));
        bind(employeeColumn, EmployeeWageSummary::employeeName);
        bind(dayCountColumn, wage -> Bicimlendirici.daysWithUnit(wage.dayCount()));
        bind(wageTotalColumn, wage -> Bicimlendirici.money(wage.totalWage()));
    }

    // ---- Yearly ---------------------------------------------------------------

    private void setUpYearTable() {
        bind(monthColumn, line -> line.month() == null ? DialogUtil.message("report.total")
                : monthName(line.month().getMonth()));
        bind(yearRevenueColumn, line -> Bicimlendirici.money(line.totals().revenue()));
        bind(yearCostColumn, line -> Bicimlendirici.money(line.totals().cost()));
        bind(yearProfitColumn, line -> Bicimlendirici.money(line.totals().profit()));
        bind(yearUncollectedColumn, line -> Bicimlendirici.money(line.totals().uncollected()));
        yearTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(YearlyReport.MonthTotals item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove(TOTAL_ROW_STYLE);
                if (!empty && item != null && item.month() == null) {
                    getStyleClass().add(TOTAL_ROW_STYLE);
                }
            }
        });
    }

    private void refreshYearly() {
        if (board == null || yearSpinner.getValue() == null) {
            return;
        }
        YearlyReport report = aylikRaporService.yearly(board, yearSpinner.getValue(), filter);
        showCards(yearlyCards, report.total());
        List<YearlyReport.MonthTotals> rows = new ArrayList<>(report.months());
        rows.add(new YearlyReport.MonthTotals(null, report.total()));
        yearTable.setItems(FXCollections.observableArrayList(rows));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (YearlyReport.MonthTotals month : report.months()) {
            series.getData().add(new XYChart.Data<>(shortMonthName(month.month().getMonth()),
                    month.totals().profit().doubleValue()));
        }
        profitChart.getData().setAll(List.of(series));
    }

    // ---- Export ---------------------------------------------------------------

    @FXML
    private void onExportExcel() {
        export("report.export.excelTitle", "Excel (*.xlsx)", "*.xlsx", ".xlsx", aylikRaporService::exportExcel);
    }

    @FXML
    private void onExportPdf() {
        export("report.export.pdfTitle", "PDF (*.pdf)", "*.pdf", ".pdf", aylikRaporService::exportPdf);
    }

    private void export(String titleKey, String filterName, String pattern, String extension,
            BiConsumer<MonthlyReport, Path> writer) {
        if (monthlyReport == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message(titleKey));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(filterName, pattern));
        chooser.setInitialFileName(DialogUtil.message("report.export.fileName",
                monthName(monthlyReport.month().getMonth()), String.valueOf(monthlyReport.month().getYear()))
                + extension);
        File selected = chooser.showSaveDialog(jobTable.getScene().getWindow());
        if (selected == null) {
            return;
        }
        MonthlyReport report = monthlyReport;
        taskRunner.run(() -> {
            writer.accept(report, selected.toPath());
            return selected;
        }, file -> DialogUtil.showInfo("report.export.success"), loadingIndicator, monthlyContent);
    }

    // ---- Helpers --------------------------------------------------------------

    private static <T> void bind(TableColumn<T, String> column, Function<T, String> value) {
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
    }

    private static String monthName(Month month) {
        return month.getDisplayName(TextStyle.FULL_STANDALONE, Bicimlendirici.TURKISH);
    }

    private static String shortMonthName(Month month) {
        return month.getDisplayName(TextStyle.SHORT_STANDALONE, Bicimlendirici.TURKISH);
    }

    private static final class MonthConverter extends StringConverter<Month> {
        @Override
        public String toString(Month month) {
            return month == null ? "" : monthName(month);
        }

        @Override
        public Month fromString(String text) {
            return null;
        }
    }

    /** "ŞANTİYE" / "SERVİS" badge followed by the job name; the total row shows neither. */
    private static final class JobCell extends TableCell<MonthlyReportRow, MonthlyReportRow> {
        @Override
        protected void updateItem(MonthlyReportRow row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null || row.jobType() == null) {
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
