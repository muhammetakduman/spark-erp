package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.domain.PaymentMethod;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.dto.EmployeeWageSummary;
import com.electrician.tracker.dto.JobSummary;
import org.junit.jupiter.api.Test;

class JobSummaryCalculatorTest {

    private final JobSummaryCalculator calculator = new JobSummaryCalculator();

    private final Customer customer = new Customer("Ahmet Yılmaz", null, null, null, null);
    private final Product cable = new Product("NYA Kablo 2.5mm", ProductUnit.METER);
    private final Product breaker = new Product("Sigorta", ProductUnit.PIECE);
    private final Employee master = new Employee("Mehmet Usta", BigDecimal.valueOf(1500), true, true);
    private final Employee helper = new Employee("Ali Çırak", BigDecimal.valueOf(800), false, true);

    @Test
    void calculatesSiteJobTotalsIncludingVatAndRemainingBalance() {
        Job job = new Job(customer, JobType.SITE, "Villa Elektrik Tesisatı", null,
                LocalDate.of(2026, 1, 1), null, JobStatus.ACTIVE,
                null, BigDecimal.valueOf(2000), false, null);

        List<MaterialItem> materials = List.of(
                new MaterialItem(job, cable, LocalDate.of(2026, 1, 2), BigDecimal.valueOf(100),
                        BigDecimal.valueOf(10), "Elektrik Pazarlama", BigDecimal.valueOf(15), PriceEntryType.UNIT, null, 20, null),
                new MaterialItem(job, breaker, LocalDate.of(2026, 1, 3), BigDecimal.valueOf(5),
                        null, null, BigDecimal.valueOf(50), PriceEntryType.UNIT, null, null, null));

        List<Attendance> attendances = List.of(
                new Attendance(job, master, LocalDate.of(2026, 1, 2), BigDecimal.valueOf(1500)),
                new Attendance(job, master, LocalDate.of(2026, 1, 3), BigDecimal.valueOf(1500)),
                new Attendance(job, helper, LocalDate.of(2026, 1, 2), BigDecimal.valueOf(800)));

        List<Payment> payments = List.of(
                new Payment(job, LocalDate.of(2026, 1, 5), BigDecimal.valueOf(3000), PaymentMethod.CASH, null));

        JobSummary summary = calculator.calculate(job, materials, attendances, payments);

        // Material sale excl. VAT: 100*15 + 5*50 = 1500 + 250 = 1750
        assertThat(summary.materialSaleTotalExcludingVat()).isEqualByComparingTo("1750.00");
        // Incl. VAT: (100*15*1.20) + (5*50) = 1800 + 250 = 2050
        assertThat(summary.materialSaleTotalIncludingVat()).isEqualByComparingTo("2050.00");
        // Purchase total only counts the cable line (breaker has no purchase price): 100*10 = 1000
        assertThat(summary.materialPurchaseTotal()).isEqualByComparingTo("1000.00");
        assertThat(summary.materialPurchasePriceMissingCount()).isEqualTo(1);
        // Attendance total: 1500 + 1500 + 800 = 3800
        assertThat(summary.attendanceTotal()).isEqualByComparingTo("3800.00");
        // Revenue = material sale excl. VAT + serviceFee(0) + laborFee(2000) = 1750 + 2000 = 3750
        assertThat(summary.saleExcludingVat()).isEqualByComparingTo("3750.00");
        // Cost = material purchase only; attendance is recorded but never costed = 1000
        assertThat(summary.costExcludingVat()).isEqualByComparingTo("1000.00");
        // Profit = revenue - cost = 3750 - 1000 = 2750
        assertThat(summary.profit()).isEqualByComparingTo("2750.00");
        assertThat(summary.collectedTotal()).isEqualByComparingTo("3000.00");
        // Remaining = sale incl. VAT - collected = (3750 + 300 VAT) - 3000 = 1050
        assertThat(summary.remaining()).isEqualByComparingTo("1050.00");
        assertThat(summary.paymentReceived()).isNull();
    }

    @Test
    void groupsAttendanceByEmployeeWithDayCountAndTotal() {
        Job job = new Job(customer, JobType.SITE, "Depo Aydınlatması", null, null, null, JobStatus.ACTIVE,
                null, null, false, null);

        List<Attendance> attendances = List.of(
                new Attendance(job, master, LocalDate.of(2026, 2, 1), BigDecimal.valueOf(1500)),
                new Attendance(job, master, LocalDate.of(2026, 2, 2), BigDecimal.valueOf(1600)),
                new Attendance(job, helper, LocalDate.of(2026, 2, 1), BigDecimal.valueOf(800)));

        JobSummary summary = calculator.calculate(job, List.of(), attendances, List.of());

        assertThat(summary.employeeWageSummaries())
                .extracting(EmployeeWageSummary::employeeName, s -> s.dayCount().stripTrailingZeros(),
                        EmployeeWageSummary::totalWage)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Ali Çırak", BigDecimal.ONE, new BigDecimal("800.00")),
                        org.assertj.core.groups.Tuple.tuple("Mehmet Usta", new BigDecimal("2"), new BigDecimal("3100.00")));
    }

    @Test
    void serviceMarkedAsPaidOwesNothing() {
        Job job = new Job(customer, JobType.SERVICE, "Priz Arızası", null, LocalDate.of(2026, 3, 1), null,
                JobStatus.COMPLETED, BigDecimal.valueOf(500), BigDecimal.valueOf(300), true, null);

        JobSummary summary = calculator.calculate(job, List.of(), List.of(), List.of());

        // Revenue = 0 (no materials) + serviceFee(500) + laborFee(300) = 800
        assertThat(summary.saleExcludingVat()).isEqualByComparingTo("800.00");
        assertThat(summary.costExcludingVat()).isEqualByComparingTo("0.00");
        assertThat(summary.profit()).isEqualByComparingTo("800.00");
        assertThat(summary.collectedTotal()).isEqualByComparingTo("0.00");
        assertThat(summary.remaining()).isEqualByComparingTo("0.00");
        assertThat(summary.isFullyPaid()).isTrue();
        assertThat(summary.paymentReceived()).isTrue();
    }

    @Test
    void servicePartialPaymentLeavesTheRestOwed() {
        Job job = new Job(customer, JobType.SERVICE, "Priz Arızası", null, LocalDate.of(2026, 3, 1), null,
                JobStatus.COMPLETED, BigDecimal.valueOf(1000), null, false, null);
        job.setServiceFeeVat(20, false);
        List<Payment> payments = List.of(
                new Payment(job, LocalDate.of(2026, 3, 1), BigDecimal.valueOf(500), PaymentMethod.CASH, null));

        JobSummary summary = calculator.calculate(job, List.of(), List.of(), payments);

        assertThat(summary.saleIncludingVat()).isEqualByComparingTo("1200.00");
        assertThat(summary.collectedTotal()).isEqualByComparingTo("500.00");
        assertThat(summary.remaining()).isEqualByComparingTo("700.00");
        assertThat(summary.isFullyPaid()).isFalse();
    }

    @Test
    void treatsMissingVatRateAsNoVatAddedOnTopOfSaleAmount() {
        Job job = new Job(customer, JobType.SITE, "Ofis Tesisatı", null, null, null, JobStatus.ACTIVE,
                null, null, false, null);

        List<MaterialItem> materials = List.of(
                new MaterialItem(job, cable, LocalDate.of(2026, 4, 1), BigDecimal.valueOf(10),
                        BigDecimal.valueOf(5), null, BigDecimal.valueOf(8), PriceEntryType.UNIT, null, null, null));

        JobSummary summary = calculator.calculate(job, materials, List.of(), List.of());

        assertThat(summary.materialSaleTotalExcludingVat()).isEqualByComparingTo("80.00");
        assertThat(summary.materialSaleTotalIncludingVat()).isEqualByComparingTo("80.00");
    }

    @Test
    void mixesIncludedFeeVatExcludedMaterialVatAndNoVatFee() {
        Job job = new Job(customer, JobType.SERVICE, "Pano arızası", null, LocalDate.of(2026, 9, 1), null,
                JobStatus.COMPLETED, BigDecimal.valueOf(1200), BigDecimal.valueOf(500), false, null);
        job.setServiceFeeVat(20, true);
        MaterialItem breakerLine = new MaterialItem(job, breaker, LocalDate.of(2026, 9, 1), BigDecimal.TEN,
                BigDecimal.valueOf(30), null, BigDecimal.valueOf(50), PriceEntryType.UNIT, null, 20, null);
        breakerLine.setVatIncluded(false);

        JobSummary summary = calculator.calculate(job, List.of(breakerLine), List.of(), List.of());

        // %20: fee 1200 incl. -> 1000 + 200; material 500 excl. -> 500 + 100. Labor fee 500 has no VAT.
        assertThat(summary.saleVat().rates()).singleElement().satisfies(rate -> {
            assertThat(rate.rate()).isEqualTo(20);
            assertThat(rate.excludingVat()).isEqualByComparingTo("1500.00");
            assertThat(rate.vatAmount()).isEqualByComparingTo("300.00");
        });
        assertThat(summary.saleExcludingVat()).isEqualByComparingTo("2000.00");
        assertThat(summary.saleVatAmount()).isEqualByComparingTo("300.00");
        assertThat(summary.saleIncludingVat()).isEqualByComparingTo("2300.00");
        // Purchase 10 x 30 excl. VAT = 300; profit on VAT-exclusive amounts: 2000 - 300
        assertThat(summary.costExcludingVat()).isEqualByComparingTo("300.00");
        assertThat(summary.profit()).isEqualByComparingTo("1700.00");
        assertThat(summary.isProfitEstimated()).isFalse();
    }

    @Test
    void siteBalanceIsOwedIncludingVat() {
        Job job = new Job(customer, JobType.SITE, "Daire", null, LocalDate.of(2026, 9, 1), null,
                JobStatus.ACTIVE, null, BigDecimal.valueOf(1000), false, null);
        job.setLaborFeeVat(20, false);
        List<Payment> payments = List.of(
                new Payment(job, LocalDate.of(2026, 9, 2), BigDecimal.valueOf(700), PaymentMethod.CASH, null));

        JobSummary summary = calculator.calculate(job, List.of(), List.of(), payments);

        assertThat(summary.saleIncludingVat()).isEqualByComparingTo("1200.00");
        assertThat(summary.remaining()).isEqualByComparingTo("500.00");
        assertThat(summary.profit()).isEqualByComparingTo("1000.00");
    }

    @Test
    void purchaseVatIsAppliedSeparatelyFromSaleVat() {
        Job job = new Job(customer, JobType.SITE, "Daire", null, LocalDate.of(2026, 9, 1), null,
                JobStatus.ACTIVE, null, null, false, null);
        // Sold at 120 incl. 20% VAT, bought at 80 excl. 20% VAT (supplier invoice).
        MaterialItem line = new MaterialItem(job, breaker, LocalDate.of(2026, 9, 1), BigDecimal.TEN,
                BigDecimal.valueOf(80), "Pazar A", BigDecimal.valueOf(120), PriceEntryType.UNIT, null, 20, null);
        line.setVatIncluded(true);
        line.setPurchaseVat(20, false);

        JobSummary summary = calculator.calculate(job, List.of(line), List.of(), List.of());

        assertThat(summary.saleExcludingVat()).isEqualByComparingTo("1000.00");
        assertThat(summary.saleIncludingVat()).isEqualByComparingTo("1200.00");
        assertThat(summary.costExcludingVat()).isEqualByComparingTo("800.00");
        assertThat(summary.profit()).isEqualByComparingTo("200.00");
    }

    @Test
    void purchaseWithoutVatCostsItsFullAmountEvenWhenSaleHasVat() {
        Job job = new Job(customer, JobType.SITE, "Daire", null, LocalDate.of(2026, 9, 1), null,
                JobStatus.ACTIVE, null, null, false, null);
        MaterialItem line = new MaterialItem(job, breaker, LocalDate.of(2026, 9, 1), BigDecimal.ONE,
                BigDecimal.valueOf(100), null, BigDecimal.valueOf(150), PriceEntryType.UNIT, null, 20, null);
        line.setVatIncluded(false);

        JobSummary summary = calculator.calculate(job, List.of(line), List.of(), List.of());

        assertThat(summary.costExcludingVat()).isEqualByComparingTo("100.00");
        assertThat(summary.profit()).isEqualByComparingTo("50.00");
    }
}
