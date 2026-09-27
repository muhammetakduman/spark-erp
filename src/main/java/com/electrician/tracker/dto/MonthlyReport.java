package com.electrician.tracker.dto;

import java.time.YearMonth;
import java.util.List;

/**
 * "Bu ay ne kazandım": the month's jobs (grouped by job date), their totals,
 * the products used and the attendance recorded on them.
 */
public record MonthlyReport(
        YearMonth month,
        ReportJobFilter filter,
        List<MonthlyReportRow> rows,
        ReportTotals totals,
        List<MaterialSummaryLine> materials,
        List<EmployeeWageSummary> wages) {
}
