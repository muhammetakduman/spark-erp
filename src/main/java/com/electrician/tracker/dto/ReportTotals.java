package com.electrician.tracker.dto;

import java.math.BigDecimal;

/**
 * Summed figures of a set of jobs, all VAT-exclusive except {@code vatAmount}
 * (shown for information). {@code cost} is the material purchase cost only;
 * {@code wageTotal} is recorded attendance, shown but never deducted from
 * profit. {@code uncollected} sums what customers still owe (overpayments do
 * not reduce it).
 */
public record ReportTotals(
        int jobCount,
        BigDecimal revenue,
        BigDecimal vatAmount,
        BigDecimal materialCost,
        BigDecimal attendanceDays,
        BigDecimal wageTotal,
        BigDecimal cost,
        BigDecimal profit,
        BigDecimal collected,
        BigDecimal uncollected,
        int missingPurchasePriceCount) {

    public static final ReportTotals ZERO = new ReportTotals(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);

    /** One job's figures. */
    public static ReportTotals of(JobSummary summary) {
        return new ReportTotals(1, summary.saleExcludingVat(), summary.saleVatAmount(),
                summary.materialPurchaseTotal(), summary.attendanceDayCount(), summary.attendanceTotal(),
                summary.costExcludingVat(), summary.profit(), summary.collectedTotal(),
                summary.remaining().max(BigDecimal.ZERO), summary.materialPurchasePriceMissingCount());
    }

    public ReportTotals plus(ReportTotals other) {
        return new ReportTotals(jobCount + other.jobCount, revenue.add(other.revenue),
                vatAmount.add(other.vatAmount), materialCost.add(other.materialCost),
                attendanceDays.add(other.attendanceDays), wageTotal.add(other.wageTotal), cost.add(other.cost),
                profit.add(other.profit), collected.add(other.collected), uncollected.add(other.uncollected),
                missingPurchasePriceCount + other.missingPurchasePriceCount);
    }

    /** Lines without a purchase price add no cost, so the profit is only an estimate. */
    public boolean isProfitEstimated() {
        return missingPurchasePriceCount > 0;
    }
}
