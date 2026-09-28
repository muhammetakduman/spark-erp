package com.electrician.tracker.ui.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobDetail;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.QuoteLink;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.AttendanceService;
import com.electrician.tracker.service.JobService;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.MetinKarsilastirici;
import com.electrician.tracker.service.PaymentService;
import com.electrician.tracker.service.QuoteService;
import com.electrician.tracker.service.ReportService;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.DeleteConfirmation;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EmptyState;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.IconSize;
import com.electrician.tracker.ui.util.Icons;
import com.electrician.tracker.ui.util.JobAttendancePane;
import com.electrician.tracker.ui.util.JobMaterialPane;
import com.electrician.tracker.ui.util.JobPaymentPane;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.PendingJobsBadge;
import com.electrician.tracker.ui.util.StatBoxes;
import com.electrician.tracker.ui.util.TableSorting;
import com.electrician.tracker.ui.util.TaskRunner;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Şantiyeler": every site as an expandable pane (active ones first, newest
 * first), with search and "Sadece aktif". A pane shows the site's figures and
 * its materials, attendance and — for an ADMIN — payments. Loaded in a fixed
 * number of queries; a change reloads only the site it touched.
 */
@Component
@Scope("prototype")
public class SiteListController {

    private static final double HEADER_SPACING = 10;
    private static final double HEADER_ARROW_WIDTH = 48;
    private static final double CONTENT_SPACING = 14;
    private static final double ACTION_SPACING = 8;

    private final JobSummaryService jobSummaryService;
    private final JobService jobService;
    private final ReportService reportService;
    private final QuoteService quoteService;
    private final AccessControl accessControl;
    private final ModalStageOpener modalStageOpener;
    private final TaskRunner taskRunner;
    private final PendingJobsBadge pendingJobsBadge;
    private final JobAttendancePane jobAttendancePane;
    private final JobMaterialPane jobMaterialPane;
    private final JobPaymentPane jobPaymentPane;

    @FXML
    private VBox rootContent;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private TextField siteSearchField;
    @FXML
    private CheckBox activeOnlyCheckBox;
    @FXML
    private StackPane emptyHolder;
    @FXML
    private Accordion siteAccordion;

    private JobBoard board;
    private Map<Long, QuoteLink> quoteLinks = Map.of();
    private int selectedTabIndex;
    private Long pendingFocusJobId;

    public SiteListController(JobSummaryService jobSummaryService, JobService jobService, ReportService reportService,
            QuoteService quoteService, AttendanceService attendanceService, MaterialService materialService,
            PaymentService paymentService, AccessControl accessControl, ModalStageOpener modalStageOpener,
            TaskRunner taskRunner, PendingJobsBadge pendingJobsBadge) {
        this.jobSummaryService = jobSummaryService;
        this.jobService = jobService;
        this.reportService = reportService;
        this.quoteService = quoteService;
        this.accessControl = accessControl;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
        this.pendingJobsBadge = pendingJobsBadge;
        this.jobAttendancePane = new JobAttendancePane(attendanceService, accessControl, this::refreshJob);
        this.jobMaterialPane = new JobMaterialPane(materialService, accessControl, this::refreshJob,
                this::openEditMaterialDialog);
        this.jobPaymentPane = new JobPaymentPane(paymentService, this::refreshJob, this::openEditPaymentDialog);
    }

    @FXML
    private void initialize() {
        accessControl.requireAdmin();
        board = null;
        siteSearchField.textProperty().addListener((obs, o, n) -> rebuildAccordion());
        activeOnlyCheckBox.selectedProperty().addListener((obs, o, n) -> rebuildAccordion());
        refreshAll();
    }

    /** Expands this site once the page has loaded (links from other screens). */
    public void focusJob(Long jobId) {
        this.pendingFocusJobId = jobId;
        if (board != null) {
            applyPendingFocus();
        }
    }

    private void refreshAll() {
        taskRunner.run(() -> new PageData(jobSummaryService.loadBoard(), quoteService.findJobLinks()),
                this::onLoaded, loadingIndicator, rootContent);
    }

    private void onLoaded(PageData data) {
        this.board = data.board();
        this.quoteLinks = data.quoteLinks();
        rebuildAccordion();
        applyPendingFocus();
    }

    /** Reloads one site after a change and rebuilds only its pane; open panes stay open. */
    private void refreshJob(Long jobId) {
        if (board == null) {
            refreshAll();
            return;
        }
        taskRunner.run(() -> jobSummaryService.loadJobDetail(jobId), this::onJobReloaded, loadingIndicator,
                siteAccordion);
    }

    private void onJobReloaded(JobDetail detail) {
        board = board.withJob(detail);
        siteAccordion.getPanes().stream()
                .filter(pane -> detail.job().getId().equals(pane.getUserData()))
                .findFirst()
                .ifPresent(pane -> fillPane(pane, detail.job()));
    }

    private void applyPendingFocus() {
        Long jobId = pendingFocusJobId;
        pendingFocusJobId = null;
        if (jobId == null) {
            return;
        }
        siteSearchField.clear();
        activeOnlyCheckBox.setSelected(false);
        siteAccordion.getPanes().stream()
                .filter(pane -> jobId.equals(pane.getUserData()))
                .findFirst()
                .ifPresent(siteAccordion::setExpandedPane);
    }

    private void rebuildAccordion() {
        if (board == null) {
            return;
        }
        String search = MetinKarsilastirici.normalize(siteSearchField.getText());
        boolean activeOnly = activeOnlyCheckBox.isSelected();
        List<TitledPane> panes = board.jobs().stream()
                .filter(job -> job.getType() == JobType.SITE)
                .filter(job -> !activeOnly || job.getStatus() == JobStatus.ACTIVE)
                .filter(job -> search.isEmpty()
                        || MetinKarsilastirici.normalize(job.getCustomer().getName()).contains(search)
                        || MetinKarsilastirici.normalize(job.getName()).contains(search))
                .sorted(TableSorting.sites())
                .map(this::buildPane)
                .toList();
        Object expandedJobId = siteAccordion.getExpandedPane() == null ? null
                : siteAccordion.getExpandedPane().getUserData();
        siteAccordion.getPanes().setAll(panes);
        showEmptyState(panes.isEmpty());
        panes.stream()
                .filter(pane -> pane.getUserData() != null && pane.getUserData().equals(expandedJobId))
                .findFirst()
                .ifPresent(siteAccordion::setExpandedPane);
    }

    /** "Henüz şantiye eklenmemiş" with the new-site button, or "no match" while searching/filtering. */
    private void showEmptyState(boolean empty) {
        emptyHolder.setVisible(empty);
        emptyHolder.setManaged(empty);
        if (!empty) {
            return;
        }
        boolean noSites = board.jobs().stream().noneMatch(job -> job.getType() == JobType.SITE);
        emptyHolder.getChildren().setAll(noSites
                ? EmptyState.of(AppIcon.SITES, "sites.emptyState", "home.action.newSite", this::onNewSite)
                : EmptyState.of(AppIcon.SEARCH, "sites.empty"));
    }

    private TitledPane buildPane(Job job) {
        TitledPane pane = new TitledPane();
        pane.setUserData(job.getId());
        pane.setText("");
        fillPane(pane, job);
        return pane;
    }

    private void fillPane(TitledPane pane, Job job) {
        JobSummary summary = board.summaries().get(job.getId());
        pane.setGraphic(buildHeader(pane, job, summary));
        pane.setContent(buildContent(job, summary));
        pane.setContextMenu(buildContextMenu(job));
    }

    private ContextMenu buildContextMenu(Job job) {
        ContextMenu menu = new ContextMenu(menuItem("home.action.editSite", () -> openEditSiteForm(job)));
        if (accessControl.isAdmin()) {
            menu.getItems().add(menuItem("home.action.deleteSite", () -> deleteSite(job)));
        }
        return menu;
    }

    /** "Customer – Site  [Aktif]  …  Kalan 7.000,00 ₺"; the balance only for users who may see it. */
    private HBox buildHeader(TitledPane pane, Job job, JobSummary summary) {
        Label titleLabel = new Label(job.getCustomer().getName() + " – " + job.getName());
        titleLabel.getStyleClass().add("site-title");
        Label statusLabel = new Label(EnumLabels.label(job.getStatus()));
        statusLabel.getStyleClass().addAll("status-pill",
                job.getStatus() == JobStatus.ACTIVE ? "status-active" : "status-completed");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(HEADER_SPACING, titleLabel, statusLabel, spacer);
        if (summary.hasFinancials()) {
            Label balanceLabel = new Label(summary.isFullyPaid() ? DialogUtil.message("home.header.paid")
                    : DialogUtil.message("home.header.remaining", Bicimlendirici.money(summary.remaining())));
            balanceLabel.getStyleClass().add(summary.isFullyPaid() ? "header-paid" : "header-due");
            header.getChildren().add(balanceLabel);
        }
        header.setAlignment(Pos.CENTER_LEFT);
        header.prefWidthProperty().bind(pane.widthProperty().subtract(HEADER_ARROW_WIDTH));
        return header;
    }

    private VBox buildContent(Job job, JobSummary summary) {
        VBox content = new VBox(CONTENT_SPACING);
        content.getStyleClass().add("job-pane-content");
        buildContactLine(job).ifPresent(content.getChildren()::add);
        quoteLinkLine(job.getId()).ifPresent(content.getChildren()::add);
        content.getChildren().addAll(buildActions(job), StatBoxes.jobRow(summary));
        foreignCurrencyNote(summary).ifPresent(content.getChildren()::add);
        content.getChildren().add(buildTabs(job, summary));
        return content;
    }

    /** "3 kalem dövizle alınmış, TL karşılıkları alış tarihindeki kurla hesaplanmıştır." */
    private static Optional<Node> foreignCurrencyNote(JobSummary summary) {
        if (summary.foreignCurrencyLineCount() == 0) {
            return Optional.empty();
        }
        Label note = new Label(DialogUtil.message("site.foreignCurrencyNote", summary.foreignCurrencyLineCount()),
                Icons.of(AppIcon.CURRENCY, IconSize.BUTTON));
        note.getStyleClass().add("info-strip");
        note.setWrapText(true);
        note.setMaxWidth(Double.MAX_VALUE);
        return Optional.of(note);
    }

    /** "Bu iş 2026/0001 numaralı tekliften oluşturuldu" with a link to the quote. */
    private Optional<Node> quoteLinkLine(Long jobId) {
        QuoteLink link = quoteLinks.get(jobId);
        if (link == null) {
            return Optional.empty();
        }
        Hyperlink hyperlink = new Hyperlink(DialogUtil.message("job.fromQuote", link.quoteNo()));
        hyperlink.getStyleClass().add("quote-link");
        hyperlink.setOnAction(e -> modalStageOpener.<QuoteFormController>openAndWait(ViewPaths.QUOTE_FORM,
                "quote.dialog.edit", window(), form -> form.editExisting(link.quoteId())));
        return Optional.of(hyperlink);
    }

    /**
     * Frequent actions on the left (material, payment, attendance, plan a
     * day); the PDF on the right; rarely used ones (edit, delete) under
     * "Daha fazla".
     */
    private HBox buildActions(Job job) {
        List<Node> actions = new ArrayList<>(List.of(
                Icons.button(AppIcon.ADD, "jobDetail.action.addMaterial", () -> openMaterialDialog(job))));
        if (accessControl.canViewFinancials()) {
            actions.add(Icons.button(AppIcon.PAYMENT, "jobDetail.action.addPayment", () -> openPaymentDialog(job)));
        }
        actions.add(Icons.button(AppIcon.ATTENDANCE, "jobDetail.action.addAttendance",
                () -> openAttendanceDialog(job)));
        actions.add(Icons.button(AppIcon.PLAN_DAY, "dailyJob.action.planForSite", () -> planDayForSite(job)));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.add(spacer);
        actions.add(Icons.button(AppIcon.PDF, "home.action.exportPdf", () -> exportJobPdf(job)));
        actions.add(moreMenu(job));
        HBox box = new HBox(ACTION_SPACING, actions.toArray(Node[]::new));
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("pane-actions");
        return box;
    }

    private MenuButton moreMenu(Job job) {
        MenuButton more = new MenuButton(DialogUtil.message("action.more"));
        more.getStyleClass().add("more-button");
        more.getItems().add(Icons.decorate(menuItem("home.action.editSite", () -> openEditSiteForm(job)),
                AppIcon.EDIT));
        if (accessControl.isAdmin()) {
            more.getItems().add(Icons.decorate(menuItem("home.action.deleteSite", () -> deleteSite(job)),
                    AppIcon.DELETE));
        }
        return more;
    }

    /** "Bu şantiyeye gün planla": the daily job form, already linked to this site. */
    private void planDayForSite(Job job) {
        DailyJobFormController form = modalStageOpener.openAndWait(ViewPaths.DAILY_JOB_FORM,
                "dailyJob.dialog.new", window(), controller -> controller.startForJob(job.getId()));
        if (form.isSaved()) {
            pendingJobsBadge.refresh();
            DialogUtil.showInfo("dailyJob.plannedForSite");
        }
    }

    /** "Tel: 0532 … · Adres: …"; nothing when the customer has neither. */
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

    private TabPane buildTabs(Job job, JobSummary summary) {
        List<MaterialItem> materials = board.materialsFor(job.getId());
        TabPane tabs = new TabPane(
                new Tab(DialogUtil.message("jobDetail.tab.materials.title", materials.size()),
                        jobMaterialPane.build(job.getId(), materials)),
                new Tab(JobAttendancePane.tabTitle(summary),
                        jobAttendancePane.build(job, summary, board.attendancesFor(job.getId()))));
        if (accessControl.canViewFinancials()) {
            List<Payment> payments = board.paymentsFor(job.getId());
            tabs.getTabs().add(new Tab(DialogUtil.message("jobDetail.tab.payments.title", payments.size()),
                    jobPaymentPane.build(job.getId(), payments)));
        }
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().select(Math.min(selectedTabIndex, tabs.getTabs().size() - 1));
        tabs.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> selectedTabIndex = n.intValue());
        return tabs;
    }

    /** Asks with the exact counts of what goes with the site; deletes everything in one transaction. */
    private void deleteSite(Job job) {
        try {
            if (!DeleteConfirmation.confirmJob(jobService.deletionImpact(job.getId()))) {
                return;
            }
            jobService.delete(job.getId());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        refreshAll();
    }

    @FXML
    private void onNewSite() {
        JobFormController controller = modalStageOpener.openAndWait(ViewPaths.JOB_FORM, "job.dialog.new", window());
        if (controller.isSaved()) {
            refreshAll();
        }
    }

    private void openEditSiteForm(Job job) {
        JobFormController controller = modalStageOpener.openAndWait(ViewPaths.JOB_FORM, "job.dialog.edit", window(),
                c -> c.editExisting(job));
        if (controller.isSaved()) {
            refreshAll();
        }
    }

    private void openMaterialDialog(Job job) {
        MaterialDialogController controller = modalStageOpener.openAndWait(ViewPaths.MATERIAL_DIALOG,
                "jobDetail.dialog.addMaterial", window(), c -> c.setJob(job));
        if (controller.isAnyAdded()) {
            refreshJob(job.getId());
        }
    }

    private void openEditMaterialDialog(MaterialItem item) {
        MaterialDialogController controller = modalStageOpener.openAndWait(ViewPaths.MATERIAL_DIALOG,
                "jobDetail.dialog.editMaterial", window(), c -> c.editExisting(item));
        if (controller.isAnyAdded()) {
            refreshJob(item.getJob().getId());
        }
    }

    private void openAttendanceDialog(Job job) {
        AttendanceEntryController controller = modalStageOpener.openAndWait(ViewPaths.ATTENDANCE_DIALOG,
                "jobDetail.dialog.addAttendance", window(), c -> c.setJob(job));
        if (controller.isSaved()) {
            refreshJob(job.getId());
        }
    }

    private void openPaymentDialog(Job job) {
        PaymentDialogController controller = modalStageOpener.openAndWait(ViewPaths.PAYMENT_DIALOG,
                "jobDetail.dialog.addPayment", window(), c -> c.setJob(job));
        if (controller.isSaved()) {
            refreshJob(job.getId());
        }
    }

    private void openEditPaymentDialog(Payment payment) {
        PaymentDialogController controller = modalStageOpener.openAndWait(ViewPaths.PAYMENT_DIALOG,
                "jobDetail.dialog.editPayment", window(), c -> c.editExisting(payment));
        if (controller.isSaved()) {
            refreshJob(payment.getJob().getId());
        }
    }

    private void exportJobPdf(Job job) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message("home.exportPdf.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName(job.getName() + ".pdf");
        File selected = chooser.showSaveDialog(window());
        if (selected == null) {
            return;
        }
        taskRunner.run(() -> {
            reportService.generateJobPdf(job.getId(), selected.toPath());
            return selected;
        }, file -> DialogUtil.showInfo("home.exportPdf.success"), loadingIndicator, rootContent);
    }

    private javafx.stage.Window window() {
        return rootContent.getScene().getWindow();
    }

    private static MenuItem menuItem(String messageKey, Runnable action) {
        MenuItem item = new MenuItem(DialogUtil.message(messageKey));
        item.setOnAction(e -> action.run());
        return item;
    }

    private record PageData(JobBoard board, Map<Long, QuoteLink> quoteLinks) {
    }
}
