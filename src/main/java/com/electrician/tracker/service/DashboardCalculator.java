package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.RecentActivity;

/**
 * Pure calculation of the main-screen figures from an already-loaded
 * {@link JobBoard}; no queries, so refreshing one job never reloads the rest.
 * <p>
 * Monthly revenue: VAT-exclusive sales of the material lines dated in the
 * month, plus a job's service/labor fees when the job started in that month.
 * Monthly profit: that revenue minus the VAT-exclusive purchase cost of the
 * same material lines. Attendance wages are only recorded, never deducted.
 */
public class DashboardCalculator {

    static final int RECENT_ACTIVITY_LIMIT = 5;
    private static final int MONEY_SCALE = 2;

    private final RecentActivities recentActivities = new RecentActivities();

    public DashboardFigures calculate(JobBoard board, YearMonth month) {
        return calculate(board, month, Set.of(RecentActivity.Kind.values()));
    }

    /** {@code activityKinds}: which kinds of activity the "son hareketler" list may show. */
    public DashboardFigures calculate(JobBoard board, YearMonth month, Set<RecentActivity.Kind> activityKinds) {
        List<Job> sites = jobsOfType(board, JobType.SITE);
        long activeSiteCount = sites.stream().filter(job -> job.getStatus() == JobStatus.ACTIVE).count();
        List<Job> completedWithBalance = sites.stream()
                .filter(job -> job.getStatus() == JobStatus.COMPLETED)
                .filter(job -> hasBalance(board.summaries().get(job.getId())))
                .sorted(Comparator.comparing(Job::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<MaterialItem> monthMaterials = new ArrayList<>();
        BigDecimal monthlyRevenue = BigDecimal.ZERO;
        BigDecimal monthlyCost = BigDecimal.ZERO;
        for (Job job : board.jobs()) {
            List<MaterialItem> materials = inMonth(board.materialsFor(job.getId()), MaterialItem::getItemDate, month);
            monthMaterials.addAll(materials);
            monthlyRevenue = monthlyRevenue.add(monthlyRevenue(job, materials, month));
            monthlyCost = monthlyCost.add(
                    KdvHesaplayici.calculate(JobSummaryCalculator.purchaseLines(materials)).excludingVat());
        }

        return new DashboardFigures(activeSiteCount, scale(monthlyRevenue), scale(monthlyRevenue.subtract(monthlyCost)),
                JobSummaryCalculator.countMissingPurchasePrice(monthMaterials) > 0,
                pendingBalance(board, sites), pendingBalance(board, jobsOfType(board, JobType.SERVICE)),
                completedWithBalance,
                recentActivities.latest(board, activityKinds, RECENT_ACTIVITY_LIMIT));
    }

    /** What customers still owe on the services of an already-loaded board. */
    public BigDecimal pendingServicePayment(JobBoard board) {
        return pendingBalance(board, jobsOfType(board, JobType.SERVICE));
    }

    private BigDecimal monthlyRevenue(Job job, List<MaterialItem> materials, YearMonth month) {
        List<KdvHesaplayici.Line> saleLines = new ArrayList<>(JobSummaryCalculator.materialSaleLines(materials));
        if (isInMonth(job.getStartDate(), month)) {
            saleLines.addAll(JobSummaryCalculator.feeLines(job));
        }
        return KdvHesaplayici.calculate(saleLines).excludingVat();
    }

    private static BigDecimal pendingBalance(JobBoard board, List<Job> jobs) {
        return scale(jobs.stream()
                .map(job -> board.summaries().get(job.getId()))
                .filter(DashboardCalculator::hasBalance)
                .map(JobSummary::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
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
