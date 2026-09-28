package com.electrician.tracker.ui.controller;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.ContentNavigator;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EmptyState;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.TaskRunner;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class EmployeeListController {

    private static final String FORM_FXML = "/fxml/employee_form.fxml";
    private static final String ATTENDANCE_FXML = "/fxml/employee_attendance.fxml";
    private static final int DOUBLE_CLICK = 2;

    private final EmployeeService employeeService;
    private final ModalStageOpener modalStageOpener;
    private final TaskRunner taskRunner;
    private final ContentNavigator contentNavigator;
    private final AccessControl accessControl;

    @FXML
    private TableView<Employee> table;
    @FXML
    private TableColumn<Employee, String> nameColumn;
    @FXML
    private TableColumn<Employee, BigDecimal> wageColumn;
    @FXML
    private Button deleteButton;
    @FXML
    private TableColumn<Employee, String> masterColumn;
    @FXML
    private TableColumn<Employee, String> activeColumn;
    @FXML
    private ProgressIndicator loadingIndicator;

    public EmployeeListController(EmployeeService employeeService, ModalStageOpener modalStageOpener,
            TaskRunner taskRunner, ContentNavigator contentNavigator, AccessControl accessControl) {
        this.employeeService = employeeService;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
        this.contentNavigator = contentNavigator;
        this.accessControl = accessControl;
    }

    @FXML
    private void initialize() {
        table.setPlaceholder(EmptyState.of(AppIcon.EMPLOYEES, "employees.emptyState",
                "employee.action.new", this::onNew));
        TableSorting.text(nameColumn, Employee::getName);
        if (accessControl.canViewFinancials()) {
            TableSorting.money(wageColumn, Employee::getDefaultDailyWage);
        } else {
            table.getColumns().remove(wageColumn);
        }
        TableSorting.text(masterColumn, employee -> yesNo(employee.isMaster()));
        TableSorting.text(activeColumn, employee -> yesNo(employee.isActive()));
        deleteButton.setVisible(accessControl.isAdmin());
        deleteButton.setManaged(accessControl.isAdmin());
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

    private static String yesNo(boolean value) {
        return DialogUtil.message(value ? "common.yes" : "common.no");
    }

    private void openAttendanceDetail(Employee employee) {
        contentNavigator.<EmployeeAttendanceController>show(ATTENDANCE_FXML,
                controller -> controller.selectEmployee(employee.getId()));
    }

    private void refresh() {
        taskRunner.run(employeeService::findAll, this::showEmployees, loadingIndicator, table);
    }

    private void showEmployees(List<Employee> employees) {
        table.setItems(TableSorting.sorted(employees, TableSorting.employees()));
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
