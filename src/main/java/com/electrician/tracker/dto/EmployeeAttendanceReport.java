package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

/** An employee's attendance in a date range, grouped by job, with overall totals. */
public record EmployeeAttendanceReport(
        List<EmployeeJobAttendance> jobs,
        BigDecimal dayCount,
        BigDecimal totalWage) {

    /** Day counts only; every wage total is removed. */
    public EmployeeAttendanceReport withoutWages() {
        return new EmployeeAttendanceReport(jobs.stream().map(EmployeeJobAttendance::withoutWages).toList(),
                dayCount, null);
    }
}
