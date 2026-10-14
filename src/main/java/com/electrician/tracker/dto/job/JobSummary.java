package com.electrician.tracker.dto.job;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.job.JobType;
import com.electrician.tracker.dto.common.VatBreakdown;
import com.electrician.tracker.dto.employee.EmployeeWageSummary;

/**
 * Aggregated figures for a single {@link com.electrician.tracker.domain.job.Job}
 * ("IsOzeti"). {@code collectedTotal} sums the payments of any job;
 * {@code remaining} is what is still owed, zero for a service marked as paid
 * in full. {@code paymentReceived} is only set for
 * {@link JobType#SERVICE} jobs ({@code null} for sites).
 * Profit is always computed on VAT-exclusive amounts; the remaining balance
 * is what the customer still owes including VAT. {@code foreignCurrencyLineCount}: lines
 * bought in dollars or euros (their lira value uses the purchase-day rate).
 * {@code initialBalance} is the figure a site was taken on for and
 * {@code initialBalanceRemaining} how much of it the charges so far leave
 * untouched; both are {@code null} for services and for sites without an
 * agreed figure.
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
        BigDecimal initialBalance,
        BigDecimal initialBalanceRemaining,
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

    /** A site whose agreed figure was entered; only then is a balance shown. */
    public boolean hasInitialBalance() {
        return initialBalance != null;
    }

    /** The agreed figure has been spent past: what is left is negative. */
    public boolean isInitialBalanceOverrun() {
        return initialBalanceRemaining != null && initialBalanceRemaining.signum() < 0;
    }

    /** False for a copy made by {@link #withoutFinancials()}. */
    public boolean hasFinancials() {
        return profit != null;
    }

    /**
     * The same summary with cost, profit, purchase, wage, collected,
     * remaining and agreed-figure amounts removed; sale figures and day counts
     * stay. What a site was taken on for is the owner's business, so a manager
     * sees neither it nor what is left of it.
     */
    public JobSummary withoutFinancials() {
        return new JobSummary(jobId, jobType, materialSaleTotalExcludingVat, materialSaleTotalIncludingVat, null, 0,
                attendanceDayCount, null,
                employeeWageSummaries.stream().map(EmployeeWageSummary::withoutWages).toList(),
                saleVat, saleExcludingVat, saleVatAmount, saleIncludingVat, null, null, null, null, null, null,
                paymentReceived, 0);
    }
}
