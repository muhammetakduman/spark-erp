package com.electrician.tracker.ui.controller;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.JobService;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.SelectionLists;
import com.electrician.tracker.ui.util.VatSelector;
import java.math.BigDecimal;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Şantiye formu" — creates/edits the SITE-type basic job fields. Service
 * jobs are created through {@link ServiceFormController} instead.
 */
@Component
@Scope("prototype")
public class JobFormController {

    private static final String CUSTOMER_FORM_FXML = "/fxml/customer_form.fxml";

    private final JobService jobService;
    private final JobSummaryService jobSummaryService;
    private final CustomerService customerService;
    private final ModalStageOpener modalStageOpener;

    @FXML
    private ComboBox<Customer> customerComboBox;
    @FXML
    private TextField nameField;
    @FXML
    private TextField addressField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private ComboBox<JobStatus> statusComboBox;
    @FXML
    private DecimalField serviceFeeField;
    @FXML
    private DecimalField laborFeeField;
    @FXML
    private VatSelector serviceFeeVatSelector;
    @FXML
    private VatSelector laborFeeVatSelector;

    private Long editingId;
    private JobStatus editingStatus;
    private boolean saved;

    public JobFormController(JobService jobService, JobSummaryService jobSummaryService,
            CustomerService customerService, ModalStageOpener modalStageOpener) {
        this.jobService = jobService;
        this.jobSummaryService = jobSummaryService;
        this.customerService = customerService;
        this.modalStageOpener = modalStageOpener;
    }

    @FXML
    private void initialize() {
        SelectionLists.setSorted(customerComboBox, customerService.findAll(), Customer::getName);
        customerComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName();
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        });

        statusComboBox.getItems().setAll(JobStatus.values());
        statusComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(JobStatus value) {
                return EnumLabels.label(value);
            }

            @Override
            public JobStatus fromString(String string) {
                return null;
            }
        });
        statusComboBox.setValue(JobStatus.ACTIVE);
        serviceFeeVatSelector.bindAmount(serviceFeeField);
        laborFeeVatSelector.bindAmount(laborFeeField);
    }

    public void editExisting(Job job) {
        this.editingId = job.getId();
        this.editingStatus = job.getStatus();
        customerComboBox.getItems().stream()
                .filter(c -> c.getId().equals(job.getCustomer().getId()))
                .findFirst()
                .ifPresent(customerComboBox::setValue);
        nameField.setText(job.getName());
        addressField.setText(job.getAddress());
        startDatePicker.setValue(job.getStartDate());
        endDatePicker.setValue(job.getEndDate());
        statusComboBox.setValue(job.getStatus());
        serviceFeeField.setValue(job.getServiceFee());
        laborFeeField.setValue(job.getLaborFee());
        serviceFeeVatSelector.setValue(job.getServiceFeeVatRate(), job.getServiceFeeVatIncluded());
        laborFeeVatSelector.setValue(job.getLaborFeeVatRate(), job.getLaborFeeVatIncluded());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onNewCustomer() {
        CustomerFormController controller = modalStageOpener.openAndWait(
                CUSTOMER_FORM_FXML, "customer.dialog.new", customerComboBox.getScene().getWindow());
        if (controller.isSaved() && controller.getResult() != null) {
            SelectionLists.addSortedAndSelect(customerComboBox, controller.getResult(), Customer::getName,
                    Customer::getId);
        }
    }

    @FXML
    private void onSave(ActionEvent event) {
        try {
            Job job = new Job(customerComboBox.getValue(), JobType.SITE, nameField.getText(), addressField.getText(),
                    startDatePicker.getValue(), endDatePicker.getValue(), statusComboBox.getValue(),
                    serviceFeeField.getValue(), laborFeeField.getValue(),
                    false, null);
            job.setServiceFeeVat(serviceFeeVatSelector.getRate(), serviceFeeVatSelector.getIncluded());
            job.setLaborFeeVat(laborFeeVatSelector.getRate(), laborFeeVatSelector.getIncluded());
            if (!confirmCompletion(job.getStatus())) {
                return;
            }
            if (editingId == null) {
                jobService.create(job);
            } else {
                jobService.update(editingId, job);
            }
            saved = true;
            closeStage(event);
        } catch (NumberFormatException e) {
            DialogUtil.showErrorMessage("error.job.fee.invalid");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    /** Completing a site that still has money owed is allowed, but only after a warning. */
    private boolean confirmCompletion(JobStatus newStatus) {
        boolean completing = editingId != null && newStatus == JobStatus.COMPLETED
                && editingStatus != JobStatus.COMPLETED;
        if (!completing) {
            return true;
        }
        BigDecimal outstanding = jobSummaryService.outstandingBalance(editingId);
        return outstanding.signum() == 0
                || DialogUtil.confirm("job.confirm.completeWithBalance", Bicimlendirici.money(outstanding));
    }

    @FXML
    private void onCancel(ActionEvent event) {
        closeStage(event);
    }

    private void closeStage(ActionEvent event) {
        ((Stage) ((Button) event.getSource()).getScene().getWindow()).close();
    }
}
