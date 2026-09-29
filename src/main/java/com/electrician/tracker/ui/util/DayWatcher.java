package com.electrician.tracker.ui.util;

import java.time.LocalDate;

import com.electrician.tracker.service.DailyJobService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

/**
 * A light once-a-minute check for a program that stays open for days: when
 * the date changes (after midnight) "today" moves on, so the İş Takip screen
 * and the pending badge switch to the new day; with the "Sistem" theme it
 * also follows a Windows dark-mode change. It asks the database nothing
 * unless the date actually changed.
 */
@Component
public class DayWatcher {

    private static final Duration TICK = Duration.minutes(1);

    private final DailyJobService dailyJobService;
    private final PendingJobsBadge pendingJobsBadge;
    private final ThemeService themeService;
    private final ReadOnlyObjectWrapper<LocalDate> today = new ReadOnlyObjectWrapper<>();
    private Timeline timeline;

    public DayWatcher(DailyJobService dailyJobService, PendingJobsBadge pendingJobsBadge, ThemeService themeService) {
        this.dailyJobService = dailyJobService;
        this.pendingJobsBadge = pendingJobsBadge;
        this.themeService = themeService;
    }

    /** Starts ticking (once; later calls do nothing). */
    public void start() {
        if (timeline != null) {
            return;
        }
        today.set(dailyJobService.today());
        timeline = new Timeline(new KeyFrame(TICK, event -> tick()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /** The current day; changes right after midnight. */
    public ReadOnlyObjectProperty<LocalDate> todayProperty() {
        return today.getReadOnlyProperty();
    }

    private void tick() {
        LocalDate now = dailyJobService.today();
        if (!now.equals(today.get())) {
            today.set(now);
            pendingJobsBadge.refresh();
        }
        themeService.refreshSystemTheme();
    }
}
