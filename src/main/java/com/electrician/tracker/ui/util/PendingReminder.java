package com.electrician.tracker.ui.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.electrician.tracker.domain.DailyJobStatus;
import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.service.DailyJobService;
import com.electrician.tracker.service.UserPreferenceService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

/**
 * "Bekleyen işler" right after login: forgotten, not visited and postponed
 * daily jobs, each with "Bugüne Al", "Tamamlandı" and "İptal" so they can be
 * settled without leaving the window. A settled row disappears and the count
 * follows; the window closes by itself when the list is empty. Shown at most
 * once per user and day, only when something is waiting and the user has not
 * switched the reminder off; the menu badge keeps the number visible anyway.
 */
@Component
public class PendingReminder {

    private static final double ROW_SPACING = 6;
    private static final double LIST_MAX_HEIGHT = 360;

    private final DailyJobService dailyJobService;
    private final UserPreferenceService preferences;
    private final PendingJobsBadge pendingJobsBadge;

    public PendingReminder(DailyJobService dailyJobService, UserPreferenceService preferences,
            PendingJobsBadge pendingJobsBadge) {
        this.dailyJobService = dailyJobService;
        this.preferences = preferences;
        this.pendingJobsBadge = pendingJobsBadge;
    }

    /** @return whether anything was changed (the caller reloads its screen) */
    public boolean showIfDue() {
        LocalDate today = dailyJobService.today();
        if (!preferences.isReminderDue(today)) {
            return false;
        }
        List<DailyJobCard> items = dailyJobService.reminderItems();
        if (items.isEmpty()) {
            return false;
        }
        preferences.markReminderShown(today);
        return new ReminderWindow(items, today).show();
    }

    /** One opening of the reminder. */
    private final class ReminderWindow {

        private final Map<Long, Node> rows = new LinkedHashMap<>();
        private final Map<Long, DailyJobCard> cards = new LinkedHashMap<>();
        private final LocalDate today;
        private final Label countLabel = new Label();
        private final VBox list = new VBox();
        private SparkDialog dialog;
        private boolean changed;

        ReminderWindow(List<DailyJobCard> items, LocalDate today) {
            this.today = today;
            items.forEach(card -> {
                cards.put(card.id(), card);
                rows.put(card.id(), row(card));
            });
        }

        boolean show() {
            list.getStyleClass().add("reminder-list");
            list.getChildren().setAll(rows.values());
            ScrollPane scroll = new ScrollPane(list);
            scroll.setFitToWidth(true);
            scroll.setMaxHeight(LIST_MAX_HEIGHT);
            scroll.getStyleClass().add("reminder-scroll");
            countLabel.getStyleClass().add("spark-dialog-detail");
            countLabel.setWrapText(true);
            updateCount();
            dialog = SparkDialog.builder(SparkDialog.Kind.WARNING)
                    .title(DialogUtil.message("reminder.title"))
                    .content(new VBox(ROW_SPACING * 2, countLabel, scroll))
                    .primary(DialogUtil.message("reminder.moveAll"))
                    .secondary(DialogUtil.message("reminder.later"))
                    .wide()
                    .build();
            if (dialog.showAndWait() && !cards.isEmpty()) {
                moveAll();
            }
            pendingJobsBadge.refresh();
            return changed;
        }

        private Node row(DailyJobCard card) {
            Label date = new Label(Bicimlendirici.date(card.date()));
            date.getStyleClass().add("reminder-date");
            Label title = new Label("·  " + card.title());
            title.getStyleClass().add("reminder-title");
            title.setWrapText(true);
            if (card.isUrgent()) {
                title.setGraphic(StatusBadges.urgentMark());
                title.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            }
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            HBox top = new HBox(ROW_SPACING, date, title, gap, StatusBadges.of(shownStatus(card)));
            top.setAlignment(Pos.CENTER_LEFT);

            Label place = new Label(placeText(card));
            place.getStyleClass().add("reminder-place");
            place.setWrapText(true);
            HBox.setHgrow(place, Priority.ALWAYS);
            place.setMaxWidth(Double.MAX_VALUE);
            HBox bottom = new HBox(ROW_SPACING, place);
            bottom.setAlignment(Pos.CENTER_LEFT);
            if (card.date().isBefore(today)) {
                bottom.getChildren().add(Icons.button(AppIcon.TODAY, "reminder.moveToday", () -> moveToToday(card)));
            }
            bottom.getChildren().addAll(
                    Icons.button(AppIcon.STATUS_COMPLETED, "reminder.completed", () -> complete(card)),
                    Icons.button(AppIcon.CANCEL, "reminder.cancel", () -> cancel(card)));

            VBox row = new VBox(ROW_SPACING / 2, top);
            if (card.isFrequentlyPostponed()) {
                Label often = new Label(DialogUtil.message("reminder.postponedTimes", card.postponeCount()));
                often.getStyleClass().add("warning-badge");
                row.getChildren().add(often);
            }
            row.getChildren().add(bottom);
            row.getStyleClass().add("reminder-row");
            return row;
        }

        /** A planned job that already moved days is shown as "Ertelendi". */
        private DailyJobStatus shownStatus(DailyJobCard card) {
            return card.status() == DailyJobStatus.PLANNED && card.postponeCount() > 0
                    ? DailyJobStatus.POSTPONED : card.status();
        }

        private String placeText(DailyJobCard card) {
            List<String> parts = new ArrayList<>();
            if (card.customerName() != null) {
                parts.add(card.customerName());
            }
            if (card.address() != null) {
                parts.add(card.address());
            }
            return String.join(", ", parts);
        }

        private void moveToToday(DailyJobCard card) {
            settle(card, () -> dailyJobService.moveToToday(card.id()));
        }

        private void complete(DailyJobCard card) {
            settle(card, () -> dailyJobService.completePending(card.id()));
        }

        private void cancel(DailyJobCard card) {
            settle(card, () -> dailyJobService.cancelPending(card.id()));
        }

        private void moveAll() {
            try {
                dailyJobService.moveAllToToday(List.copyOf(cards.keySet()));
                changed = true;
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
            }
        }

        /** Runs the action; the row leaves the list, and the window closes when nothing is left. */
        private void settle(DailyJobCard card, Runnable action) {
            try {
                action.run();
            } catch (RuntimeException ex) {
                DialogUtil.showError(ex);
                return;
            }
            changed = true;
            cards.remove(card.id());
            list.getChildren().remove(rows.remove(card.id()));
            updateCount();
            if (cards.isEmpty()) {
                dialog.close();
            }
        }

        private void updateCount() {
            countLabel.setText(DialogUtil.message("reminder.count", cards.size()));
        }
    }
}
