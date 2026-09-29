package com.electrician.tracker.ui.controller;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.JobLabels;
import com.electrician.tracker.service.JobService;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.service.ReportService;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.BulkDeleteFlow;
import com.electrician.tracker.ui.util.BulkSelection;
import com.electrician.tracker.ui.util.DeleteConfirmation;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EmptyState;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.TaskRunner;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Servisler": every service call in a table (newest first): date, customer,
 * work done, amount and the "payment received" tick box, which can be ticked
 * right in the row. Filter Tümü / Alındı / Alınmadı; below, the total still
 * owed on services (ADMIN only).
 */
@Component
@Scope("prototype")
public class ServiceListController {

    private static final int DOUBLE_CLICK = 2;
    private static final String PAID_ROW_STYLE = "payment-received-row";
    private static final String PENDING_ROW_STYLE = "payment-pending-row";

    private final JobSummaryService jobSummaryService;
    private final JobService jobService;
    private final ReportService reportService;
    private final AccessControl accessControl;
    private final ModalStageOpener modalStageOpener;
    private final TaskRunner taskRunner;

    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private ToggleGroup filterGroup;
    @FXML
    private Button paymentButton;
    @FXML
    private Button deleteButton;
    @FXML
    private TableView<Job> serviceTable;
    @FXML
    private TableColumn<Job, LocalDate> dateColumn;
    @FXML
    private TableColumn<Job, String> customerColumn;
    @FXML
    private TableColumn<Job, String> descriptionColumn;
    @FXML
    private TableColumn<Job, BigDecimal> amountColumn;
    @FXML
    private TableColumn<Job, Boolean> paidColumn;
    @FXML
    private Label pendingTotalLabel;

    private JobBoard board;
    private FilteredList<Job> filteredServices;
    private Long pendingFocusJobId;

    public ServiceListController(JobSummaryService jobSummaryService, JobService jobService,
            ReportService reportService, AccessControl accessControl, ModalStageOpener modalStageOpener,
            TaskRunner taskRunner) {
        this.jobSummaryService = jobSummaryService;
        this.jobService = jobService;
        this.reportService = reportService;
        this.accessControl = accessControl;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
    }

    @FXML
    private void initialize() {
        board = null;
        setShown(paymentButton, accessControl.canViewFinancials());
        setShown(deleteButton, accessControl.isAdmin());
        setShown(pendingTotalLabel, accessControl.canViewFinancials());
        setUpColumns();
        setUpRows();
        if (accessControl.isAdmin()) {
            BulkSelection.forTable(serviceTable, Job::getId, this::deleteSelected);
        }
        serviceTable.setPlaceholder(EmptyState.of(AppIcon.SERVICES, "services.emptyState", "home.action.newService",
                this::onNewService));
        filterGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                filterGroup.selectToggle(oldToggle);
                return;
            }
            applyFilter();
        });
        refresh();
    }

    /** Selects this service once the page has loaded (links from other screens). */
    public void focusJob(Long jobId) {
        this.pendingFocusJobId = jobId;
        if (board != null) {
            applyPendingFocus();
        }
    }

    private void setUpColumns() {
        TableSorting.date(dateColumn, Job::getStartDate);
        TableSorting.text(customerColumn, job -> job.getCustomer().getName());
        TableSorting.text(descriptionColumn, Job::getName);
        TableSorting.money(amountColumn, job -> summaryFor(job).saleIncludingVat());
        paidColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().isPaymentReceived()));
        paidColumn.setCellFactory(column -> new PaidCell());
    }

    private void setUpRows() {
        serviceTable.setRowFactory(tv -> {
            TableRow<Job> row = new TableRow<>() {
                @Override
                protected void updateItem(Job job, boolean empty) {
                    super.updateItem(job, empty);
                    getStyleClass().removeAll(PAID_ROW_STYLE, PENDING_ROW_STYLE);
                    if (!empty && job != null && board != null) {
                        getStyleClass().add(summaryFor(job).isFullyPaid() ? PAID_ROW_STYLE : PENDING_ROW_STYLE);
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == DOUBLE_CLICK && !row.isEmpty()) {
                    openEditForm(row.getItem());
                }
            });
            ContextMenu menu = buildContextMenu();
            row.emptyProperty().addListener((obs, wasEmpty, empty) -> row.setContextMenu(empty ? null : menu));
            return row;
        });
    }

    private ContextMenu buildContextMenu() {
        ContextMenu menu = new ContextMenu(menuItem("action.edit", this::onEdit));
        if (accessControl.canViewFinancials()) {
            menu.getItems().add(menuItem("jobDetail.action.addPayment", this::onPayment));
        }
        menu.getItems().addAll(menuItem("jobDetail.action.addAttendance", this::onAttendance),
                menuItem("home.action.exportPdf", this::onPdf));
        if (accessControl.isAdmin()) {
            menu.getItems().add(menuItem("action.delete", this::onDelete));
        }
        return menu;
    }

    private void refresh() {
        taskRunner.run(jobSummaryService::loadServiceBoard, this::onLoaded, loadingIndicator, serviceTable);
    }

    private void onLoaded(JobBoard loaded) {
        this.board = loaded;
        ObservableList<Job> services = TableSorting.sorted(
                loaded.jobs().stream().filter(job -> job.getType() == JobType.SERVICE).toList(),
                TableSorting.services());
        filteredServices = new FilteredList<>(services);
        TableSorting.bindSorted(serviceTable, filteredServices);
        applyFilter();
        showPendingTotal();
        applyPendingFocus();
    }

    private void applyFilter() {
        if (filteredServices == null) {
            return;
        }
        String filter = (String) filterGroup.getSelectedToggle().getUserData();
        Predicate<Job> predicate = switch (filter) {
            case "PAID" -> job -> summaryFor(job).isFullyPaid();
            case "UNPAID" -> job -> !summaryFor(job).isFullyPaid();
            default -> job -> true;
        };
        filteredServices.setPredicate(predicate);
    }

    private void showPendingTotal() {
        BigDecimal pending = jobSummaryService.pendingServicePayment(board);
        pendingTotalLabel.setText(pending == null ? ""
                : DialogUtil.message("services.pendingTotal", Bicimlendirici.money(pending)));
    }

    private void applyPendingFocus() {
        Long jobId = pendingFocusJobId;
        pendingFocusJobId = null;
        if (jobId == null) {
            return;
        }
        filterGroup.getToggles().stream()
                .filter(toggle -> "ALL".equals(toggle.getUserData()))
                .findFirst()
                .ifPresent(filterGroup::selectToggle);
        serviceTable.getItems().stream()
                .filter(job -> job.getId().equals(jobId))
                .findFirst()
                .ifPresent(job -> {
                    serviceTable.getSelectionModel().select(job);
                    serviceTable.scrollTo(job);
                    serviceTable.requestFocus();
                });
    }

    private JobSummary summaryFor(Job job) {
        return board.summaries().get(job.getId());
    }

    @FXML
    private void onNewService() {
        ServiceFormController controller = modalStageOpener.openAndWait(ViewPaths.SERVICE_FORM,
                "home.dialog.newService", window());
        if (controller.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onEdit() {
        selected().ifPresent(this::openEditForm);
    }

    private void openEditForm(Job service) {
        ServiceFormController controller = modalStageOpener.openAndWait(ViewPaths.SERVICE_FORM,
                "home.dialog.editService", window(), c -> c.editExisting(service.getId()));
        if (controller.isSaved()) {
            refresh();
        }
    }

    @FXML
    private void onPayment() {
        selected().ifPresent(job -> {
            PaymentDialogController controller = modalStageOpener.openAndWait(ViewPaths.PAYMENT_DIALOG,
                    "jobDetail.dialog.addPayment", window(), c -> c.setJob(job));
            if (controller.isSaved()) {
                refresh();
            }
        });
    }

    @FXML
    private void onAttendance() {
        selected().ifPresent(job -> {
            AttendanceEntryController controller = modalStageOpener.openAndWait(ViewPaths.ATTENDANCE_DIALOG,
                    "jobDetail.dialog.addAttendance", window(), c -> c.setJob(job));
            if (controller.isSaved()) {
                refresh();
            }
        });
    }

    @FXML
    private void onPdf() {
        selected().ifPresent(job -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(DialogUtil.message("home.exportPdf.title"));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
            chooser.setInitialFileName(job.getCustomer().getName() + ".pdf");
            File file = chooser.showSaveDialog(window());
            if (file == null) {
                return;
            }
            taskRunner.run(() -> {
                reportService.generateJobPdf(job.getId(), file.toPath());
                return file;
            }, done -> DialogUtil.showInfo("home.exportPdf.success"), loadingIndicator, serviceTable);
        });
    }

    @FXML
    private void onDelete() {
        selected().ifPresent(job -> {
            try {
                if (!DeleteConfirmation.confirmJob(jobService.deletionImpact(job.getId()))) {
                    return;
                }
                jobService.delete(job.getId());
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
            refresh();
        });
    }

    /** "Seçilenleri Sil": the ticked services with their lines, attendance and payments. */
    private void deleteSelected(List<Job> jobs) {
        BulkDeleteFlow.of(jobs, Job::getId, JobLabels::full)
                .itemCount("bulk.count.services")
                .impact(jobService::bulkDeletionImpact)
                .delete(jobService::deleteAll)
                .afterwards(this::refresh)
                .run();
    }

    private java.util.Optional<Job> selected() {
        Job job = serviceTable.getSelectionModel().getSelectedItem();
        if (job == null) {
            DialogUtil.showErrorMessage("error.selection.required");
        }
        return java.util.Optional.ofNullable(job);
    }

    private Window window() {
        return serviceTable.getScene().getWindow();
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private static MenuItem menuItem(String messageKey, Runnable action) {
        MenuItem item = new MenuItem(DialogUtil.message(messageKey));
        item.setOnAction(e -> action.run());
        return item;
    }

    /** The "payment received in full" tick box, changed right in the row. */
    private final class PaidCell extends TableCell<Job, Boolean> {

        private final CheckBox checkBox = new CheckBox();

        private PaidCell() {
            checkBox.setOnAction(e -> {
                Job job = getTableRow().getItem();
                try {
                    jobService.setPaymentReceived(job.getId(), checkBox.isSelected());
                } catch (RuntimeException ex) {
                    DialogUtil.showError(ex);
                }
                refresh();
            });
        }

        @Override
        protected void updateItem(Boolean received, boolean empty) {
            super.updateItem(received, empty);
            if (empty || received == null) {
                setGraphic(null);
                return;
            }
            checkBox.setSelected(received);
            checkBox.setText(DialogUtil.message(received ? "home.filter.paid" : "home.filter.unpaid"));
            setGraphic(checkBox);
        }
    }
}
