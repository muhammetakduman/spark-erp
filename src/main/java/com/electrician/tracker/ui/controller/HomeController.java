package com.electrician.tracker.ui.controller;

import java.io.File;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobDetail;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.service.AttendanceService;
import com.electrician.tracker.service.JobService;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.MetinKarsilastirici;
import com.electrician.tracker.service.PaymentService;
import com.electrician.tracker.service.ReportService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.JobAttendancePane;
import com.electrician.tracker.ui.util.JobMaterialPane;
import com.electrician.tracker.ui.util.JobPaymentPane;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.StatBoxes;
import com.electrician.tracker.ui.util.TaskRunner;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

@Component
public class HomeController {

    private static final String JOB_FORM_FXML = "/fxml/job_form.fxml";
    private static final String SERVICE_FORM_FXML = "/fxml/service_form.fxml";
    private static final String MATERIAL_DIALOG_FXML = "/fxml/material_dialog.fxml";
    private static final String ATTENDANCE_DIALOG_FXML = "/fxml/attendance_entry.fxml";
    private static final String PAYMENT_DIALOG_FXML = "/fxml/payment_dialog.fxml";
    private static final String SERVICE_FILTER_ALL = "all";
    private static final int DOUBLE_CLICK = 2;
    private static final double HEADER_SPACING = 10;
    private static final double HEADER_ARROW_WIDTH = 48;
    private static final double CONTENT_SPACING = 14;
    private static final double ACTION_SPACING = 8;

    private final JobSummaryService jobSummaryService;
    private final JobService jobService;
    private final ReportService reportService;
    private final ModalStageOpener modalStageOpener;
    private final TaskRunner taskRunner;
    private final JobAttendancePane jobAttendancePane;
    private final JobMaterialPane jobMaterialPane;
    private final JobPaymentPane jobPaymentPane;

    @FXML
    private VBox rootContent;
    @FXML
    private HBox summaryCardsBox;
    @FXML
    private VBox completedBalanceBox;
    @FXML
    private VBox completedBalanceList;
    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private TextField siteSearchField;
    @FXML
    private CheckBox activeOnlyCheckBox;
    @FXML
    private Accordion siteAccordion;

    @FXML
    private ToggleGroup serviceFilterGroup;
    @FXML
    private TableView<Job> serviceTable;
    @FXML
    private TableColumn<Job, String> serviceDateColumn;
    @FXML
    private TableColumn<Job, String> serviceCustomerColumn;
    @FXML
    private TableColumn<Job, String> serviceDescriptionColumn;
    @FXML
    private TableColumn<Job, String> serviceAmountColumn;
    @FXML
    private TableColumn<Job, String> servicePhoneColumn;
    @FXML
    private TableColumn<Job, String> serviceBalanceColumn;
    @FXML
    private TableColumn<Job, Void> servicePaidColumn;

    private JobBoard board;
    private int selectedSiteTabIndex;
    private Long pendingFocusJobId;

    public HomeController(JobSummaryService jobSummaryService, JobService jobService, ReportService reportService,
            AttendanceService attendanceService, MaterialService materialService, PaymentService paymentService,
            ModalStageOpener modalStageOpener, TaskRunner taskRunner) {
        this.jobSummaryService = jobSummaryService;
        this.jobService = jobService;
        this.reportService = reportService;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
        this.jobAttendancePane = new JobAttendancePane(attendanceService, this::refreshJob);
        this.jobMaterialPane = new JobMaterialPane(materialService, this::refreshJob, this::openEditMaterialDialog);
        this.jobPaymentPane = new JobPaymentPane(paymentService, this::refreshJob, this::openEditPaymentDialog);
    }

    @FXML
    private void initialize() {
        serviceDateColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getStartDate() == null ? "" : data.getValue().getStartDate().format(Bicimlendirici.DATE)));
        serviceCustomerColumn.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCustomer().getName()));
        serviceDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        setUpServicePaidColumn();
        serviceTable.setRowFactory(tv -> {
            TableRow<Job> row = new TableRow<>() {
                @Override
                protected void updateItem(Job job, boolean empty) {
                    super.updateItem(job, empty);
                    getStyleClass().removeAll("payment-received-row", "payment-pending-row");
                    if (!empty && job != null && board != null) {
                        getStyleClass().add(summaryFor(job).isFullyPaid()
                                ? "payment-received-row" : "payment-pending-row");
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == DOUBLE_CLICK && !row.isEmpty()) {
                    openEditServiceForm(row.getItem());
                }
            });
            ContextMenu menu = buildServiceContextMenu();
            row.emptyProperty().addListener((obs, wasEmpty, empty) -> row.setContextMenu(empty ? null : menu));
            return row;
        });

        siteSearchField.textProperty().addListener((obs, o, n) -> rebuildSiteAccordion());
        activeOnlyCheckBox.selectedProperty().addListener((obs, o, n) -> rebuildSiteAccordion());
        serviceFilterGroup.selectedToggleProperty().addListener((obs, o, n) -> rebuildServiceTable());

        refreshAll();
    }

    private void setUpServicePaidColumn() {
        serviceAmountColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                Bicimlendirici.money(board == null ? null : summaryFor(data.getValue()).saleIncludingVat())));
        servicePhoneColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                Objects.requireNonNullElse(data.getValue().getCustomer().getPhone(), "")));
        serviceBalanceColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                board == null ? "" : balanceText(summaryFor(data.getValue()))));
        servicePaidColumn.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    Job job = getTableView().getItems().get(getIndex());
                    jobService.setPaymentReceived(job.getId(), checkBox.isSelected());
                    refreshJob(job.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Job job = getTableView().getItems().get(getIndex());
                checkBox.setSelected(job.isPaymentReceived());
                setGraphic(checkBox);
            }
        });
    }

    private JobSummary summaryFor(Job job) {
        return board.summaries().get(job.getId());
    }

    private void refreshAll() {
        taskRunner.run(jobSummaryService::loadBoard, this::onBoardLoaded, loadingIndicator, rootContent);
    }

    /**
     * Reloads one job after a material/attendance/payment change and updates
     * only its panel (or service row) plus the summary cards; open panels
     * stay open.
     */
    private void refreshJob(Long jobId) {
        if (board == null) {
            refreshAll();
            return;
        }
        taskRunner.run(() -> jobSummaryService.loadJobDetail(jobId), this::onJobReloaded, loadingIndicator,
                summaryCardsBox);
    }

    private void onJobReloaded(JobDetail detail) {
        board = board.withJob(detail);
        rebuildSummaryCards();
        Job job = detail.job();
        if (job.getType() == JobType.SITE) {
            siteAccordion.getPanes().stream()
                    .filter(pane -> job.getId().equals(pane.getUserData()))
                    .findFirst()
                    .ifPresent(pane -> fillSitePane(pane, job));
        } else {
            rebuildServiceTable();
        }
    }

    private void onBoardLoaded(JobBoard loadedBoard) {
        this.board = loadedBoard;
        rebuildSummaryCards();
        rebuildSiteAccordion();
        rebuildServiceTable();
        applyPendingFocus();
    }

    /**
     * Opens a job once the board has loaded: a site's pane is expanded, a
     * service is selected in the services table. Used when another screen
     * (e.g. product usage history) links to a job.
     */
    public void focusJob(Long jobId) {
        this.pendingFocusJobId = jobId;
    }

    private void applyPendingFocus() {
        Long jobId = pendingFocusJobId;
        pendingFocusJobId = null;
        if (jobId == null) {
            return;
        }
        board.jobs().stream()
                .filter(job -> job.getId().equals(jobId))
                .findFirst()
                .ifPresent(job -> {
                    if (job.getType() == JobType.SITE) {
                        showSitePane(jobId);
                    } else {
                        showServiceRow(job);
                    }
                });
    }

    private void showServiceRow(Job job) {
        serviceFilterGroup.getToggles().stream()
                .filter(toggle -> SERVICE_FILTER_ALL.equals(toggle.getUserData()))
                .findFirst()
                .ifPresent(serviceFilterGroup::selectToggle);
        serviceTable.getItems().stream()
                .filter(row -> row.getId().equals(job.getId()))
                .findFirst()
                .ifPresent(row -> {
                    serviceTable.getSelectionModel().select(row);
                    serviceTable.scrollTo(row);
                    serviceTable.requestFocus();
                });
    }

    // ---- Summary cards ----------------------------------------------------

    private void rebuildSummaryCards() {
        DashboardFigures figures = jobSummaryService.dashboard(board, YearMonth.now());
        String profitCaption = figures.monthlyProfitEstimated()
                ? DialogUtil.message("home.stat.profitEstimated") : null;

        summaryCardsBox.getChildren().setAll(
                summaryCard("home.card.activeSites", String.valueOf(figures.activeSiteCount()), null),
                summaryCard("home.card.monthlyProfit", Bicimlendirici.money(figures.monthlyProfit()), profitCaption),
                summaryCard("home.card.pendingSiteReceivable", Bicimlendirici.money(figures.pendingSiteReceivable()),
                        null),
                summaryCard("home.card.pendingServicePayment",
                        Bicimlendirici.money(figures.pendingServicePayment()), null));
        rebuildCompletedWithBalance(figures.completedSitesWithBalance());
    }

    private VBox summaryCard(String titleKey, String value, String caption) {
        VBox card = StatBoxes.box(DialogUtil.message(titleKey), value, caption);
        card.getStyleClass().add("summary-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /** "Bitmiş ama tahsilatı eksik" list; each entry opens its site pane. */
    private void rebuildCompletedWithBalance(List<Job> sites) {
        completedBalanceBox.setVisible(!sites.isEmpty());
        completedBalanceBox.setManaged(!sites.isEmpty());
        completedBalanceList.getChildren().setAll(sites.stream().map(this::buildCompletedBalanceLink).toList());
    }

    private Hyperlink buildCompletedBalanceLink(Job job) {
        Hyperlink link = new Hyperlink(DialogUtil.message("home.completedBalance.line",
                job.getCustomer().getName(), job.getName(), Bicimlendirici.money(summaryFor(job).remaining())));
        link.setOnAction(e -> showSitePane(job.getId()));
        return link;
    }

    private void showSitePane(Long jobId) {
        siteSearchField.clear();
        activeOnlyCheckBox.setSelected(false);
        siteAccordion.getPanes().stream()
                .filter(pane -> jobId.equals(pane.getUserData()))
                .findFirst()
                .ifPresent(siteAccordion::setExpandedPane);
    }

    // ---- Site accordion -----------------------------------------------------

    @FXML
    private void onNewSite() {
        JobFormController controller = modalStageOpener.openAndWait(JOB_FORM_FXML, "job.dialog.new",
                rootContent.getScene().getWindow());
        if (controller.isSaved()) {
            refreshAll();
        }
    }

    private void rebuildSiteAccordion() {
        if (board == null) {
            return;
        }
        String search = MetinKarsilastirici.normalize(siteSearchField.getText());
        boolean activeOnly = activeOnlyCheckBox.isSelected();

        List<TitledPane> panes = board.jobs().stream()
                .filter(j -> j.getType() == JobType.SITE)
                .filter(j -> !activeOnly || j.getStatus() == JobStatus.ACTIVE)
                .filter(j -> search.isEmpty()
                        || MetinKarsilastirici.normalize(j.getCustomer().getName()).contains(search)
                        || MetinKarsilastirici.normalize(j.getName()).contains(search))
                .map(this::buildSitePane)
                .toList();
        Object expandedJobId = siteAccordion.getExpandedPane() == null ? null
                : siteAccordion.getExpandedPane().getUserData();
        siteAccordion.getPanes().setAll(panes);
        // Keep the pane the user was working in open across refreshes.
        panes.stream()
                .filter(pane -> pane.getUserData() != null && pane.getUserData().equals(expandedJobId))
                .findFirst()
                .ifPresent(siteAccordion::setExpandedPane);
    }

    private TitledPane buildSitePane(Job job) {
        TitledPane pane = new TitledPane();
        pane.setUserData(job.getId());
        pane.setText("");
        fillSitePane(pane, job);
        return pane;
    }

    private void fillSitePane(TitledPane pane, Job job) {
        JobSummary summary = summaryFor(job);
        pane.setGraphic(buildSiteHeader(pane, job, summary));
        pane.setContent(buildSitePaneContent(job, summary));
        pane.setContextMenu(buildSiteContextMenu(job));
    }

    /** Right click on a site: edit or delete it without opening it first. */
    private ContextMenu buildSiteContextMenu(Job job) {
        return new ContextMenu(
                menuItem("home.action.editSite", () -> openEditSiteForm(job)),
                menuItem("home.action.deleteSite", () -> deleteSite(job)));
    }

    /** "Customer - Site  [Aktif]  ...  Kalan 7.000,00 TL" across the whole pane header. */
    private HBox buildSiteHeader(TitledPane pane, Job job, JobSummary summary) {
        Label titleLabel = new Label(job.getCustomer().getName() + " – " + job.getName());
        titleLabel.getStyleClass().add("site-title");
        Label statusLabel = new Label(EnumLabels.label(job.getStatus()));
        statusLabel.getStyleClass().addAll("status-pill",
                job.getStatus() == JobStatus.ACTIVE ? "status-active" : "status-completed");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label balanceLabel = new Label(summary.isFullyPaid() ? DialogUtil.message("home.header.paid")
                : DialogUtil.message("home.header.remaining", Bicimlendirici.money(summary.remaining())));
        balanceLabel.getStyleClass().add(summary.isFullyPaid() ? "header-paid" : "header-due");

        HBox header = new HBox(HEADER_SPACING, titleLabel, statusLabel, spacer, balanceLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.prefWidthProperty().bind(pane.widthProperty().subtract(HEADER_ARROW_WIDTH));
        return header;
    }

    private VBox buildSitePaneContent(Job job, JobSummary summary) {
        VBox content = new VBox(CONTENT_SPACING);
        content.getStyleClass().add("job-pane-content");
        buildContactLine(job).ifPresent(content.getChildren()::add);
        content.getChildren().addAll(buildSiteActions(job), StatBoxes.jobRow(summary), buildDetailTabs(job, summary));
        return content;
    }

    /** Frequent actions on the left, rarely used ones (edit, PDF, delete) on the right. */
    private HBox buildSiteActions(Job job) {
        Button addMaterialButton = new Button(DialogUtil.message("jobDetail.action.addMaterial"));
        addMaterialButton.getStyleClass().add("primary-button");
        addMaterialButton.setOnAction(e -> openMaterialDialog(job));
        Button addPaymentButton = new Button(DialogUtil.message("jobDetail.action.addPayment"));
        addPaymentButton.setOnAction(e -> openPaymentDialog(job));
        Button addAttendanceButton = new Button(DialogUtil.message("jobDetail.action.addAttendance"));
        addAttendanceButton.setOnAction(e -> openAttendanceDialog(job));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button editButton = new Button(DialogUtil.message("home.action.editSite"));
        editButton.setOnAction(e -> openEditSiteForm(job));
        Button pdfButton = new Button(DialogUtil.message("home.action.exportPdf"));
        pdfButton.setOnAction(e -> exportJobPdf(job));
        Button deleteButton = new Button(DialogUtil.message("home.action.deleteSite"));
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(e -> deleteSite(job));

        HBox actions = new HBox(ACTION_SPACING, addMaterialButton, addPaymentButton, addAttendanceButton, spacer,
                editButton, pdfButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    /** "Tel: 0532 ... · Adres: ..."; nothing when the customer has neither. */
    private Optional<Label> buildContactLine(Job job) {
        Customer customer = job.getCustomer();
        List<String> parts = new ArrayList<>();
        if (customer.getPhone() != null && !customer.getPhone().isBlank()) {
            parts.add(DialogUtil.message("home.pane.phone", customer.getPhone()));
        }
        if (customer.getAddress() != null && !customer.getAddress().isBlank()) {
            parts.add(DialogUtil.message("home.pane.customerAddress", customer.getAddress()));
        }
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        Label label = new Label(String.join(" · ", parts));
        label.getStyleClass().add("contact-line");
        return Optional.of(label);
    }

    /** Remaining amount, or "Ödendi". */
    private static String balanceText(JobSummary summary) {
        return summary.isFullyPaid() ? DialogUtil.message("home.service.paid")
                : Bicimlendirici.money(summary.remaining());
    }

    private TabPane buildDetailTabs(Job job, JobSummary summary) {
        List<MaterialItem> materials = board.materialsFor(job.getId());
        List<Payment> payments = board.paymentsFor(job.getId());
        Tab materialTab = new Tab(DialogUtil.message("jobDetail.tab.materials.title", materials.size()),
                jobMaterialPane.build(job.getId(), materials));
        Tab teamTab = new Tab(JobAttendancePane.tabTitle(summary),
                jobAttendancePane.build(job, summary, board.attendancesFor(job.getId())));
        Tab paymentTab = new Tab(DialogUtil.message("jobDetail.tab.payments.title", payments.size()),
                jobPaymentPane.build(job.getId(), payments));
        TabPane tabs = new TabPane(materialTab, teamTab, paymentTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().select(selectedSiteTabIndex);
        tabs.getSelectionModel().selectedIndexProperty().addListener(
                (obs, o, n) -> selectedSiteTabIndex = n.intValue());
        return tabs;
    }

    /** A wrongly entered site can be removed at once, together with its materials, attendance and payments. */
    private void deleteSite(Job job) {
        if (!DialogUtil.confirm("job.confirm.deleteSite", job.getName())) {
            return;
        }
        try {
            jobService.delete(job.getId());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        refreshAll();
    }

    private void openMaterialDialog(Job job) {
        MaterialDialogController controller = modalStageOpener.openAndWait(
                MATERIAL_DIALOG_FXML, "jobDetail.dialog.addMaterial", rootContent.getScene().getWindow(),
                c -> c.setJob(job));
        if (controller.isAnyAdded()) {
            refreshJob(job.getId());
        }
    }

    private void openEditPaymentDialog(Payment payment) {
        PaymentDialogController controller = modalStageOpener.openAndWait(
                PAYMENT_DIALOG_FXML, "jobDetail.dialog.editPayment", rootContent.getScene().getWindow(),
                c -> c.editExisting(payment));
        if (controller.isSaved()) {
            refreshJob(payment.getJob().getId());
        }
    }

    @FXML
    private void onServicePayment() {
        Job selected = serviceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        openPaymentDialog(selected);
    }

    private void openEditMaterialDialog(MaterialItem item) {
        MaterialDialogController controller = modalStageOpener.openAndWait(
                MATERIAL_DIALOG_FXML, "jobDetail.dialog.editMaterial", rootContent.getScene().getWindow(),
                c -> c.editExisting(item));
        if (controller.isAnyAdded()) {
            refreshJob(item.getJob().getId());
        }
    }

    private void openAttendanceDialog(Job job) {
        AttendanceEntryController controller = modalStageOpener.openAndWait(
                ATTENDANCE_DIALOG_FXML, "jobDetail.dialog.addAttendance", rootContent.getScene().getWindow(),
                c -> c.setJob(job));
        if (controller.isSaved()) {
            refreshJob(job.getId());
        }
    }

    private void openPaymentDialog(Job job) {
        PaymentDialogController controller = modalStageOpener.openAndWait(
                PAYMENT_DIALOG_FXML, "jobDetail.dialog.addPayment", rootContent.getScene().getWindow(),
                c -> c.setJob(job));
        if (controller.isSaved()) {
            refreshJob(job.getId());
        }
    }

    private void openEditSiteForm(Job job) {
        JobFormController controller = modalStageOpener.openAndWait(
                JOB_FORM_FXML, "job.dialog.edit", rootContent.getScene().getWindow(),
                c -> c.editExisting(job));
        if (controller.isSaved()) {
            refreshAll();
        }
    }

    private void exportJobPdf(Job job) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message("home.exportPdf.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName(job.getName() + ".pdf");
        File selected = chooser.showSaveDialog(rootContent.getScene().getWindow());
        if (selected == null) {
            return;
        }
        reportService.generateJobPdf(job.getId(), selected.toPath());
        DialogUtil.showInfo("home.exportPdf.success");
    }

    // ---- Services table -----------------------------------------------------

    @FXML
    private void onNewService() {
        ServiceFormController controller = modalStageOpener.openAndWait(
                SERVICE_FORM_FXML, "home.dialog.newService", rootContent.getScene().getWindow());
        if (controller.isSaved()) {
            refreshAll();
        }
    }

    @FXML
    private void onEditService() {
        Job selected = serviceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        openEditServiceForm(selected);
    }

    private void openEditServiceForm(Job service) {
        ServiceFormController controller = modalStageOpener.openAndWait(
                SERVICE_FORM_FXML, "home.dialog.editService", rootContent.getScene().getWindow(),
                c -> c.editExisting(service.getId()));
        if (controller.isSaved()) {
            refreshJob(service.getId());
        }
    }

    @FXML
    private void onServicePdf() {
        Job selected = serviceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        exportJobPdf(selected);
    }

    @FXML
    private void onServiceAttendance() {
        Job selected = serviceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        openAttendanceDialog(selected);
    }

    @FXML
    private void onDeleteService() {
        Job selected = serviceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showErrorMessage("error.selection.required");
            return;
        }
        if (!DialogUtil.confirm("job.confirm.delete")) {
            return;
        }
        jobService.delete(selected.getId());
        refreshAll();
    }

    private ContextMenu buildServiceContextMenu() {
        return new ContextMenu(
                menuItem("action.edit", this::onEditService),
                menuItem("jobDetail.action.addPayment", this::onServicePayment),
                menuItem("jobDetail.action.addAttendance", this::onServiceAttendance),
                menuItem("home.action.exportPdf", this::onServicePdf),
                menuItem("action.delete", this::onDeleteService));
    }

    private static MenuItem menuItem(String messageKey, Runnable action) {
        MenuItem item = new MenuItem(DialogUtil.message(messageKey));
        item.setOnAction(e -> action.run());
        return item;
    }

    private void rebuildServiceTable() {
        if (board == null) {
            return;
        }
        ToggleButton selected = (ToggleButton) serviceFilterGroup.getSelectedToggle();
        String filter = selected == null ? "all" : (String) selected.getUserData();

        List<Job> services = board.jobs().stream()
                .filter(j -> j.getType() == JobType.SERVICE)
                .filter(j -> filter == null || "all".equals(filter)
                        || ("unpaid".equals(filter) && !summaryFor(j).isFullyPaid())
                        || ("paid".equals(filter) && summaryFor(j).isFullyPaid()))
                .toList();
        serviceTable.setItems(FXCollections.observableArrayList(services));
    }
}
