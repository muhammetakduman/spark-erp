package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.dto.EmployeeWageSummary;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.AttendanceMath;
import com.electrician.tracker.service.AttendanceService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Content of a job's "Puantaj" tab: a per-employee summary (most days first)
 * and the raw attendance rows (oldest first; editable). Wage columns exist
 * only for users who may see wages; only an ADMIN gets the delete button.
 * Attendance is only recorded; it never changes the job's profit. Every
 * change goes through {@link AttendanceService}; the caller refreshes.
 */
public final class JobAttendancePane {

    private static final double ROW_HEIGHT = 28;
    private static final double HEADER_HEIGHT = 40;
    private static final double MAX_TABLE_HEIGHT = 240;
    private static final double SPACING = 10;
    private static final double NOTE_WIDTH = 200;

    private final AttendanceService attendanceService;
    private final AccessControl accessControl;
    private final Consumer<Long> onChanged;

    /** {@code onChanged} receives the id of the job whose attendance changed. */
    public JobAttendancePane(AttendanceService attendanceService, AccessControl accessControl,
            Consumer<Long> onChanged) {
        this.attendanceService = attendanceService;
        this.accessControl = accessControl;
        this.onChanged = onChanged;
    }

    /** "Puantaj (18 gün)". */
    public static String tabTitle(JobSummary summary) {
        return DialogUtil.message("jobDetail.tab.team", Bicimlendirici.days(summary.attendanceDayCount()));
    }

    public VBox build(Job job, JobSummary summary, List<Attendance> attendances) {
        Runnable changed = () -> onChanged.accept(job.getId());
        TableView<Attendance> rawTable = buildRawTable(attendances, changed);
        rawTable.setTooltip(new Tooltip(DialogUtil.message(accessControl.canViewFinancials()
                ? "attendance.rows.tooltip" : "attendance.rows.tooltipNoWage")));
        Label rawTitle = new Label(DialogUtil.message("attendance.section.rows"));
        rawTitle.getStyleClass().add("subsection-title");
        VBox content = new VBox(SPACING, buildSummaryTable(summary.employeeWageSummaries()), rawTitle, rawTable);
        if (accessControl.isAdmin()) {
            Button deleteButton = new Button(DialogUtil.message("attendance.action.deleteSelected"));
            deleteButton.getStyleClass().add("danger-button");
            deleteButton.setOnAction(e -> deleteSelected(rawTable, changed));
            HBox buttons = new HBox(SPACING, deleteButton);
            buttons.getStyleClass().add("table-actions");
            content.getChildren().add(buttons);
        }
        return content;
    }

    private TableView<EmployeeWageSummary> buildSummaryTable(List<EmployeeWageSummary> summaries) {
        TableView<EmployeeWageSummary> table = new TableView<>(
                TableSorting.sorted(summaries, TableSorting.attendanceSummary()));
        List<TableColumn<EmployeeWageSummary, ?>> columns = new ArrayList<>();
        TableColumn<EmployeeWageSummary, String> employee =
                new TableColumn<>(DialogUtil.message("attendance.field.employee"));
        TableSorting.text(employee, s -> s.employeeName()
                + (s.master() ? " (" + DialogUtil.message("employee.field.master") + ")" : ""));
        TableColumn<EmployeeWageSummary, BigDecimal> days =
                new TableColumn<>(DialogUtil.message("attendance.field.dayCount"));
        TableSorting.number(days, EmployeeWageSummary::dayCount, Bicimlendirici::days);
        columns.addAll(List.of(employee, days));
        if (accessControl.canViewFinancials()) {
            TableColumn<EmployeeWageSummary, BigDecimal> average =
                    new TableColumn<>(DialogUtil.message("attendance.field.averageWage"));
            TableSorting.money(average, EmployeeWageSummary::averageWage);
            TableColumn<EmployeeWageSummary, BigDecimal> total =
                    new TableColumn<>(DialogUtil.message("attendance.field.total"));
            TableSorting.money(total, EmployeeWageSummary::totalWage);
            columns.addAll(List.of(average, total));
        }
        TableColumn<EmployeeWageSummary, String> worked =
                new TableColumn<>(DialogUtil.message("attendance.field.workedDays"));
        TableSorting.text(worked, s -> Bicimlendirici.workedDays(s.days()));
        columns.add(worked);
        table.getColumns().setAll(columns);
        table.setPlaceholder(new Label(DialogUtil.message("attendance.empty")));
        fitHeight(table, summaries.size());
        return table;
    }

    private TableView<Attendance> buildRawTable(List<Attendance> attendances, Runnable changed) {
        TableView<Attendance> table = new TableView<>(TableSorting.sorted(attendances, TableSorting.attendanceRows()));
        table.setEditable(true);
        TableColumn<Attendance, LocalDate> dateColumn = new TableColumn<>(DialogUtil.message("attendance.field.date"));
        TableSorting.date(dateColumn, Attendance::getAttendanceDate);
        TableColumn<Attendance, String> employeeColumn =
                new TableColumn<>(DialogUtil.message("attendance.field.employee"));
        TableSorting.text(employeeColumn, a -> a.getEmployee().getName());
        List<TableColumn<Attendance, ?>> columns = new ArrayList<>(List.of(dateColumn, employeeColumn));
        if (accessControl.canViewFinancials()) {
            columns.add(wageColumn(changed));
        }
        columns.add(factorColumn(changed));
        columns.add(noteColumn(changed));
        table.getColumns().setAll(columns);
        table.setPlaceholder(new Label(DialogUtil.message("attendance.empty")));
        fitHeight(table, attendances.size());
        return table;
    }

    private TableColumn<Attendance, BigDecimal> wageColumn(Runnable changed) {
        TableColumn<Attendance, BigDecimal> column = new TableColumn<>(DialogUtil.message("attendance.field.wage"));
        column.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getDailyWage()));
        column.setCellFactory(TextFieldTableCell.forTableColumn(new MoneyConverter()));
        column.setComparator(Comparator.nullsLast(Comparator.naturalOrder()));
        column.setOnEditCommit(e -> save(e.getRowValue(), e.getNewValue(), e.getRowValue().getDayFactor(),
                e.getRowValue().getNote(), changed));
        return column;
    }

    private TableColumn<Attendance, BigDecimal> factorColumn(Runnable changed) {
        TableColumn<Attendance, BigDecimal> column =
                new TableColumn<>(DialogUtil.message("attendance.field.dayFactor"));
        column.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getDayFactor()));
        column.setCellFactory(ComboBoxTableCell.forTableColumn(new FactorConverter(),
                AttendanceMath.FULL_DAY, AttendanceMath.HALF_DAY));
        column.setComparator(Comparator.nullsLast(Comparator.naturalOrder()));
        column.setOnEditCommit(e -> save(e.getRowValue(), e.getRowValue().getDailyWage(), e.getNewValue(),
                e.getRowValue().getNote(), changed));
        return column;
    }

    private TableColumn<Attendance, String> noteColumn(Runnable changed) {
        TableColumn<Attendance, String> column = new TableColumn<>(DialogUtil.message("attendance.field.note"));
        column.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNote()));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setComparator(TableSorting.turkishText());
        column.setOnEditCommit(e -> save(e.getRowValue(), e.getRowValue().getDailyWage(),
                e.getRowValue().getDayFactor(), e.getNewValue(), changed));
        column.setPrefWidth(NOTE_WIDTH);
        return column;
    }

    private void save(Attendance row, BigDecimal wage, BigDecimal factor, String note, Runnable changed) {
        try {
            if (!confirmFactorChange(row, factor)) {
                changed.run();
                return;
            }
            attendanceService.update(row.getId(), wage, factor, note);
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        changed.run();
    }

    /** Raising a day factor over one full day for the employee's date is warned, not blocked. */
    private boolean confirmFactorChange(Attendance row, BigDecimal factor) {
        boolean factorChanged = factor != null && row.getDayFactor().compareTo(factor) != 0;
        return !factorChanged || !attendanceService.exceedsFullDayAfterUpdate(row.getId(), factor)
                || DialogUtil.confirm("attendance.confirm.overFullDay", row.getEmployee().getName(),
                        Bicimlendirici.date(row.getAttendanceDate()));
    }

    private void deleteSelected(TableView<Attendance> table, Runnable changed) {
        Attendance selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        String description = DialogUtil.message("delete.single.attendance", selected.getEmployee().getName(),
                Bicimlendirici.date(selected.getAttendanceDate()), Bicimlendirici.factor(selected.getDayFactor()));
        if (!DeleteConfirmation.confirmSingle(description)) {
            return;
        }
        try {
            attendanceService.delete(selected.getId());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        changed.run();
    }

    private static void fitHeight(TableView<?> table, int rowCount) {
        table.setPrefHeight(Math.min(MAX_TABLE_HEIGHT, HEADER_HEIGHT + Math.max(rowCount, 1) * ROW_HEIGHT));
    }

    private static final class MoneyConverter extends StringConverter<BigDecimal> {
        @Override
        public String toString(BigDecimal value) {
            return Bicimlendirici.money(value);
        }

        @Override
        public BigDecimal fromString(String text) {
            try {
                return Bicimlendirici.parseMoney(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private static final class FactorConverter extends StringConverter<BigDecimal> {
        @Override
        public String toString(BigDecimal value) {
            return Bicimlendirici.factor(value);
        }

        @Override
        public BigDecimal fromString(String string) {
            return null;
        }
    }
}
