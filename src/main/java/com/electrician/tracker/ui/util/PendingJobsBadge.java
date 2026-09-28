package com.electrician.tracker.ui.util;

import com.electrician.tracker.service.DailyJobService;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import org.springframework.stereotype.Component;

/**
 * The number next to "İş Takip" in the menu: daily jobs of earlier days that
 * are still open or were not visited. Refreshed in the background whenever
 * the plan changes or the main window is built.
 */
@Component
public class PendingJobsBadge {

    private final DailyJobService dailyJobService;
    private final ReadOnlyLongWrapper count = new ReadOnlyLongWrapper(0);

    public PendingJobsBadge(DailyJobService dailyJobService) {
        this.dailyJobService = dailyJobService;
    }

    public ReadOnlyLongProperty countProperty() {
        return count.getReadOnlyProperty();
    }

    public void refresh() {
        javafx.concurrent.Task<Long> task = new javafx.concurrent.Task<>() {
            @Override
            protected Long call() {
                return dailyJobService.pendingCount();
            }
        };
        task.setOnSucceeded(event -> count.set(task.getValue()));
        Thread thread = new Thread(task, "pending-badge");
        thread.setDaemon(true);
        thread.start();
    }

    public void reset() {
        count.set(0);
    }
}
