package com.electrician.tracker.dto;

import java.time.YearMonth;
import java.util.List;

/** Twelve months of a year (every month present, empty ones zero) and the year's total. */
public record YearlyReport(
        int year,
        ReportJobFilter filter,
        List<MonthTotals> months,
        ReportTotals total) {

    public record MonthTotals(YearMonth month, ReportTotals totals) {
    }
}
