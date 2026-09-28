package com.electrician.tracker.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * One day of the work plan: its entries (urgent first, then by time) and
 * the three counters "Planlanan · Tamamlanan · Gidilmeyen" (not visited
 * includes entries moved to another day).
 */
public record DayPlan(LocalDate date, List<DailyJobCard> cards, int plannedCount, int completedCount,
        int notVisitedCount) {
}
