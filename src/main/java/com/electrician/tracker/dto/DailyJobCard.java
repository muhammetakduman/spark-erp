package com.electrician.tracker.dto;

import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.DailyJobPriority;
import com.electrician.tracker.domain.DailyJobStatus;

/**
 * One daily job as the plan screens and the printed program show it.
 * {@code postponeCount}: how many times this job was already moved to another
 * day before reaching this entry (counted along the "moved from" chain).
 */
public record DailyJobCard(
        Long id,
        LocalDate date,
        String timeOfDay,
        String title,
        String customerName,
        String address,
        String phone,
        List<Long> employeeIds,
        List<String> employeeNames,
        DailyJobStatus status,
        DailyJobPriority priority,
        int postponeCount,
        Long jobId,
        String jobLabel,
        String note,
        String completionNote,
        LocalDate postponedTo) {

    /** From this many postponements on the card gets an orange warning badge. */
    public static final int FREQUENTLY_POSTPONED = 3;

    public boolean isUrgent() {
        return priority == DailyJobPriority.URGENT;
    }

    /** Still waiting for "Gidildi" / "Gidilmedi". */
    public boolean isOpen() {
        return status == DailyJobStatus.PLANNED;
    }

    public boolean isLinkedToJob() {
        return jobId != null;
    }

    /** Linked, or unlinked with a customer (it becomes a service when done). */
    public boolean willHaveJob() {
        return isLinkedToJob() || (customerName != null && !customerName.isBlank());
    }

    public boolean isFrequentlyPostponed() {
        return postponeCount >= FREQUENTLY_POSTPONED;
    }
}
