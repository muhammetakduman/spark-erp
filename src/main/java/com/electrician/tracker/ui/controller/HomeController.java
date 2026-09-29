package com.electrician.tracker.ui.controller;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.RecentActivity;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.ContentNavigator;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.JobNavigator;
import com.electrician.tracker.ui.util.PendingJobsBadge;
import com.electrician.tracker.ui.util.StatBoxes;
import com.electrician.tracker.ui.util.TaskRunner;
import com.electrician.tracker.ui.util.ViewPaths;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "Ana Ekran": the summary dashboard only. Cards (active sites, this month's
 * revenue and profit, site and service receivables) open the related page;
 * below are the completed sites still owed money and the latest activities.
 * Profit and receivable cards exist only for users who may see them. The
 * figures are reloaded every time the screen is shown, so changes made on
 * another page always appear here.
 */
@Component
@Scope("prototype")
public class HomeController {

    private static final String CLICKABLE_STYLE = "clickable-card";

    private final JobSummaryService jobSummaryService;
    private final AccessControl accessControl;
    private final TaskRunner taskRunner;
    private final JobNavigator jobNavigator;
    private final ContentNavigator contentNavigator;
    private final PendingJobsBadge pendingJobsBadge;

    @FXML
    private VBox rootContent;
    @FXML
    private VBox pendingJobsBox;
    @FXML
    private Label pendingJobsLabel;
    @FXML
    private FlowPane summaryCardsBox;
    @FXML
    private VBox completedBalanceBox;
    @FXML
    private VBox completedBalanceList;
    @FXML
    private VBox recentList;
    @FXML
    private ProgressIndicator loadingIndicator;

    public HomeController(JobSummaryService jobSummaryService, AccessControl accessControl, TaskRunner taskRunner,
            JobNavigator jobNavigator, ContentNavigator contentNavigator, PendingJobsBadge pendingJobsBadge) {
        this.jobSummaryService = jobSummaryService;
        this.accessControl = accessControl;
        this.taskRunner = taskRunner;
        this.jobNavigator = jobNavigator;
        this.contentNavigator = contentNavigator;
        this.pendingJobsBadge = pendingJobsBadge;
    }

    @FXML
    private void initialize() {
        accessControl.requireAdmin();
        pendingJobsBox.visibleProperty().bind(pendingJobsBadge.countProperty().greaterThan(0));
        pendingJobsBox.managedProperty().bind(pendingJobsBox.visibleProperty());
        pendingJobsLabel.textProperty().bind(Bindings.createStringBinding(() -> DialogUtil.message(
                "home.pendingJobs.text", pendingJobsBadge.countProperty().get()), pendingJobsBadge.countProperty()));
        taskRunner.run(() -> jobSummaryService.loadDashboard(YearMonth.now()), this::show, loadingIndicator,
                rootContent);
    }

    private void show(DashboardFigures figures) {
        summaryCardsBox.getChildren().setAll(cards(figures));
        showCompletedWithBalance(figures.completedSitesWithBalance());
        showRecentActivities(figures.recentActivities());
    }

    private List<Node> cards(DashboardFigures figures) {
        boolean financials = accessControl.canViewFinancials();
        List<Node> cards = new ArrayList<>();
        cards.add(card("home.card.activeSites", String.valueOf(figures.activeSiteCount()), null,
                jobNavigator::showSites));
        cards.add(card("home.card.monthlyRevenue", Bicimlendirici.money(figures.monthlyRevenue()),
                DialogUtil.message("home.card.monthlyRevenueCaption"), financials ? this::showMonthlyReport : null));
        if (financials) {
            String profitCaption = figures.monthlyProfitEstimated()
                    ? DialogUtil.message("home.stat.profitEstimated") : null;
            cards.add(card("home.card.monthlyProfit", Bicimlendirici.money(figures.monthlyProfit()), profitCaption,
                    this::showMonthlyReport));
            cards.add(card("home.card.pendingSiteReceivable", Bicimlendirici.money(figures.pendingSiteReceivable()),
                    null, jobNavigator::showSites));
            cards.add(card("home.card.pendingServicePayment",
                    Bicimlendirici.money(figures.pendingServicePayment()), null, jobNavigator::showServices));
        }
        return cards;
    }

    /** A summary card; clicking it opens the related page when {@code onClick} is given. */
    private VBox card(String titleKey, String value, String caption, Runnable onClick) {
        VBox card = StatBoxes.box(DialogUtil.message(titleKey), value, caption);
        card.getStyleClass().add("summary-card");
        if (onClick != null) {
            card.getStyleClass().add(CLICKABLE_STYLE);
            card.setOnMouseClicked(event -> onClick.run());
        }
        return card;
    }

    /** "Bekleyen işler" card: İş Takip, "Bekleyen" tab. */
    @FXML
    private void onOpenPendingJobs() {
        contentNavigator.<DailyPlanController>show(ViewPaths.DAILY_PLAN, DailyPlanController::showPendingTab);
    }

    private void showMonthlyReport() {
        contentNavigator.show(ViewPaths.MONTHLY_REPORT);
    }

    /** "Bitmiş ama tahsilatı eksik" list; each entry opens its site. */
    private void showCompletedWithBalance(List<Job> sites) {
        completedBalanceBox.setVisible(!sites.isEmpty());
        completedBalanceBox.setManaged(!sites.isEmpty());
        completedBalanceList.getChildren().setAll(sites.stream().map(this::completedBalanceLink).toList());
    }

    private Hyperlink completedBalanceLink(Job job) {
        Hyperlink link = new Hyperlink(DialogUtil.message("home.completedBalance.site", job.getCustomer().getName(),
                job.getName()));
        link.setOnAction(e -> jobNavigator.open(job.getId(), JobType.SITE));
        return link;
    }

    private void showRecentActivities(List<RecentActivity> activities) {
        if (activities.isEmpty()) {
            recentList.getChildren().setAll(new Label(DialogUtil.message("home.recent.empty")));
            return;
        }
        recentList.getChildren().setAll(activities.stream().map(this::activityLink).toList());
    }

    private Hyperlink activityLink(RecentActivity activity) {
        Hyperlink link = new Hyperlink(Bicimlendirici.date(activity.date()) + "  ·  " + activityText(activity));
        link.getStyleClass().add("recent-item");
        link.setOnAction(e -> jobNavigator.open(activity.jobId(), activity.jobType()));
        return link;
    }

    private static String activityText(RecentActivity activity) {
        return switch (activity.kind()) {
            case JOB_OPENED -> DialogUtil.message(activity.jobType() == JobType.SITE
                    ? "home.recent.siteOpened" : "home.recent.serviceOpened", activity.jobLabel());
            case MATERIAL -> DialogUtil.message("home.recent.material", activity.subject(),
                    Bicimlendirici.quantity(activity.value()), activity.jobLabel());
            case PAYMENT -> DialogUtil.message("home.recent.payment", Bicimlendirici.money(activity.value()),
                    activity.jobLabel());
        };
    }
}
