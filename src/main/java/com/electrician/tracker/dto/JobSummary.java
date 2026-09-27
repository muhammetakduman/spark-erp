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
 * is what the customer still owes including VAT.
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
        Boolean paymentReceived) {

    /** Nothing left to collect (overpayment counts as paid). */
    public boolean isFullyPaid() {
        return remaining.signum() <= 0;
    }

    /** Lines without a purchase price add no cost, so the profit is only an estimate. */
    public boolean isProfitEstimated() {
        return materialPurchasePriceMissingCount > 0;
    }
}
