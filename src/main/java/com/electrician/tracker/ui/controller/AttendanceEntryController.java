package com.electrician.tracker.ui.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.dto.AttendanceConflict;
import com.electrician.tracker.dto.AttendanceEntry;
import com.electrician.tracker.dto.AttendancePreview;
import com.electrician.tracker.dto.AttendanceSaveResult;
import com.electrician.tracker.service.AttendanceMath;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.AttendanceService;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.service.JobLabels;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.SelectionLists;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Attendance entry for any job (site or service, active or completed):
 * a single day or a date range × the checked employees, one row per
 * date × employee. A MANAGER picks people and days only: the wage column
 * does not exist and the service takes each employee's default wage.
 */
@Component
@Scope("prototype")
public class AttendanceEntryController {

    private static final int MAX_CONFLICT_LINES = 10;
    private static final String EMPLOYEE_FORM_FXML = "/fxml/employee_form.fxml";

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final ModalStageOpener modalStageOpener;
    private final AccessControl accessControl;

    @FXML
    private Label jobTitleLabel;
    @FXML
    private RadioButton rangeRadio;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private Label startLabel;
    @FXML
    private Label endLabel;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private HBox weekendBox;
    @FXML
    private CheckBox skipSaturdayCheckBox;
    @FXML
    private CheckBox skipSundayCheckBox;
    @FXML
    private GridPane employeeGrid;
    @FXML
    private CheckBox showInactiveCheckBox;
    @FXML
    private Label noEmployeeLabel;
    @FXML
    private Label previewLabel;

    private final List<EmployeeRow> rows = new ArrayList<>();
    private Job job;
    private boolean saved;

    public AttendanceEntryController(AttendanceService attendanceService, EmployeeService employeeService,
            ModalStageOpener modalStageOpener, AccessControl accessControl) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
        this.modalStageOpener = modalStageOpener;
        this.accessControl = accessControl;
    }

    public void setJob(Job job) {
        this.job = job;
        jobTitleLabel.setText(JobLabels.full(job));
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        rangeRadio.selectedProperty().addListener((obs, o, n) -> applyDateMode());
        startDatePicker.valueProperty().addListener((obs, o, n) -> refreshPreview());
        endDatePicker.valueProperty().addListener((obs, o, n) -> refreshPreview());
        skipSaturdayCheckBox.selectedProperty().addListener((obs, o, n) -> refreshPreview());
        skipSundayCheckBox.selectedProperty().addListener((obs, o, n) -> refreshPreview());
        showInactiveCheckBox.selectedProperty().addListener((obs, o, n) -> layoutEmployeeRows());
        for (Employee employee : employeeService.findAll()) {
            rows.add(new EmployeeRow(employee, this::refreshPreview, accessControl.canViewFinancials()));
        }
        layoutEmployeeRows();
        applyDateMode();
    }

    /**
     * Passive employees are hidden unless asked for. If nobody is listed the
     * dialog says so instead of showing an empty list that silently saves
     * nothing (the cause of "attendance cannot be entered on some sites").
     */
    private void layoutEmployeeRows() {
        employeeGrid.getChildren().clear();
        boolean showInactive = showInactiveCheckBox.isSelected();
        List<EmployeeRow> visible = rows.stream()
                .filter(row -> showInactive || row.isActive() || row.isSelected())
                .sorted(SelectionLists.byName(EmployeeRow::name))
                .toList();
        for (int i = 0; i < visible.size(); i++) {
            employeeGrid.addRow(i, visible.get(i).cells());
        }
        noEmployeeLabel.setVisible(visible.isEmpty());
        noEmployeeLabel.setManaged(visible.isEmpty());
        refreshPreview();
    }

    @FXML
    private void onNewEmployee() {
        EmployeeFormController controller = modalStageOpener.openAndWait(
                EMPLOYEE_FORM_FXML, "employee.dialog.new", employeeGrid.getScene().getWindow());
        if (controller.isSaved() && controller.getResult() != null) {
            EmployeeRow row = new EmployeeRow(controller.getResult(), this::refreshPreview,
                    accessControl.canViewFinancials());
            row.select();
            rows.add(row);
            layoutEmployeeRows();
        }
    }

    private void applyDateMode() {
        boolean range = rangeRadio.isSelected();
        for (Node node : List.of(endLabel, endDatePicker, weekendBox)) {
            node.setVisible(range);
            node.setManaged(range);
        }
        startLabel.setText(DialogUtil.message(range ? "attendance.field.startDate" : "attendance.field.date"));
        refreshPreview();
    }

    private void refreshPreview() {
        try {
            AttendancePreview preview = attendanceService.preview(selectedDates(), selectedEntries());
            String dates = String.valueOf(preview.dateCount());
            String people = String.valueOf(preview.personCount());
            String days = Bicimlendirici.days(preview.dayCount());
            previewLabel.setText(preview.amount() == null
                    ? DialogUtil.message("attendance.previewNoAmount", dates, people, days)
                    : DialogUtil.message("attendance.preview", dates, people, days,
                            Bicimlendirici.money(preview.amount())));
        } catch (RuntimeException e) {
            // Incomplete input (missing date, unparsable wage) simply hides the preview.
            previewLabel.setText("");
        }
    }

    private List<LocalDate> selectedDates() {
        if (!rangeRadio.isSelected()) {
            return startDatePicker.getValue() == null ? List.of() : List.of(startDatePicker.getValue());
        }
        return attendanceService.expandDates(startDatePicker.getValue(), endDatePicker.getValue(),
                skipSaturdayCheckBox.isSelected(), skipSundayCheckBox.isSelected());
    }

    private List<AttendanceEntry> selectedEntries() {
        return rows.stream().filter(EmployeeRow::isSelected).map(EmployeeRow::toEntry).toList();
    }

    @FXML
    private void onSave() {
        try {
            List<LocalDate> dates = selectedDates();
            List<AttendanceEntry> entries = selectedEntries();
            if (!confirmDatesOutsideJob(attendanceService.findDatesOutsideJob(job.getId(), dates))
                    || !confirmConflicts(attendanceService.findConflicts(job.getId(), dates, entries))) {
                return;
            }
            AttendanceSaveResult result = attendanceService.saveBatch(job.getId(), dates, entries);
            saved = true;
            if (result.skippedCount() > 0) {
                showInfo(DialogUtil.message("attendance.info.skipped",
                        String.valueOf(result.createdCount()), String.valueOf(result.skippedCount())));
            }
            closeStage();
        } catch (NumberFormatException e) {
            DialogUtil.showErrorMessage("error.attendance.wage.invalid");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    /** Other-job overlaps are only a warning: the user may still add the rows. */
    private boolean confirmConflicts(List<AttendanceConflict> conflicts) {
        if (conflicts.isEmpty()) {
            return true;
        }
        String lines = conflicts.stream()
                .limit(MAX_CONFLICT_LINES)
                .map(this::conflictLine)
                .collect(Collectors.joining("\n"));
        if (conflicts.size() > MAX_CONFLICT_LINES) {
            lines += "\n" + DialogUtil.message("attendance.conflict.more",
                    String.valueOf(conflicts.size() - MAX_CONFLICT_LINES));
        }
        return DialogUtil.confirmText(lines + "\n\n" + DialogUtil.message("attendance.conflict.question"));
    }

    private String conflictLine(AttendanceConflict conflict) {
        String line = DialogUtil.message("attendance.conflict.line", conflict.employeeName(),
                Bicimlendirici.date(conflict.date()), conflict.jobLabel());
        if (conflict.exceedsFullDay()) {
            line += " " + DialogUtil.message("attendance.conflict.overFullDay",
                    Bicimlendirici.days(conflict.dayFactorTotal()));
        }
        return line;
    }

    /** Dates outside the job's start–end range are only a warning. */
    private boolean confirmDatesOutsideJob(List<LocalDate> outside) {
        if (outside.isEmpty()) {
            return true;
        }
        String dates = outside.stream()
                .limit(MAX_CONFLICT_LINES)
                .map(Bicimlendirici::date)
                .collect(Collectors.joining(", "));
        if (outside.size() > MAX_CONFLICT_LINES) {
            dates += " …";
        }
        return DialogUtil.confirm("attendance.confirm.outsideJobRange", dates, Bicimlendirici.date(job.getStartDate()),
                job.getEndDate() == null ? "—" : Bicimlendirici.date(job.getEndDate()));
    }

    private void showInfo(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void onCancel() {
        closeStage();
    }

    private void closeStage() {
        ((Stage) previewLabel.getScene().getWindow()).close();
    }

    private static final class EmployeeRow {
        private final Employee employee;
        private final CheckBox checkBox = new CheckBox();
        private final DecimalField wageField = new DecimalField();
        private final boolean showWage;
        private final ComboBox<BigDecimal> factorCombo = new ComboBox<>();
        private final Node nameCell;

        private EmployeeRow(Employee employee, Runnable onChange, boolean showWage) {
            this.employee = employee;
            this.showWage = showWage;
            wageField.setValue(employee.getDefaultDailyWage());
            factorCombo.getItems().setAll(AttendanceMath.FULL_DAY, AttendanceMath.HALF_DAY);
            factorCombo.setValue(AttendanceMath.FULL_DAY);
            factorCombo.setConverter(new FactorConverter());
            nameCell = buildNameCell(employee);
            checkBox.selectedProperty().addListener((obs, o, n) -> onChange.run());
            wageField.textProperty().addListener((obs, o, n) -> onChange.run());
            factorCombo.valueProperty().addListener((obs, o, n) -> onChange.run());
        }

        private static Node buildNameCell(Employee employee) {
            HBox cell = new HBox(6, new Label(employee.getName()));
            if (employee.isMaster()) {
                cell.getChildren().add(tag("employee.field.master", "master-tag"));
            }
            if (!employee.isActive()) {
                cell.getChildren().add(tag("attendance.tag.inactive", "inactive-tag"));
            }
            return cell;
        }

        private static Label tag(String messageKey, String styleClass) {
            Label tag = new Label(DialogUtil.message(messageKey));
            tag.getStyleClass().add(styleClass);
            return tag;
        }

        private Node[] cells() {
            return showWage ? new Node[] { checkBox, nameCell, wageField, factorCombo }
                    : new Node[] { checkBox, nameCell, factorCombo };
        }

        private boolean isSelected() {
            return checkBox.isSelected();
        }

        private void select() {
            checkBox.setSelected(true);
        }

        private boolean isActive() {
            return employee.isActive();
        }

        private String name() {
            return employee.getName();
        }

        /** Without a visible wage field the wage is left to the service (the employee's default). */
        private AttendanceEntry toEntry() {
            return new AttendanceEntry(employee.getId(), showWage ? wageField.getValue() : null,
                    factorCombo.getValue());
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
