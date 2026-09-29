package com.electrician.tracker.ui.controller;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.electrician.tracker.dto.AttendanceSaveResult;
import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.dto.DailyJobCompletion;
import com.electrician.tracker.dto.DayPlan;
import com.electrician.tracker.dto.WeekPlan;
import com.electrician.tracker.service.AccessControl;
import com.electrician.tracker.service.DailyJobService;
import com.electrician.tracker.service.DailyPlanCalculator;
import com.electrician.tracker.ui.util.AppIcon;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.BulkDeleteFlow;
import com.electrician.tracker.ui.util.BulkSelection;
import com.electrician.tracker.ui.util.DailyJobCardView;
import com.electrician.tracker.ui.util.DayWatcher;
import com.electrician.tracker.ui.util.DeleteConfirmation;
import com.electrician.tracker.ui.util.DialogUtil;
import com.electrician.tracker.ui.util.EmptyState;
import com.electrician.tracker.ui.util.ModalStageOpener;
import com.electrician.tracker.ui.util.PendingJobsBadge;
import com.electrician.tracker.ui.util.StatBoxes;
import com.electrician.tracker.ui.util.TaskRunner;
import com.electrician.tracker.ui.util.ViewPaths;
import com.electrician.tracker.ui.util.WeekGridView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * "İş Takip": the day's jobs as cards with "Gidildi" / "Gidilmedi" and the
 * counters Planlanan · Tamamlanan · Gidilmeyen; the week as a grid of days
 * and people; and earlier jobs still waiting ("Bekleyen") with "Bugüne
 * aktar". Every change reloads the shown tab and the menu badge.
 */
@Component
@Scope("prototype")
public class DailyPlanController implements DailyJobCardView.Actions {

    private final DailyJobService dailyJobService;
    private final ModalStageOpener modalStageOpener;
    private final TaskRunner taskRunner;
    private final PendingJobsBadge pendingJobsBadge;
    private final DayWatcher dayWatcher;
    private final AccessControl accessControl;
    private final ChangeListener<LocalDate> dayListener = (obs, oldDay, newDay) -> onDayChanged(oldDay, newDay);

    @FXML
    private BorderPane rootContent;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Label noticeLabel;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab dayTab;
    @FXML
    private Tab weekTab;
    @FXML
    private Tab pendingTab;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Label dayNameLabel;
    @FXML
    private FlowPane counterBox;
    @FXML
    private VBox dayCardList;
    @FXML
    private Label weekLabel;
    @FXML
    private StackPane weekHolder;
    @FXML
    private VBox pendingList;

    private final DailyJobCardView cardView = new DailyJobCardView(this);
    private List<DailyJobCard> shownCards = List.of();
    private final BulkSelection<DailyJobCard> selection =
            new BulkSelection<>(() -> shownCards, DailyJobCard::id, this::deleteSelected);
    private final Set<Long> noteEditorsOpen = new HashSet<>();
    private LocalDate weekDay;

    public DailyPlanController(DailyJobService dailyJobService, ModalStageOpener modalStageOpener,
            TaskRunner taskRunner, PendingJobsBadge pendingJobsBadge, DayWatcher dayWatcher,
            AccessControl accessControl) {
        this.dailyJobService = dailyJobService;
        this.modalStageOpener = modalStageOpener;
        this.taskRunner = taskRunner;
        this.pendingJobsBadge = pendingJobsBadge;
        this.dayWatcher = dayWatcher;
        this.accessControl = accessControl;
    }

    @FXML
    private void initialize() {
        LocalDate today = dailyJobService.today();
        weekDay = today;
        datePicker.setValue(today);
        datePicker.valueProperty().addListener((obs, old, date) -> {
            if (date != null) {
                noteEditorsOpen.clear();
                loadDay();
            }
        });
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> reloadShownTab());
        dayWatcher.todayProperty().addListener(new WeakChangeListener<>(dayListener));
        if (accessControl.isAdmin()) {
            cardView.setSelectBox(selection::checkBoxFor);
            selection.placeAbove(tabPane);
            selection.installKeys(rootContent);
        }
        loadDay();
        loadPendingCount();
    }

    /** Opens the "Bekleyen" tab (from the home screen's pending-jobs card). */
    public void showPendingTab() {
        tabPane.getSelectionModel().select(pendingTab);
    }

    /** After midnight the screen moves on to the new day if it was showing "today". */
    private void onDayChanged(LocalDate oldDay, LocalDate newDay) {
        if (newDay == null) {
            return;
        }
        if (oldDay == null || oldDay.equals(datePicker.getValue())) {
            datePicker.setValue(newDay);
        } else {
            reloadShownTab();
        }
    }

    // ---- Loading ---------------------------------------------------------------

    private void reloadShownTab() {
        Tab shown = tabPane.getSelectionModel().getSelectedItem();
        if (shown == weekTab) {
            loadWeek();
        } else if (shown == pendingTab) {
            loadPending();
        } else {
            loadDay();
        }
        loadPendingCount();
        pendingJobsBadge.refresh();
    }

    private void loadDay() {
        LocalDate date = datePicker.getValue();
        taskRunner.run(() -> dailyJobService.dayPlan(date), this::showDay, loadingIndicator, rootContent);
    }

    private void showDay(DayPlan plan) {
        showCards(plan.cards());
        dayNameLabel.setText(Bicimlendirici.dateWithDay(plan.date()));
        counterBox.getChildren().setAll(
                counter("dailyPlan.counter.planned", plan.plannedCount(), "stat-box-planned"),
                counter("dailyPlan.counter.completed", plan.completedCount(), "stat-box-ok"),
                counter("dailyPlan.counter.notVisited", plan.notVisitedCount(), "stat-box-due"));
        if (plan.cards().isEmpty()) {
            dayCardList.getChildren().setAll(EmptyState.of(AppIcon.DAILY_PLAN, "dailyPlan.day.empty",
                    "dailyPlan.action.add", this::onAdd));
            return;
        }
        dayCardList.getChildren().setAll(plan.cards().stream()
                .map(card -> cardView.dayCard(card, noteEditorsOpen.contains(card.id())))
                .toList());
    }

    private static Node counter(String titleKey, int value, String styleClass) {
        VBox box = StatBoxes.box(DialogUtil.message(titleKey), String.valueOf(value), null);
        box.getStyleClass().addAll("summary-card", styleClass);
        return box;
    }

    /** The cards now on screen are the ones a bulk selection works on. */
    private void showCards(List<DailyJobCard> cards) {
        shownCards = cards;
        selection.clear();
    }

    /** "Seçilenleri Sil" for daily jobs. */
    private void deleteSelected(List<DailyJobCard> cards) {
        BulkDeleteFlow.of(cards, DailyJobCard::id,
                        card -> Bicimlendirici.date(card.date()) + " – " + card.title())
                .itemCount("bulk.count.dailyJobs")
                .delete(dailyJobService::deleteAll)
                .afterwards(this::reloadShownTab)
                .run();
    }

    private void loadWeek() {
        LocalDate day = weekDay;
        LocalDate today = dailyJobService.today();
        taskRunner.run(() -> dailyJobService.weekPlan(day), plan -> showWeek(plan, today), loadingIndicator,
                rootContent);
    }

    private void showWeek(WeekPlan plan, LocalDate today) {
        showCards(List.of());
        List<LocalDate> days = plan.days();
        weekLabel.setText(DialogUtil.message("dailyPlan.week.range", Bicimlendirici.date(days.get(0)),
                Bicimlendirici.date(days.get(days.size() - 1))));
        weekHolder.getChildren().setAll(WeekGridView.build(plan, today, this::openDay));
    }

    private void openDay(LocalDate day) {
        datePicker.setValue(day);
        tabPane.getSelectionModel().select(dayTab);
    }

    private void loadPending() {
        taskRunner.run(dailyJobService::pending, this::showPending, loadingIndicator, rootContent);
    }

    private void showPending(List<DailyJobCard> cards) {
        showCards(cards);
        updatePendingTitle(cards.size());
        if (cards.isEmpty()) {
            pendingList.getChildren().setAll(EmptyState.of(AppIcon.PENDING, "dailyPlan.pending.empty"));
            return;
        }
        pendingList.getChildren().setAll(cards.stream().map(cardView::pendingCard).toList());
    }

    private void loadPendingCount() {
        taskRunner.run(dailyJobService::pendingCount, count -> updatePendingTitle(count.intValue()),
                loadingIndicator, pendingList);
    }

    private void updatePendingTitle(int count) {
        pendingTab.setText(count == 0 ? DialogUtil.message("dailyPlan.tab.pending")
                : DialogUtil.message("dailyPlan.tab.pendingCount", count));
    }

    // ---- Navigation ------------------------------------------------------------

    @FXML
    private void onPreviousDay() {
        datePicker.setValue(datePicker.getValue().minusDays(1));
    }

    @FXML
    private void onNextDay() {
        datePicker.setValue(datePicker.getValue().plusDays(1));
    }

    @FXML
    private void onToday() {
        datePicker.setValue(dailyJobService.today());
    }

    @FXML
    private void onPreviousWeek() {
        weekDay = weekDay.minusWeeks(1);
        loadWeek();
    }

    @FXML
    private void onNextWeek() {
        weekDay = weekDay.plusWeeks(1);
        loadWeek();
    }

    @FXML
    private void onThisWeek() {
        weekDay = DailyPlanCalculator.weekStart(dailyJobService.today());
        loadWeek();
    }

    // ---- Actions ---------------------------------------------------------------

    @FXML
    private void onAdd() {
        LocalDate date = datePicker.getValue();
        DailyJobFormController form = modalStageOpener.openAndWait(ViewPaths.DAILY_JOB_FORM, "dailyJob.dialog.new",
                window(), controller -> controller.startForDate(date));
        if (form.isSaved()) {
            reloadShownTab();
        }
    }

    /** "Günlük Program" PDF of the shown day, to print and carry. */
    @FXML
    private void onPrint() {
        LocalDate date = datePicker.getValue();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(DialogUtil.message("dailyPlan.print.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName(DialogUtil.message("dailyPlan.print.fileName", Bicimlendirici.date(date)));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        taskRunner.run(() -> {
            dailyJobService.exportDayPdf(date, file.toPath());
            return file;
        }, saved -> DialogUtil.showInfo("dailyPlan.print.success"), loadingIndicator, rootContent);
    }

    /** One click completes the job; a small note field then opens on the card (optional). */
    @Override
    public void done(DailyJobCard card, boolean writeAttendance) {
        try {
            DailyJobCompletion completion = dailyJobService.complete(card.id(), null, writeAttendance);
            noteEditorsOpen.add(card.id());
            showNotice(completionNotice(completion, writeAttendance));
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        reloadShownTab();
    }

    /** "Servisler listesine eklendi." and/or "Puantaja 2 kayıt yazıldı (…)."; null when there is nothing to say. */
    private static String completionNotice(DailyJobCompletion completion, boolean writeAttendance) {
        List<String> parts = new ArrayList<>();
        if (completion.serviceCreated()) {
            parts.add(DialogUtil.message("dailyJob.serviceCreated"));
        }
        if (writeAttendance) {
            AttendanceSaveResult attendance = completion.attendance();
            parts.add(DialogUtil.message("dailyJob.attendanceWritten", attendance.createdCount(),
                    attendance.skippedCount()));
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    @Override
    public void notDone(DailyJobCard card) {
        NotVisitedDialogController dialog = modalStageOpener.openAndWait(ViewPaths.NOT_VISITED_DIALOG,
                "dailyJob.notVisited.title", window(), controller -> controller.forJob(card));
        dialog.getDecision().ifPresent(decision -> {
            try {
                dailyJobService.markNotVisited(card.id(), decision.reason(), decision.moveToNextDay());
                showNotice(decision.moveToNextDay() ? DialogUtil.message("dailyJob.movedToNextDay",
                        Bicimlendirici.date(card.date().plusDays(1))) : null);
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
            reloadShownTab();
        });
    }

    @Override
    public void saveCompletionNote(DailyJobCard card, String note) {
        try {
            dailyJobService.updateCompletionNote(card.id(), note);
            noteEditorsOpen.remove(card.id());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        reloadShownTab();
    }

    @Override
    public void edit(DailyJobCard card) {
        DailyJobFormController form = modalStageOpener.openAndWait(ViewPaths.DAILY_JOB_FORM, "dailyJob.dialog.edit",
                window(), controller -> controller.editExisting(card.id()));
        if (form.isSaved()) {
            reloadShownTab();
        }
    }

    @Override
    public void cancel(DailyJobCard card) {
        if (!DialogUtil.confirm("dailyJob.confirm.cancel", card.title())) {
            return;
        }
        try {
            dailyJobService.cancel(card.id());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        reloadShownTab();
    }

    @Override
    public void delete(DailyJobCard card) {
        if (!DeleteConfirmation.confirmSingle(DialogUtil.message("delete.single.dailyJob",
                Bicimlendirici.date(card.date()), card.title()))) {
            return;
        }
        try {
            dailyJobService.delete(card.id());
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        reloadShownTab();
    }

    @Override
    public void moveToToday(DailyJobCard card) {
        try {
            dailyJobService.moveToToday(card.id());
            showNotice(DialogUtil.message("dailyJob.movedToToday", card.title()));
        } catch (RuntimeException ex) {
            DialogUtil.showError(ex);
        }
        reloadShownTab();
    }

    /** A short line under the title instead of a pop-up; {@code null} hides it. */
    private void showNotice(String text) {
        boolean shown = text != null;
        noticeLabel.setText(shown ? text : "");
        noticeLabel.setVisible(shown);
        noticeLabel.setManaged(shown);
    }

    private Window window() {
        return rootContent.getScene().getWindow();
    }
}
