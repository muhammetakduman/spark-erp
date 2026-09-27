package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.MaterialSummaryLine;
import com.electrician.tracker.dto.MonthlyReport;
import com.electrician.tracker.dto.MonthlyReportRow;
import com.electrician.tracker.dto.ReportJobFilter;
import com.electrician.tracker.dto.ReportTotals;
import com.electrician.tracker.dto.YearlyReport;

/**
 * Pure monthly/yearly report figures from an already-loaded {@link JobBoard}.
 * Every money figure comes from the board's {@code JobSummary} (the same
 * numbers the main screen shows), so nothing is calculated twice.
 * <p>
 * A job counts in the month of its date: a site's start date, a service's
 * date. A September job paid in October is September revenue; what is still
 * owed shows as "tahsil edilmeyen".
 */
public class AylikRaporHesaplayici {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    public MonthlyReport monthly(JobBoard board, YearMonth month, ReportJobFilter filter) {
        List<Job> jobs = jobsOf(board, filter, date -> YearMonth.from(date).equals(month));
        List<MonthlyReportRow> rows = jobs.stream().map(job -> row(board, job)).toList();
        ReportTotals totals = rows.stream().map(MonthlyReportRow::figures).reduce(ReportTotals.ZERO, ReportTotals::plus);
        List<MaterialItem> materials = jobs.stream().flatMap(job -> board.materialsFor(job.getId()).stream()).toList();
        List<Attendance> attendances = jobs.stream()
                .flatMap(job -> board.attendancesFor(job.getId()).stream())
                .toList();
        return new MonthlyReport(month, filter, rows, totals, summarizeMaterials(materials),
                AttendanceMath.summarizeByEmployee(attendances));
    }

    public YearlyReport yearly(JobBoard board, int year, ReportJobFilter filter) {
        Map<YearMonth, ReportTotals> byMonth = new LinkedHashMap<>();
        for (Month month : Month.values()) {
            byMonth.put(YearMonth.of(year, month), ReportTotals.ZERO);
        }
        for (Job job : jobsOf(board, filter, date -> date.getYear() == year)) {
            byMonth.merge(YearMonth.from(job.getStartDate()), totalsOf(board, job), ReportTotals::plus);
        }
        List<YearlyReport.MonthTotals> months = byMonth.entrySet().stream()
                .map(entry -> new YearlyReport.MonthTotals(entry.getKey(), entry.getValue()))
                .toList();
        ReportTotals total = byMonth.values().stream().reduce(ReportTotals.ZERO, ReportTotals::plus);
        return new YearlyReport(year, filter, months, total);
    }

    private static List<Job> jobsOf(JobBoard board, ReportJobFilter filter, Predicate<LocalDate> dateMatches) {
        return board.jobs().stream()
                .filter(job -> job.getStartDate() != null && dateMatches.test(job.getStartDate()))
                .filter(job -> filter.matches(job.getType()))
                .sorted(Comparator.comparing(Job::getStartDate).thenComparing(Job::getId))
                .toList();
    }

    private static MonthlyReportRow row(JobBoard board, Job job) {
        return new MonthlyReportRow(job.getId(), job.getStartDate(), job.getCustomer().getName(), job.getName(),
                job.getType(), totalsOf(board, job));
    }

    private static ReportTotals totalsOf(JobBoard board, Job job) {
        return ReportTotals.of(board.summaries().get(job.getId()));
    }

    /** Per product, most used first; purchase and sale VAT-exclusive, as in the job summaries. */
    static List<MaterialSummaryLine> summarizeMaterials(List<MaterialItem> materials) {
        Map<Long, List<MaterialItem>> byProduct = new LinkedHashMap<>();
        for (MaterialItem item : materials) {
            byProduct.computeIfAbsent(item.getProduct().getId(), id -> new java.util.ArrayList<>()).add(item);
        }
        Comparator<MaterialSummaryLine> byName = Comparator.comparing(MaterialSummaryLine::productName,
                Collator.getInstance(TURKISH));
        return byProduct.entrySet().stream()
                .map(entry -> summaryLine(entry.getValue().get(0).getProduct(), entry.getValue()))
                .sorted(Comparator.comparing(MaterialSummaryLine::quantity).reversed().thenComparing(byName))
                .toList();
    }

    private static MaterialSummaryLine summaryLine(Product product, List<MaterialItem> items) {
        BigDecimal quantity = items.stream().map(MaterialItem::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purchase = KdvHesaplayici.calculate(JobSummaryCalculator.purchaseLines(items)).excludingVat();
        BigDecimal sale = KdvHesaplayici.calculate(JobSummaryCalculator.materialSaleLines(items)).excludingVat();
        return new MaterialSummaryLine(product.getName(), product.getUnit(), quantity, purchase, sale);
    }
}
