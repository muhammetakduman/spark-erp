package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.electrician.tracker.domain.JobType;

/**
 * One line of the main screen's "son hareketler" list. {@code subject} is the
 * product for a material line; {@code value} is the quantity of a material
 * line or the amount of a payment.
 */
public record RecentActivity(
        LocalDate date,
        Kind kind,
        Long jobId,
        JobType jobType,
        String jobLabel,
        String subject,
        BigDecimal value) {

    public enum Kind {
        JOB_OPENED,
        MATERIAL,
        PAYMENT
    }
}
