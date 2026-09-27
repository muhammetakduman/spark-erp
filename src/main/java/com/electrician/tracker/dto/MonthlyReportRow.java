package com.electrician.tracker.dto;

import java.time.LocalDate;

import com.electrician.tracker.domain.JobType;

/**
 * One job of a monthly report. The job belongs to the month of its date (a
 * site's start date, a service's date), whenever it is paid.
 */
public record MonthlyReportRow(
        Long jobId,
        LocalDate date,
        String customerName,
        String jobName,
        JobType jobType,
        ReportTotals figures) {

    public boolean isFullyPaid() {
        return figures.uncollected().signum() == 0;
    }
}
