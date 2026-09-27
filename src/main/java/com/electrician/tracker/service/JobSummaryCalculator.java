package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.dto.EmployeeWageSummary;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.VatBreakdown;

/**
 * Pure calculation of {@link JobSummary} figures from already-loaded entity
 * lists. Kept free of Spring/repository dependencies so it can be unit
 * tested directly against plain constructed entities. All VAT maths is
 * delegated to {@link KdvHesaplayici}.
 */
public class JobSummaryCalculator {

    private static final int MONEY_SCALE = 2;

    public JobSummary calculate(Job job, List<MaterialItem> materialItems, List<Attendance> attendances,
            List<Payment> payments) {
        VatBreakdown materialSale = KdvHesaplayici.calculate(materialSaleLines(materialItems));
        VatBreakdown sale = saleBreakdown(job, materialItems);
        BigDecimal materialPurchaseTotal = KdvHesaplayici.calculate(purchaseLines(materialItems)).excludingVat();
        int materialPurchaseMissingCount = countMissingPurchasePrice(materialItems);
        BigDecimal attendanceDayCount = AttendanceMath.dayCount(attendances);
        BigDecimal attendanceTotal = AttendanceMath.totalWage(attendances);
        List<EmployeeWageSummary> employeeWageSummaries = AttendanceMath.summarizeByEmployee(attendances);

        // Attendance is only recorded (days, wages); it never enters the job's cost or profit.
        BigDecimal cost = materialPurchaseTotal;
        BigDecimal profit = sale.excludingVat().subtract(cost);

        boolean isSite = job.getType() == JobType.SITE;
        BigDecimal collectedTotal = sumPayments(payments);
        // A service marked "payment received" is settled in full, whatever was entered as payments.
        boolean settled = !isSite && job.isPaymentReceived();
        BigDecimal remaining = settled ? BigDecimal.ZERO : sale.includingVat().subtract(collectedTotal);
        Boolean paymentReceived = isSite ? null : job.isPaymentReceived();

        return new JobSummary(
                job.getId(),
                job.getType(),
                materialSale.excludingVat(),
                materialSale.includingVat(),
                scale(materialPurchaseTotal),
                materialPurchaseMissingCount,
                attendanceDayCount,
                attendanceTotal,
                employeeWageSummaries,
                sale,
                sale.excludingVat(),
                sale.vatTotal(),
                sale.includingVat(),
                scale(cost),
                scale(profit),
                scale(collectedTotal),
                scale(remaining),
                paymentReceived);
    }

    /** Everything the customer is charged: material lines plus service and labor fees. */
    public VatBreakdown saleBreakdown(Job job, List<MaterialItem> materialItems) {
        List<KdvHesaplayici.Line> lines = new ArrayList<>(materialSaleLines(materialItems));
        lines.addAll(feeLines(job));
        return KdvHesaplayici.calculate(lines);
    }

    /** The job's service and labor fees as priced lines. */
    static List<KdvHesaplayici.Line> feeLines(Job job) {
        return List.of(
                new KdvHesaplayici.Line(job.getServiceFee(), job.getServiceFeeVatRate(),
                        job.getServiceFeeVatIncluded()),
                new KdvHesaplayici.Line(job.getLaborFee(), job.getLaborFeeVatRate(), job.getLaborFeeVatIncluded()));
    }

    static List<KdvHesaplayici.Line> materialSaleLines(List<MaterialItem> items) {
        return items.stream()
                .map(item -> new KdvHesaplayici.Line(MaterialPriceCalculator.saleTotal(item),
                        item.getVatRate(), item.getVatIncluded()))
                .toList();
    }

    /** Purchase cost lines with their own purchase VAT; lines without a purchase price are skipped. */
    static List<KdvHesaplayici.Line> purchaseLines(List<MaterialItem> items) {
        return items.stream()
                .filter(item -> item.getPurchaseUnitPrice() != null)
                .map(item -> new KdvHesaplayici.Line(item.getQuantity().multiply(item.getPurchaseUnitPrice()),
                        item.getPurchaseVatRate(), item.getPurchaseVatIncluded()))
                .toList();
    }

    static int countMissingPurchasePrice(List<MaterialItem> items) {
        return (int) items.stream().filter(item -> item.getPurchaseUnitPrice() == null).count();
    }

    private BigDecimal sumPayments(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
