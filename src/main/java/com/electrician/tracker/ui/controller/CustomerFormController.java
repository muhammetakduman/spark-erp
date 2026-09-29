package com.electrician.tracker.ui.controller;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.ui.util.DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CustomerFormController {

    private final CustomerService customerService;

    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField taxNoField;
    @FXML
    private TextArea noteField;
    @FXML
    private CheckBox activeCheckBox;

    private Long editingId;
    private boolean saved;
    private Customer result;

    public CustomerFormController(CustomerService customerService) {
        this.customerService = customerService;
    }

    public void editExisting(Customer customer) {
        this.editingId = customer.getId();
        nameField.setText(customer.getName());
        phoneField.setText(customer.getPhone());
        addressField.setText(customer.getAddress());
        emailField.setText(customer.getEmail());
        taxNoField.setText(customer.getTaxNo());
        noteField.setText(customer.getNote());
        activeCheckBox.setSelected(customer.isActive());
    }

    public boolean isSaved() {
        return saved;
    }

    public Customer getResult() {
        return result;
    }

    @FXML
    private void onSave(javafx.event.ActionEvent event) {
        Customer customer = new Customer(nameField.getText(), phoneField.getText(), addressField.getText(),
                taxNoField.getText(), noteField.getText());
        customer.setEmail(emailField.getText() == null || emailField.getText().isBlank() ? null
                : emailField.getText().trim());
        customer.setActive(activeCheckBox.isSelected());
        try {
            if (editingId == null) {
                result = customerService.create(customer);
            } else {
                result = customerService.update(editingId, customer);
            }
            saved = true;
            closeStage(event);
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onCancel(javafx.event.ActionEvent event) {
        closeStage(event);
    }

    private void closeStage(javafx.event.ActionEvent event) {
        ((Stage) ((Button) event.getSource()).getScene().getWindow()).close();
    }
}
