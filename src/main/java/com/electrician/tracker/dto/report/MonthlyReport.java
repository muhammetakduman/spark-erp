package com.electrician.tracker.dto.report;

import java.time.YearMonth;
import java.util.List;

import com.electrician.tracker.dto.employee.EmployeeWageSummary;
import com.electrician.tracker.dto.material.MaterialSummaryLine;

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
