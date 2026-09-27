package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobSummary;

/**
 * Pure calculation of the main-screen figures from an already-loaded
 * {@link JobBoard}; no queries, so refreshing one job never reloads the rest.
 * <p>
 * Monthly profit: VAT-exclusive sales minus VAT-exclusive purchase cost of the
 * material lines dated in the month, plus a job's service/labor fees when the
 * job started in that month. Attendance wages are only recorded, never
 * deducted from profit.
 */
public class DashboardCalculator {

    private static final int MONEY_SCALE = 2;

    public DashboardFigures calculate(JobBoard board, YearMonth month) {
        List<Job> sites = jobsOfType(board, JobType.SITE);
        long activeSiteCount = sites.stream().filter(job -> job.getStatus() == JobStatus.ACTIVE).count();
        List<Job> completedWithBalance = sites.stream()
                .filter(job -> job.getStatus() == JobStatus.COMPLETED)
                .filter(job -> hasBalance(board.summaries().get(job.getId())))
                .sorted(Comparator.comparing(Job::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        BigDecimal pendingSiteReceivable = sites.stream()
                .map(job -> board.summaries().get(job.getId()))
                .filter(DashboardCalculator::hasBalance)
                .map(JobSummary::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingServicePayment = jobsOfType(board, JobType.SERVICE).stream()
                .map(job -> board.summaries().get(job.getId()))
                .filter(DashboardCalculator::hasBalance)
                .map(JobSummary::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MaterialItem> monthMaterials = new ArrayList<>();
        BigDecimal monthlyProfit = BigDecimal.ZERO;
        for (Job job : board.jobs()) {
            List<MaterialItem> materials = inMonth(board.materialsFor(job.getId()), MaterialItem::getItemDate, month);
            monthMaterials.addAll(materials);
            monthlyProfit = monthlyProfit.add(monthlyProfit(job, materials, month));
        }

        return new DashboardFigures(activeSiteCount, scale(monthlyProfit),
                JobSummaryCalculator.countMissingPurchasePrice(monthMaterials) > 0,
                scale(pendingSiteReceivable), scale(pendingServicePayment), completedWithBalance);
    }

    private BigDecimal monthlyProfit(Job job, List<MaterialItem> materials, YearMonth month) {
        List<KdvHesaplayici.Line> saleLines = new ArrayList<>(JobSummaryCalculator.materialSaleLines(materials));
        if (isInMonth(job.getStartDate(), month)) {
            saleLines.addAll(JobSummaryCalculator.feeLines(job));
        }
        BigDecimal revenue = KdvHesaplayici.calculate(saleLines).excludingVat();
        BigDecimal cost = KdvHesaplayici.calculate(JobSummaryCalculator.purchaseLines(materials)).excludingVat();
        return revenue.subtract(cost);
    }

    private static List<Job> jobsOfType(JobBoard board, JobType type) {
        return board.jobs().stream().filter(job -> job.getType() == type).toList();
    }

    private static boolean hasBalance(JobSummary summary) {
        return summary != null && summary.remaining() != null && summary.remaining().signum() > 0;
    }

    private static <T> List<T> inMonth(List<T> rows, Function<T, LocalDate> date, YearMonth month) {
        return rows.stream().filter(row -> isInMonth(date.apply(row), month)).toList();
    }

    private static boolean isInMonth(LocalDate date, YearMonth month) {
        return date != null && YearMonth.from(date).equals(month);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
