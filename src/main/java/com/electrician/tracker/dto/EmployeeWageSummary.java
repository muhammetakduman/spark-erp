package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-employee attendance total within a job. {@code dayCount} is the sum of
 * day factors (e.g. 2.5), never a row count; {@code averageWage} is the
 * average full-day wage (total / dayCount).
 */
public record EmployeeWageSummary(
        Long employeeId,
        String employeeName,
        boolean master,
        BigDecimal dayCount,
        BigDecimal averageWage,
        BigDecimal totalWage,
        List<WorkedDay> days) {
}
