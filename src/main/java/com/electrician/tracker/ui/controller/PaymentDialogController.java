package com.electrician.tracker.ui.controller;

import java.time.LocalDate;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.domain.PaymentMethod;
import com.electrician.tracker.service.PaymentService;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PaymentDialogController {

    private final PaymentService paymentService;

    @FXML
    private DatePicker datePicker;
    @FXML
    private DecimalField amountField;
    @FXML
    private ComboBox<PaymentMethod> methodComboBox;
    @FXML
    private TextField noteField;

    private Job job;
    private Long editingId;
    private boolean saved;

    public PaymentDialogController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    /** Edits a saved payment instead of adding a new one. */
    public void editExisting(Payment payment) {
        this.job = payment.getJob();
        this.editingId = payment.getId();
        datePicker.setValue(payment.getPaymentDate());
        amountField.setValue(payment.getAmount());
        methodComboBox.setValue(payment.getMethod());
        noteField.setText(payment.getNote());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void initialize() {
        datePicker.setValue(LocalDate.now());
        methodComboBox.getItems().setAll(PaymentMethod.values());
        methodComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(PaymentMethod method) {
                return EnumLabels.label(method);
            }

            @Override
            public PaymentMethod fromString(String string) {
                return null;
            }
        });
    }

    @FXML
    private void onSave() {
        try {
            Payment payment = new Payment(job, datePicker.getValue(), amountField.getValue(),
                    methodComboBox.getValue(), noteField.getText());
            if (editingId == null) {
                paymentService.addPayment(payment);
            } else {
                paymentService.update(editingId, payment);
            }
            saved = true;
            closeStage();
        } catch (NumberFormatException e) {
            DialogUtil.showErrorMessage("error.payment.amount.invalid");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onCancel() {
        closeStage();
    }

    private void closeStage() {
        ((Stage) datePicker.getScene().getWindow()).close();
    }
}
