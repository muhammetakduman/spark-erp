package com.electrician.tracker.ui.controller.employee;

import com.electrician.tracker.domain.employee.Employee;
import com.electrician.tracker.service.user.AccessControl;
import com.electrician.tracker.service.employee.EmployeeService;
import com.electrician.tracker.ui.util.control.DecimalField;
import com.electrician.tracker.ui.util.dialog.DialogUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class EmployeeFormController {

    private final EmployeeService employeeService;
    private final AccessControl accessControl;

    @FXML
    private TextField nameField;
    @FXML
    private Label wageLabel;
    @FXML
    private DecimalField wageField;
    @FXML
    private CheckBox masterCheckBox;
    @FXML
    private CheckBox activeCheckBox;

    private Long editingId;
    private boolean saved;
    private Employee result;

    public EmployeeFormController(EmployeeService employeeService, AccessControl accessControl) {
        this.employeeService = employeeService;
        this.accessControl = accessControl;
    }

    /** The wage field does not exist for users who may not see wages (the service keeps the stored one). */
    @FXML
    private void initialize() {
        boolean showWage = accessControl.canViewFinancials();
        for (javafx.scene.Node node : java.util.List.of(wageLabel, wageField)) {
            node.setVisible(showWage);
            node.setManaged(showWage);
        }
    }

    public void editExisting(Employee employee) {
        this.editingId = employee.getId();
        nameField.setText(employee.getName());
        wageField.setValue(employee.getDefaultDailyWage());
        masterCheckBox.setSelected(employee.isMaster());
        activeCheckBox.setSelected(employee.isActive());
    }

    public boolean isSaved() {
        return saved;
    }

    /** The saved employee, so a caller can add and select it without reloading. */
    public Employee getResult() {
        return result;
    }

    @FXML
    private void onSave(ActionEvent event) {
        try {
            Employee employee = new Employee(nameField.getText(), wageField.getValue(),
                    masterCheckBox.isSelected(), activeCheckBox.isSelected());
            result = editingId == null
                    ? employeeService.create(employee)
                    : employeeService.update(editingId, employee);
            saved = true;
            closeStage(event);
        } catch (NumberFormatException e) {
            DialogUtil.showErrorMessage("error.employee.wage.invalid");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onCancel(ActionEvent event) {
        closeStage(event);
    }

    private void closeStage(ActionEvent event) {
        ((Stage) ((Button) event.getSource()).getScene().getWindow()).close();
    }
}
