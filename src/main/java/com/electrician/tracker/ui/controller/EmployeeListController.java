package com.electrician.tracker.ui.controller;

import java.util.List;

import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.ContentNavigator;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.TaskRunner;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class EmployeeListController {

    private static final String FORM_FXML = "/fxml/employee_form.fxml";
    private static final String ATTENDANCE_FXML = "/fxml/employee_attendance.fxml";
    private static final int DOUBLE_CLICK = 2;
    private static final String YES = "Evet";
    private static final String NO = "Hayır";

    private final EmployeeService employeeService;
    private final ModalStageOpener modalStageOpener;
    private final TaskRunner taskRunner;
    private final ContentNavigator contentNavigator;

    @FXML
    private TableView<Employee> table;
    @FXML
    private TableColumn<Employee, String> nameColumn;
    @FXML
    private TableColumn<Employee, String> wageColumn;
    @FXML
    private TableColumn<Employee, String> masterColumn;
    @FXML
    private TableColumn<Employee, String> activeColumn;
    @FXML
    private ProgressIndicator loadingIndicator;

    public EmployeeListController(EmployeeService employeeService, ModalStageOpener modalStageOpener,
            TaskRunner taskRunner, ContentNavigator contentNavigator) {
        this.employeeService = employeeService;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
        this.contentNavigator = contentNavigator;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        wageColumn.setCellValueFactory(
                data -> new SimpleStringProperty(Bicimlendirici.money(data.getValue().getDefaultDailyWage())));
        masterColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().isMaster() ? YES : NO));
        activeColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().isActive() ? YES : NO));
        table.setRowFactory(tv -> {
            TableRow<Employee> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == DOUBLE_CLICK && !row.isEmpty()) {
                    openAttendanceDetail(row.getItem());
                }
            });
            return row;
        });
        refresh();
    }

    private void openAttendanceDetail(Employee employee) {
        contentNavigator.<EmployeeAttendanceController>show(ATTENDANCE_FXML,
                controller -> controller.selectEmployee(employee.getId()));
    }

    private void refresh() {
        taskRunner.run(employeeService::findAll, this::showEmployees, loadingIndicator, table);
    }

    private void showEmployees(List<Employee> employees) {
        table.setItems(FXCollections.observableArrayList(employees));
    }

    @FXML
    private void onNew() {
        EmployeeFormController controller = modalStageOpener.openAndWait(
                FORM_FXML, "employee.dialog.new", table.getScene().getWindow());
        if (controller.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onEdit() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        EmployeeFormController controller = modalStageOpener.openAndWait(
                FORM_FXML, "employee.dialog.edit", table.getScene().getWindow(),
                c -> c.editExisting(selected));
        if (controller.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onToggleActive() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        employeeService.setActive(selected.getId(), !selected.isActive());
        refresh();
    }

    @FXML
    private void onDelete() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        if (!DialogUtil.confirm("employee.confirm.delete")) {
            return;
        }
        try {
            employeeService.delete(selected.getId());
            refresh();
        } catch (ReferencedEntityException ex) {
            offerDeactivation(selected, ex);
        }
    }

    /** An employee with attendance cannot be deleted; an active one can be retired instead. */
    private void offerDeactivation(Employee employee, ReferencedEntityException ex) {
        if (!employee.isActive()) {
            DialogUtil.showError(ex);
            return;
        }
        if (DialogUtil.confirm("employee.delete.offerDeactivate", ex.getReferenceCount())) {
            employeeService.setActive(employee.getId(), false);
            refresh();
        }
    }
}
