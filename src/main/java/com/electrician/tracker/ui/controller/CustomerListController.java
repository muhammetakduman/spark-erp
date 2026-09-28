package com.electrician.tracker.ui.controller;

import java.util.List;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.ui.util.AppIcon;
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
            TaskRunner taskRunner) {
        this.customerService = customerService;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
    }

    @FXML
    private void initialize() {
        table.setPlaceholder(EmptyState.of(AppIcon.CUSTOMERS, "customers.emptyState",
                "customer.action.new", this::onNew));
        TableSorting.text(nameColumn, Customer::getName);
        TableSorting.text(phoneColumn, Customer::getPhone);
        TableSorting.text(addressColumn, Customer::getAddress);
        TableSorting.text(emailColumn, Customer::getEmail);
        TableSorting.text(taxNoColumn, Customer::getTaxNo);
        refresh();
    }

    private void refresh() {
        taskRunner.run(customerService::findAll, this::showCustomers, loadingIndicator, table);
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
