package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.JobType;

/**
 * Aggregated figures for a single {@link com.electrician.tracker.domain.Job}
 * ("IsOzeti"). {@code collectedTotal} sums the payments of any job;
 * {@code remaining} is what is still owed, zero for a service marked as paid
 * in full. {@code paymentReceived} is only set for
 * {@link JobType#SERVICE} jobs ({@code null} for sites).
 * Profit is always computed on VAT-exclusive amounts; the remaining balance
 * is what the customer still owes including VAT. {@code foreignCurrencyLineCount}: lines
 * bought in dollars or euros (their lira value uses the purchase-day rate).
 */
public record JobSummary(
        Long jobId,
        JobType jobType,
        BigDecimal materialSaleTotalExcludingVat,
        BigDecimal materialSaleTotalIncludingVat,
        BigDecimal materialPurchaseTotal,
        int materialPurchasePriceMissingCount,
        BigDecimal attendanceDayCount,
        BigDecimal attendanceTotal,
        List<EmployeeWageSummary> employeeWageSummaries,
        VatBreakdown saleVat,
        BigDecimal saleExcludingVat,
        BigDecimal saleVatAmount,
        BigDecimal saleIncludingVat,
        BigDecimal costExcludingVat,
        BigDecimal profit,
        BigDecimal collectedTotal,
        BigDecimal remaining,
        Boolean paymentReceived,
        int foreignCurrencyLineCount) {

    /**
     * Nothing left to collect (overpayment counts as paid). Without financial
     * figures only a service's "paid in full" flag is known.
     */
    public boolean isFullyPaid() {
        if (remaining == null) {
            return Boolean.TRUE.equals(paymentReceived);
        }
        return remaining.signum() <= 0;
    }

    /** Lines without a purchase price add no cost, so the profit is only an estimate. */
    public boolean isProfitEstimated() {
        return materialPurchasePriceMissingCount > 0;
    }

    /** False for a copy made by {@link #withoutFinancials()}. */
    public boolean hasFinancials() {
        return profit != null;
    }

    /**
     * The same summary with cost, profit, purchase, wage, collected and
     * remaining figures removed; sale figures and day counts stay.
     */
    public JobSummary withoutFinancials() {
        return new JobSummary(jobId, jobType, materialSaleTotalExcludingVat, materialSaleTotalIncludingVat, null, 0,
                attendanceDayCount, null,
                employeeWageSummaries.stream().map(EmployeeWageSummary::withoutWages).toList(),
                saleVat, saleExcludingVat, saleVatAmount, saleIncludingVat, null, null, null, null, paymentReceived, 0);
    }
}
