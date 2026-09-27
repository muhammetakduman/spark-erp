package com.electrician.tracker.ui.controller;

import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class EmployeeFormController {

    private final EmployeeService employeeService;

    @FXML
    private TextField nameField;
    @FXML
    private DecimalField wageField;
    @FXML
    private CheckBox masterCheckBox;
    @FXML
    private CheckBox activeCheckBox;

    private Long editingId;
    private boolean saved;
    private Employee result;

    public EmployeeFormController(EmployeeService employeeService) {
        this.employeeService = employeeService;
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
