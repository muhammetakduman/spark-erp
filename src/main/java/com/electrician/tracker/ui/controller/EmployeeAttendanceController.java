package com.electrician.tracker.ui.controller;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.AttendanceMatrix;
import com.electrician.tracker.dto.EmployeeAttendanceReport;
import com.electrician.tracker.dto.EmployeeJobAttendance;
import com.electrician.tracker.service.AttendanceService;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.service.ReportService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.TaskRunner;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Personel Puantaj Detayı": one employee's attendance in a date range
 * grouped by job, plus a calendar matrix of all employees.
 */
@Component
@Scope("prototype")
public class EmployeeAttendanceController {

    private static final DateTimeFormatter DAY_HEADER = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter WEEKDAY_HEADER =
            DateTimeFormatter.ofPattern("EEE", Bicimlendirici.TURKISH);
    private static final int SHORT_NAME_LENGTH = 10;
    private static final String ELLIPSIS = "…";

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final ReportService reportService;
    private final TaskRunner taskRunner;

    @FXML
    private ComboBox<Employee> employeeComboBox;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private TabPane tabPane;
    @FXML
    private VBox jobListBox;
    @FXML
    private GridPane matrixGrid;

    public EmployeeAttendanceController(AttendanceService attendanceService, EmployeeService employeeService,
            ReportService reportService, TaskRunner taskRunner) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
        this.reportService = reportService;
        this.taskRunner = taskRunner;
    }

    /** Pre-selects an employee, e.g. when opened from the employee list. */
    public void selectEmployee(Long employeeId) {
        employeeComboBox.getItems().stream()
                .filter(e -> e.getId().equals(employeeId))
                .findFirst()
                .ifPresent(employeeComboBox::setValue);
    }

    @FXML
    private void initialize() {
        YearMonth thisMonth = YearMonth.now();
        fromDatePicker.setValue(thisMonth.atDay(1));
        toDatePicker.setValue(thisMonth.atEndOfMonth());
        setUpEmployeeCombo();
        employeeComboBox.valueProperty().addListener((obs, o, n) -> refresh());
        fromDatePicker.valueProperty().addListener((obs, o, n) -> refresh());
        toDatePicker.valueProperty().addListener((obs, o, n) -> refresh());
        if (!employeeComboBox.getItems().isEmpty()) {
            employeeComboBox.setValue(employeeComboBox.getItems().get(0));
        } else {
            refresh();
        }
    }

    private void setUpEmployeeCombo() {
        List<Employee> employees = employeeService.findAll().stream()
                .sorted(TableSorting.employees())
                .toList();
        employeeComboBox.getItems().setAll(employees);
        employeeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Employee employee) {
                return employee == null ? "" : employee.getName();
            }

            @Override
            public Employee fromString(String string) {
                return null;
            }
        });
    }

    private void refresh() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        if (from == null || to == null) {
            return;
        }
        if (to.isBefore(from)) {
            DialogUtil.showErrorMessage("error.attendance.range.endBeforeStart");
            return;
        }
        Employee employee = employeeComboBox.getValue();
        taskRunner.run(() -> new ScreenData(
                        employee == null ? null : attendanceService.reportForEmployee(employee.getId(), from, to),
                        attendanceService.matrix(from, to)),
                this::show, loadingIndicator, tabPane);
    }

    private void show(ScreenData data) {
        showJobList(data.report());
        showMatrix(data.matrix());
    }

    // ---- By job -------------------------------------------------------------

    private void showJobList(EmployeeAttendanceReport report) {
        jobListBox.getChildren().clear();
        if (report == null || report.jobs().isEmpty()) {
            jobListBox.getChildren().add(new Label(DialogUtil.message("attendance.empty")));
            return;
        }
        for (EmployeeJobAttendance job : report.jobs()) {
            jobListBox.getChildren().add(jobRow(job));
        }
        Label totalBadge = badge(DialogUtil.message("employeeAttendance.total"), "badge-total");
        Label totalText = new Label(withWage(Bicimlendirici.daysWithUnit(report.dayCount()), report.totalWage()));
        totalText.getStyleClass().add("attendance-total");
        HBox totalRow = new HBox(12, totalBadge, totalText);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        jobListBox.getChildren().add(totalRow);
    }

    private Node jobRow(EmployeeJobAttendance job) {
        boolean site = job.jobType() == JobType.SITE;
        Label typeBadge = badge(DialogUtil.message(site ? "employeeAttendance.badge.site" : "employeeAttendance.badge.service"),
                site ? "badge-site" : "badge-service");
        Label title = new Label(job.jobLabel());
        title.getStyleClass().add("form-label");
        Label detail = new Label(withWage(Bicimlendirici.daysWithUnit(job.dayCount()) + " · "
                + Bicimlendirici.workedDays(job.days()), job.totalWage()));
        detail.setWrapText(true);
        HBox row = new HBox(12, typeBadge, new VBox(2, title, detail));
        row.getStyleClass().add("attendance-job-row");
        return row;
    }

    /** "3 gün · 4.500,00 ₺"; the wage part is left out when it was not sent (no rights to see it). */
    private static String withWage(String text, java.math.BigDecimal wage) {
        return wage == null ? text : text + " · " + Bicimlendirici.money(wage);
    }

    private static Label badge(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("badge", styleClass);
        label.setMinWidth(Label.USE_PREF_SIZE);
        return label;
    }

    // ---- Calendar -----------------------------------------------------------

    private void showMatrix(AttendanceMatrix matrix) {
        matrixGrid.getChildren().clear();
        matrixGrid.add(headerCell(DialogUtil.message("attendance.field.employee")), 0, 0);
        for (int col = 0; col < matrix.dates().size(); col++) {
            LocalDate date = matrix.dates().get(col);
            matrixGrid.add(headerCell(date.format(DAY_HEADER) + "\n" + date.format(WEEKDAY_HEADER)), col + 1, 0);
        }
        for (int row = 0; row < matrix.rows().size(); row++) {
            AttendanceMatrix.Row employeeRow = matrix.rows().get(row);
            Label name = new Label(employeeRow.employeeName());
            name.getStyleClass().addAll("matrix-cell", "matrix-name");
            matrixGrid.add(name, 0, row + 1);
            for (int col = 0; col < matrix.dates().size(); col++) {
                List<AttendanceMatrix.Cell> cells = employeeRow.cells()
                        .getOrDefault(matrix.dates().get(col), List.of());
                matrixGrid.add(matrixCell(cells), col + 1, row + 1);
            }
        }
    }

    private static Label headerCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("matrix-cell", "matrix-header");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static Node matrixCell(List<AttendanceMatrix.Cell> cells) {
        VBox box = new VBox(2);
        box.getStyleClass().add("matrix-cell");
        for (AttendanceMatrix.Cell cell : cells) {
            Label jobLabel = new Label(shorten(cell.jobShortName()));
            jobLabel.setTooltip(new Tooltip(cell.jobShortName()));
            if (Bicimlendirici.isHalfDay(cell.dayFactor())) {
                Label half = new Label("½");
                half.getStyleClass().add("half-day-badge");
                HBox line = new HBox(3, jobLabel, half);
                line.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().add(line);
            } else {
                box.getChildren().add(jobLabel);
            }
        }
        if (!cells.isEmpty()) {
            box.getStyleClass().add("matrix-cell-filled");
        }
        return box;
    }

    private static String shorten(String text) {
        return text.length() <= SHORT_NAME_LENGTH ? text : text.substring(0, SHORT_NAME_LENGTH) + ELLIPSIS;
    }

    // ---- Export -------------------------------------------------------------

    @FXML
    private void onExportExcel() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message("employeeAttendance.export.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName(DialogUtil.message("employeeAttendance.export.fileName") + ".xlsx");
        File selected = chooser.showSaveDialog(tabPane.getScene().getWindow());
        if (selected == null) {
            return;
        }
        try {
            reportService.exportAttendanceMatrix(from, to, selected.toPath());
            DialogUtil.showInfo("employeeAttendance.export.success");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    private record ScreenData(EmployeeAttendanceReport report, AttendanceMatrix matrix) {
    }
}
