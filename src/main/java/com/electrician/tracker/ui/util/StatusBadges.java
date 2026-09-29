package com.electrician.tracker.ui.util;

import java.util.Locale;

import com.electrician.tracker.domain.DailyJobStatus;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Status badges of daily jobs (small icon + text on a tinted background) and
 * the urgent mark. One meaning, one icon everywhere: done is always the tick,
 * not visited the cross, postponed the arrow, planned the clock, cancelled
 * the ban sign. Colours come from the theme ({@code -status-*}).
 */
public final class StatusBadges {

    private StatusBadges() {
    }

    public static Label of(DailyJobStatus status) {
        Label badge = new Label(EnumLabels.label(status), Icons.of(icon(status), IconSize.BADGE));
        badge.getStyleClass().addAll("status-badge", styleClass(status));
        return badge;
    }

    /** The red exclamation mark next to the title of an urgent job. */
    public static FontIcon urgentMark() {
        FontIcon mark = Icons.of(AppIcon.URGENT, IconSize.MARK);
        mark.getStyleClass().add("urgent-mark");
        Tooltip.install(mark, new Tooltip(DialogUtil.message("dailyJob.card.urgent")));
        return mark;
    }

    static AppIcon icon(DailyJobStatus status) {
        return switch (status) {
            case PLANNED -> AppIcon.STATUS_PLANNED;
            case COMPLETED -> AppIcon.STATUS_COMPLETED;
            case NOT_VISITED -> AppIcon.STATUS_NOT_VISITED;
            case POSTPONED -> AppIcon.STATUS_POSTPONED;
            case CANCELLED -> AppIcon.STATUS_CANCELLED;
        };
    }

    private static String styleClass(DailyJobStatus status) {
        return "status-" + status.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
