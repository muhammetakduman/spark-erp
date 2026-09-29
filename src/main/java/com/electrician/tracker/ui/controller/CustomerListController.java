package com.electrician.tracker.ui.controller;

import java.util.List;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.BulkDeleteFlow;
import com.electrician.tracker.ui.util.BulkSelection;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EmptyState;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.TaskRunner;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CustomerListController {

    private static final String FORM_FXML = "/fxml/customer_form.fxml";

    private final CustomerService customerService;
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
    private ProgressIndicator loadingIndicator;

    public CustomerListController(CustomerService customerService, ModalStageOpener modalStageOpener,
            TaskRunner taskRunner, AccessControl accessControl) {
        this.customerService = customerService;
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
        if (accessControl.isAdmin()) {
            BulkSelection.forTable(table, Customer::getId, this::deleteSelected);
        }
        refresh();
    }

    private void refresh() {
        taskRunner.run(customerService::findAll, this::showCustomers, loadingIndicator, table);
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
        table.setItems(TableSorting.sorted(customers, TableSorting.customers()));
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
