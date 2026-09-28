package com.electrician.tracker.ui.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.dto.JobDetail;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.service.PaymentService;
import com.electrician.tracker.service.QuoteService;
import com.electrician.tracker.service.ServiceJobService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DecimalField;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.JobPaymentPane;
import com.electrician.tracker.ui.util.MaterialColumns;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.SelectionLists;
import com.electrician.tracker.ui.util.VatSelector;
import com.electrician.tracker.ui.util.VatSummaryView;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Servis formu" — single-page create/edit form for a SERVICE job, including
 * its changed materials. The payments section exists only for an ADMIN; a
 * MANAGER can remove only lines that are not saved yet (a saved line is a
 * delete, which is ADMIN only).
 */
@Component
@Scope("prototype")
public class ServiceFormController {

    private static final String CUSTOMER_FORM_FXML = "/fxml/customer_form.fxml";
    private static final String MATERIAL_DIALOG_FXML = "/fxml/material_dialog.fxml";
    private static final String PAYMENT_DIALOG_FXML = "/fxml/payment_dialog.fxml";

    private final CustomerService customerService;
    private final ServiceJobService serviceJobService;
    private final JobSummaryService jobSummaryService;
    private final ModalStageOpener modalStageOpener;
    private final QuoteService quoteService;
    private final AccessControl accessControl;
    private final JobPaymentPane paymentPane;

    @FXML
    private ComboBox<Customer> customerComboBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField descriptionField;
    @FXML
    private DecimalField serviceFeeField;
    @FXML
    private DecimalField laborFeeField;
    @FXML
    private VatSelector serviceFeeVatSelector;
    @FXML
    private VatSelector laborFeeVatSelector;
    @FXML
    private TableView<MaterialItem> materialTable;
    @FXML
    private TableColumn<MaterialItem, String> materialProductColumn;
    @FXML
    private TableColumn<MaterialItem, String> materialQuantityColumn;
    @FXML
    private TableColumn<MaterialItem, String> materialVatColumn;
    @FXML
    private TableColumn<MaterialItem, MaterialItem> materialTotalColumn;
    @FXML
    private GridPane totalsGrid;
    @FXML
    private CheckBox paymentReceivedCheckBox;
    @FXML
    private VBox paymentSection;
    @FXML
    private VBox paymentPaneHolder;
    @FXML
    private Label paymentBalanceLabel;
    @FXML
    private Hyperlink quoteLink;

    private final ObservableList<MaterialItem> materialItems = FXCollections.observableArrayList();
    private boolean saved;
    private Long editingId;

    public ServiceFormController(CustomerService customerService, ServiceJobService serviceJobService,
            JobSummaryService jobSummaryService, PaymentService paymentService, ModalStageOpener modalStageOpener,
            QuoteService quoteService, AccessControl accessControl) {
        this.customerService = customerService;
        this.serviceJobService = serviceJobService;
        this.jobSummaryService = jobSummaryService;
        this.modalStageOpener = modalStageOpener;
        this.quoteService = quoteService;
        this.accessControl = accessControl;
        this.paymentPane = new JobPaymentPane(paymentService, jobId -> refreshPayments(), this::openEditPayment);
    }

    public boolean isSaved() {
        return saved;
    }

    /** Reopens a saved service: its fields, fee VAT, payment flag and material lines. */
    public void editExisting(Long jobId) {
        JobDetail detail = jobSummaryService.loadJobDetail(jobId);
        Job job = detail.job();
        this.editingId = job.getId();
        customerComboBox.getItems().stream()
                .filter(customer -> customer.getId().equals(job.getCustomer().getId()))
                .findFirst()
                .ifPresent(customerComboBox::setValue);
        datePicker.setValue(job.getStartDate());
        descriptionField.setText(job.getName());
        serviceFeeField.setValue(job.getServiceFee());
        laborFeeField.setValue(job.getLaborFee());
        serviceFeeVatSelector.setValue(job.getServiceFeeVatRate(), job.getServiceFeeVatIncluded());
        laborFeeVatSelector.setValue(job.getLaborFeeVatRate(), job.getLaborFeeVatIncluded());
        paymentReceivedCheckBox.setSelected(job.isPaymentReceived());
        materialItems.setAll(detail.materials());
        recomputeTotal();
        showQuoteLink(jobId);
        if (accessControl.canViewFinancials()) {
            paymentSection.setVisible(true);
            paymentSection.setManaged(true);
            showPayments(detail);
        }
    }

    /** "Bu iş 2026/0001 numaralı tekliften oluşturuldu", opening the quote. */
    private void showQuoteLink(Long jobId) {
        quoteService.findLinkForJob(jobId).ifPresent(link -> {
            quoteLink.setText(DialogUtil.message("job.fromQuote", link.quoteNo()));
            quoteLink.setOnAction(e -> modalStageOpener.<QuoteFormController>openAndWait(ViewPaths.QUOTE_FORM,
                    "quote.dialog.edit", quoteLink.getScene().getWindow(), form -> form.editExisting(link.quoteId())));
            quoteLink.setVisible(true);
            quoteLink.setManaged(true);
        });
    }

    /** Payments are saved right away (independently of the form's Save), so the list reloads from the database. */
    private void refreshPayments() {
        showPayments(jobSummaryService.loadJobDetail(editingId));
    }

    private void showPayments(JobDetail detail) {
        paymentPaneHolder.getChildren().setAll(paymentPane.build(editingId, detail.payments()));
        paymentBalanceLabel.setText(DialogUtil.message("service.payments.balance",
                Bicimlendirici.money(detail.summary().collectedTotal()),
                Bicimlendirici.money(detail.summary().remaining())));
    }

    @FXML
    private void onAddPayment() {
        Job job = jobSummaryService.loadJobDetail(editingId).job();
        PaymentDialogController controller = modalStageOpener.openAndWait(
                PAYMENT_DIALOG_FXML, "jobDetail.dialog.addPayment", paymentSection.getScene().getWindow(),
                c -> c.setJob(job));
        if (controller.isSaved()) {
            refreshPayments();
        }
    }

    private void openEditPayment(Payment payment) {
        PaymentDialogController controller = modalStageOpener.openAndWait(
                PAYMENT_DIALOG_FXML, "jobDetail.dialog.editPayment", paymentSection.getScene().getWindow(),
                c -> c.editExisting(payment));
        if (controller.isSaved()) {
            refreshPayments();
        }
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
        datePicker.setValue(LocalDate.now());
        setUpMaterialTable();

        serviceFeeField.textProperty().addListener((obs, o, n) -> recomputeTotal());
        laborFeeField.textProperty().addListener((obs, o, n) -> recomputeTotal());
        serviceFeeVatSelector.bindAmount(serviceFeeField);
        laborFeeVatSelector.bindAmount(laborFeeField);
        serviceFeeVatSelector.setOnChange(this::recomputeTotal);
        laborFeeVatSelector.setOnChange(this::recomputeTotal);
        recomputeTotal();
    }

    private void setUpMaterialTable() {
        materialTable.setItems(materialItems);
        MaterialColumns.bindProduct(materialProductColumn);
        materialQuantityColumn.setCellValueFactory(d -> new SimpleStringProperty(
                Bicimlendirici.quantity(d.getValue().getQuantity()) + " " + EnumLabels.label(d.getValue().getProduct().getUnit())));
        MaterialColumns.bindVat(materialVatColumn);
        MaterialColumns.bindTotal(materialTotalColumn);
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
    private void onAddMaterial() {
        MaterialDialogController controller = modalStageOpener.openAndWait(
                MATERIAL_DIALOG_FXML, "jobDetail.dialog.addMaterial", materialTable.getScene().getWindow(),
                c -> c.startDraft(datePicker.getValue()));
        materialItems.addAll(controller.getAddedItems());
        recomputeTotal();
    }

    @FXML
    private void onRemoveMaterial() {
        MaterialItem selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (selected.getId() != null && !accessControl.isAdmin()) {
            DialogUtil.showErrorMessage("error.access.adminOnly");
            return;
        }
        materialItems.remove(selected);
        recomputeTotal();
    }

    private void recomputeTotal() {
        Job preview = buildJob(serviceFeeField.getValue(), laborFeeField.getValue());
        VatSummaryView.fill(totalsGrid, jobSummaryService.previewSale(preview, materialItems));
    }

    private Job buildJob(BigDecimal serviceFee, BigDecimal laborFee) {
        Job job = new Job(customerComboBox.getValue(), JobType.SERVICE, descriptionField.getText(), null,
                datePicker.getValue(), null, JobStatus.COMPLETED, serviceFee, laborFee,
                paymentReceivedCheckBox.isSelected(), null);
        job.setServiceFeeVat(serviceFeeVatSelector.getRate(), serviceFeeVatSelector.getIncluded());
        job.setLaborFeeVat(laborFeeVatSelector.getRate(), laborFeeVatSelector.getIncluded());
        return job;
    }

    @FXML
    private void onSave() {
        if (customerComboBox.getValue() == null) {
            DialogUtil.showErrorMessage("error.job.customer.required");
            return;
        }
        try {
            Job job = buildJob(serviceFeeField.getValue(), laborFeeField.getValue());
            serviceJobService.save(editingId, job, List.copyOf(materialItems));
            this.saved = true;
            closeStage();
        } catch (NumberFormatException e) {
            DialogUtil.showErrorMessage("error.job.fee.invalid");
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
    }

    @FXML
    private void onCancel() {
        closeStage();
    }

    private void closeStage() {
        ((Stage) customerComboBox.getScene().getWindow()).close();
    }
}
