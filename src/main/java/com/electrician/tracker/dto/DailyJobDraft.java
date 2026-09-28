package com.electrician.tracker.dto;

import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.DailyJobPriority;

/**
 * A daily job as the form sends it. {@code jobId} links it to an existing
 * site or service; {@code customerId} to a listed customer, otherwise
 * {@code customerName} keeps the typed name.
 */
public record DailyJobDraft(
        LocalDate date,
        String timeOfDay,
        String title,
        Long customerId,
        String customerName,
        String address,
        String phone,
        List<Long> employeeIds,
        DailyJobPriority priority,
        String note,
        Long jobId) {
}
