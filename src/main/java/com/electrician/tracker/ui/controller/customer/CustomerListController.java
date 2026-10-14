package com.electrician.tracker.ui.controller.customer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.electrician.tracker.domain.customer.Customer;
import com.electrician.tracker.dto.messaging.MessageTarget;
import com.electrician.tracker.service.customer.CustomerService;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.service.job.JobSummaryService;
import com.electrician.tracker.service.messaging.MessagePlaceholders;
import com.electrician.tracker.service.settings.CompanyService;
import com.electrician.tracker.service.user.AccessControl;
import com.electrician.tracker.ui.controller.messaging.MessageDialogController;
import com.electrician.tracker.ui.util.dialog.BulkDeleteFlow;
import com.electrician.tracker.ui.util.dialog.DialogUtil;
import com.electrician.tracker.ui.util.dialog.ModalStageOpener;
import com.electrician.tracker.ui.util.format.Bicimlendirici;
import com.electrician.tracker.ui.util.table.BulkSelection;
import com.electrician.tracker.ui.util.table.TableSorting;
import com.electrician.tracker.ui.util.task.TaskRunner;
import com.electrician.tracker.ui.util.theme.AppIcon;
import com.electrician.tracker.ui.util.view.EmptyState;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CustomerListController {

    private static final String FORM_FXML = "/fxml/customer_form.fxml";
    private static final String MESSAGE_FXML = "/fxml/message_dialog.fxml";

    private final CustomerService customerService;
    private final JobSummaryService jobSummaryService;
    private final CompanyService companyService;
    private final ModalStageOpener modalStageOpener;
    private final TaskRunner taskRunner;
    private final AccessControl accessControl;

    @FXML
    private TableView<Customer> table;
    @FXML
    private TableColumn<Customer, String> nameColumn;
    @FXML
    private TableColumn<Customer, String> phoneColumn;
    @FXML
    private TableColumn<Customer, String> addressColumn;
    @FXML
    private TableColumn<Customer, String> emailColumn;
    @FXML
    private TableColumn<Customer, String> taxNoColumn;
    @FXML
    private TableColumn<Customer, BigDecimal> outstandingColumn;
    @FXML
    private Button messageButton;
    @FXML
    private CheckBox debtorsOnlyCheckBox;
    @FXML
    private ProgressIndicator loadingIndicator;

    /** Filled with the customer list; empty for a user who may not see receivables. */
    private Map<Long, BigDecimal> outstandingByCustomer = new HashMap<>();
    /** Everyone loaded, so the debtors filter can be turned off again without reloading. */
    private List<Customer> allCustomers = List.of();
    private BulkSelection<Customer> selection;

    public CustomerListController(CustomerService customerService, JobSummaryService jobSummaryService,
            CompanyService companyService, ModalStageOpener modalStageOpener, TaskRunner taskRunner,
            AccessControl accessControl) {
        this.customerService = customerService;
        this.jobSummaryService = jobSummaryService;
        this.companyService = companyService;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
        this.accessControl = accessControl;
    }

    @FXML
    private void initialize() {
        table.setPlaceholder(EmptyState.of(AppIcon.CUSTOMERS, "customers.emptyState",
                "customer.action.new", this::onNew));
        TableSorting.text(nameColumn, CustomerListController::nameWithState);
        TableSorting.text(phoneColumn, Customer::getPhone);
        TableSorting.text(addressColumn, Customer::getAddress);
        TableSorting.text(emailColumn, Customer::getEmail);
        TableSorting.text(taxNoColumn, Customer::getTaxNo);
        TableSorting.money(outstandingColumn, this::outstandingOf);
        // Receivables are the owner's business; a manager gets the list without them.
        boolean showsMoney = accessControl.canViewFinancials();
        outstandingColumn.setVisible(showsMoney);
        // Without balances the filter would have nothing to go on.
        debtorsOnlyCheckBox.setVisible(showsMoney);
        debtorsOnlyCheckBox.setManaged(showsMoney);
        debtorsOnlyCheckBox.selectedProperty().addListener((observable, previous, on) -> showCustomers(allCustomers));
        selection = BulkSelection.forTable(table, Customer::getId,
                accessControl.isAdmin() ? this::deleteSelected : null);
        refresh();
    }

    private void refresh() {
        taskRunner.run(this::loadPage, this::showPage, loadingIndicator, table);
    }

    /** Customers and their balances together, so the screen costs one load. */
    private CustomerPage loadPage() {
        return new CustomerPage(customerService.findAll(), jobSummaryService.outstandingByCustomer());
    }

    private void showPage(CustomerPage page) {
        outstandingByCustomer = page.outstanding();
        allCustomers = page.customers();
        showCustomers(allCustomers);
    }

    private BigDecimal outstandingOf(Customer customer) {
        return outstandingByCustomer.get(customer.getId());
    }

    /** What one load of this screen brings back. */
    private record CustomerPage(List<Customer> customers, Map<Long, BigDecimal> outstanding) {
    }

    /** "Ahmet Yılmaz (pasif)" for a customer no longer offered in pickers. */
    private static String nameWithState(Customer customer) {
        return customer.isActive() ? customer.getName()
                : DialogUtil.message("common.inactiveName", customer.getName());
    }

    /** "Seçilenleri Sil": customers with jobs are skipped and can be made inactive instead. */
    private void deleteSelected(List<Customer> customers) {
        BulkDeleteFlow.of(customers, Customer::getId, Customer::getName)
                .itemCount("bulk.count.customers")
                .delete(customerService::deleteAll)
                .deactivate(customerService::deactivateAll)
                .afterwards(this::refresh)
                .run();
    }

    private void showCustomers(List<Customer> customers) {
        List<Customer> shown = debtorsOnlyCheckBox.isSelected()
                ? customers.stream().filter(this::owesMoney).toList()
                : customers;
        table.setItems(TableSorting.sorted(shown, TableSorting.customers()));
    }

    private boolean owesMoney(Customer customer) {
        BigDecimal outstanding = outstandingOf(customer);
        return outstanding != null && outstanding.signum() > 0;
    }

    @FXML
    private void onNew() {
        CustomerFormController controller = modalStageOpener.openAndWait(
                FORM_FXML, "customer.dialog.new", table.getScene().getWindow());
        if (controller.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onEdit() {
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        CustomerFormController controller = modalStageOpener.openAndWait(
                FORM_FXML, "customer.dialog.edit", table.getScene().getWindow(),
                c -> c.editExisting(selected));
        if (controller.isSaved()) {
            refresh();
        }
    }

    /**
     * "Mesaj Gönder": opens the message window with this customer's name,
     * number and balance already in it, so nothing has to be typed again.
     */
    @FXML
    private void onMessage() {
        List<Customer> ticked = selection.selected();
        if (ticked.size() > 1) {
            messageEveryone(ticked);
            return;
        }
        Customer selected = ticked.isEmpty() ? table.getSelectionModel().getSelectedItem() : ticked.get(0);
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        modalStageOpener.<MessageDialogController>openAndWait(MESSAGE_FXML, "message.dialog.title",
                table.getScene().getWindow(), dialog -> dialog.startFor(targetFor(selected)));
    }

    /**
     * Several customers ticked: the same message goes to all of them over SMS,
     * each filled with its own name and balance. This is the point of the
     * debtors filter — tick everyone who owes, send one reminder.
     */
    private void messageEveryone(List<Customer> customers) {
        List<MessageTarget> targets = new ArrayList<>();
        for (Customer customer : customers) {
            targets.add(targetFor(customer));
        }
        modalStageOpener.<MessageDialogController>openAndWait(MESSAGE_FXML, "message.dialog.titleMany",
                table.getScene().getWindow(), dialog -> dialog.startForMany(targets));
    }

    private MessageTarget targetFor(Customer customer) {
        Map<String, String> values = new HashMap<>();
        values.put(MessagePlaceholders.CUSTOMER, customer.getName());
        values.put(MessagePlaceholders.COMPANY, companyService.get().getName());
        values.put(MessagePlaceholders.BALANCE, Bicimlendirici.money(outstandingOf(customer)));
        values.put(MessagePlaceholders.STATUS, DialogUtil.message(
                owesMoney(customer) ? "message.status.owing" : "message.status.settled"));
        return MessageTarget.of(customer.getName(), customer.getPhone(), values);
    }

    @FXML
    private void onDelete() {
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        if (!DialogUtil.confirm("customer.confirm.delete")) {
            return;
        }
        try {
            customerService.delete(selected.getId());
            refresh();
        } catch (ReferencedEntityException ex) {
            DialogUtil.showError(ex);
        }
    }
}
