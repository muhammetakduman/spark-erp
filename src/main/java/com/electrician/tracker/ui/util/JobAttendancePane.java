package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.dto.EmployeeWageSummary;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.service.AttendanceMath;
import com.electrician.tracker.service.AttendanceService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
 * Content of a job's "Puantaj" tab: a per-employee summary and the raw
 * attendance rows (editable and deletable). Attendance is only recorded; it
 * never changes the job's profit. Every change goes through
 * {@link AttendanceService}; the caller refreshes.
 */
public final class JobAttendancePane {

    private static final double ROW_HEIGHT = 28;
    private static final double HEADER_HEIGHT = 40;
    private static final double MAX_TABLE_HEIGHT = 240;
    private static final double SPACING = 10;

    private final AttendanceService attendanceService;
    private final Consumer<Long> onChanged;

    /** {@code onChanged} receives the id of the job whose attendance changed. */
    public JobAttendancePane(AttendanceService attendanceService, Consumer<Long> onChanged) {
        this.attendanceService = attendanceService;
        this.onChanged = onChanged;
    }

    /** "Puantaj (18 gün)". */
    public static String tabTitle(JobSummary summary) {
        return DialogUtil.message("jobDetail.tab.team", Bicimlendirici.days(summary.attendanceDayCount()));
    }

    public VBox build(Job job, JobSummary summary, List<Attendance> attendances) {
        Runnable changed = () -> onChanged.accept(job.getId());
        TableView<Attendance> rawTable = buildRawTable(attendances, changed);
        rawTable.setTooltip(new Tooltip(DialogUtil.message("attendance.rows.tooltip")));
        Button deleteButton = new Button(DialogUtil.message("attendance.action.deleteSelected"));
        deleteButton.setOnAction(e -> deleteSelected(rawTable, changed));
        HBox buttons = new HBox(SPACING, deleteButton);
        buttons.getStyleClass().add("table-actions");

        Label rawTitle = new Label(DialogUtil.message("attendance.section.rows"));
        rawTitle.getStyleClass().add("subsection-title");
        return new VBox(SPACING,
                buildSummaryTable(summary.employeeWageSummaries()),
                rawTitle,
                rawTable,
                buttons);
    }

    private TableView<EmployeeWageSummary> buildSummaryTable(List<EmployeeWageSummary> summaries) {
        TableView<EmployeeWageSummary> table = new TableView<>(FXCollections.observableArrayList(summaries));
        table.getColumns().setAll(List.of(
                column("attendance.field.employee", s -> s.employeeName()
                        + (s.master() ? " (" + DialogUtil.message("employee.field.master") + ")" : "")),
                column("attendance.field.dayCount", s -> Bicimlendirici.days(s.dayCount())),
                column("attendance.field.averageWage", s -> Bicimlendirici.money(s.averageWage())),
                column("attendance.field.total", s -> Bicimlendirici.money(s.totalWage())),
                column("attendance.field.workedDays", s -> Bicimlendirici.workedDays(s.days()))));
        table.setPlaceholder(new Label(DialogUtil.message("attendance.empty")));
        fitHeight(table, summaries.size());
        return table;
    }

    private TableView<Attendance> buildRawTable(List<Attendance> attendances, Runnable changed) {
        TableView<Attendance> table = new TableView<>(FXCollections.observableArrayList(attendances));
        table.setEditable(true);

        TableColumn<Attendance, String> dateColumn = column("attendance.field.date",
                a -> a.getAttendanceDate().format(Bicimlendirici.DATE));
        TableColumn<Attendance, String> employeeColumn = column("attendance.field.employee",
                a -> a.getEmployee().getName());

        TableColumn<Attendance, BigDecimal> wageColumn = new TableColumn<>(DialogUtil.message("attendance.field.wage"));
        wageColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getDailyWage()));
        wageColumn.setCellFactory(TextFieldTableCell.forTableColumn(new MoneyConverter()));
        wageColumn.setOnEditCommit(e -> save(e.getRowValue(), e.getNewValue(), e.getRowValue().getDayFactor(),
                e.getRowValue().getNote(), changed));

        TableColumn<Attendance, BigDecimal> factorColumn = new TableColumn<>(DialogUtil.message("attendance.field.dayFactor"));
        factorColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getDayFactor()));
        factorColumn.setCellFactory(ComboBoxTableCell.forTableColumn(new FactorConverter(),
                AttendanceMath.FULL_DAY, AttendanceMath.HALF_DAY));
        factorColumn.setOnEditCommit(e -> save(e.getRowValue(), e.getRowValue().getDailyWage(), e.getNewValue(),
                e.getRowValue().getNote(), changed));

        TableColumn<Attendance, String> noteColumn = new TableColumn<>(DialogUtil.message("attendance.field.note"));
        noteColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNote()));
        noteColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        noteColumn.setOnEditCommit(e -> save(e.getRowValue(), e.getRowValue().getDailyWage(),
                e.getRowValue().getDayFactor(), e.getNewValue(), changed));
        noteColumn.setPrefWidth(200);

        table.getColumns().setAll(List.of(dateColumn, employeeColumn, wageColumn, factorColumn, noteColumn));
        table.setPlaceholder(new Label(DialogUtil.message("attendance.empty")));
        fitHeight(table, attendances.size());
        return table;
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
        if (!DialogUtil.confirm("attendance.confirm.delete")) {
            return;
        }
        try {
            attendanceService.delete(selected.getId());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        changed.run();
    }

    private static <T> TableColumn<T, String> column(String titleKey, java.util.function.Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(DialogUtil.message(titleKey));
        column.setCellValueFactory(d -> new SimpleStringProperty(value.apply(d.getValue())));
        return column;
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
