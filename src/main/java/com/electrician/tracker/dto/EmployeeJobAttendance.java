package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.JobType;

/** One job's share of an employee's attendance within a date range. */
public record EmployeeJobAttendance(
        Long jobId,
        JobType jobType,
        String jobLabel,
        BigDecimal dayCount,
        BigDecimal totalWage,
        List<WorkedDay> days) {
}
