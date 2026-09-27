package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.domain.PaymentMethod;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardCalculatorTest {

    private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);

    private final Customer customer = new Customer("Ahmet Bey", null, null, null, null);
    private final Product cable = new Product("Kablo", ProductUnit.METER);
    private final Employee usta = new Employee("Mustafa", new BigDecimal("500"), true, true);
    private final JobSummaryCalculator summaryCalculator = new JobSummaryCalculator();

    private Job activeSite;
    private Job completedSite;
    private Job unpaidService;
    private JobBoard board;

    @BeforeEach
    void setUp() {
        // Started this month: its labor fee counts in this month's profit.
        activeSite = job(1L, JobType.SITE, JobStatus.ACTIVE, LocalDate.of(2026, 9, 1), null, new BigDecimal("1000"));
        // Started last month, completed, still owed money.
        completedSite = job(2L, JobType.SITE, JobStatus.COMPLETED, LocalDate.of(2026, 8, 1), null,
                new BigDecimal("5000"));
        unpaidService = job(3L, JobType.SERVICE, JobStatus.COMPLETED, LocalDate.of(2026, 7, 1),
                new BigDecimal("300"), null);

        List<MaterialItem> activeMaterials = List.of(
                material(activeSite, LocalDate.of(2026, 9, 5), "10", "10", "15", 20),
                material(activeSite, LocalDate.of(2026, 8, 20), "10", "10", "100", null));
        List<Attendance> activeAttendance = List.of(
                new Attendance(activeSite, usta, LocalDate.of(2026, 9, 5), new BigDecimal("500"),
                        new BigDecimal("0.5"), null),
                new Attendance(activeSite, usta, LocalDate.of(2026, 8, 20), new BigDecimal("1000")));
        List<MaterialItem> completedMaterials = List.of(
                material(completedSite, LocalDate.of(2026, 9, 10), "1", null, "200", null));
        List<Payment> completedPayments = List.of(
                new Payment(completedSite, LocalDate.of(2026, 9, 15), new BigDecimal("1000"), PaymentMethod.CASH, null));

        Map<Long, JobSummary> summaries = Map.of(
                1L, summaryCalculator.calculate(activeSite, activeMaterials, activeAttendance, List.of()),
                2L, summaryCalculator.calculate(completedSite, completedMaterials, List.of(), completedPayments),
                3L, summaryCalculator.calculate(unpaidService, List.of(), List.of(), List.of()));
        board = new JobBoard(List.of(activeSite, completedSite, unpaidService), summaries,
                Map.of(1L, activeMaterials, 2L, completedMaterials), Map.of(1L, activeAttendance),
                Map.of(2L, completedPayments));
    }

    @Test
    void monthlyProfitUsesVatExclusiveAmountsOfTheMonthOnly() {
        DashboardFigures figures = new DashboardCalculator().calculate(board, SEPTEMBER);

        // Active site: sale 150 (excl. 20% VAT) + labor fee 1000 − purchase 100 = 1050 (wages are not deducted)
        // Completed site: sale 200 − no purchase price = 200 (fees are from August)
        assertThat(figures.monthlyProfit()).isEqualByComparingTo("1250.00");
        assertThat(figures.monthlyProfitEstimated()).isTrue();
    }

    @Test
    void summarizesCardsAndListsCompletedSitesWithBalance() {
        DashboardFigures figures = new DashboardCalculator().calculate(board, SEPTEMBER);

        assertThat(figures.activeSiteCount()).isEqualTo(1);
        // Active: 180 + 1000 + 1000 = 2180; completed: 5200 − 1000 = 4200
        assertThat(figures.pendingSiteReceivable()).isEqualByComparingTo("6380.00");
        assertThat(figures.pendingServicePayment()).isEqualByComparingTo("300.00");
        assertThat(figures.completedSitesWithBalance()).containsExactly(completedSite);
    }

    @Test
    void monthWithoutMissingPurchasePricesIsNotEstimated() {
        DashboardFigures figures = new DashboardCalculator().calculate(board, YearMonth.of(2026, 8));

        assertThat(figures.monthlyProfitEstimated()).isFalse();
    }

    private Job job(Long id, JobType type, JobStatus status, LocalDate start, BigDecimal serviceFee,
            BigDecimal laborFee) {
        Job job = spy(new Job(customer, type, "İş " + id, null, start, null, status, serviceFee, laborFee, false,
                null));
        doReturn(id).when(job).getId();
        return job;
    }

    private MaterialItem material(Job job, LocalDate date, String quantity, String purchase, String sale,
            Integer vatRate) {
        return new MaterialItem(job, cable, date, new BigDecimal(quantity), purchase == null ? null
                : new BigDecimal(purchase), null, new BigDecimal(sale), PriceEntryType.UNIT, null, vatRate, null);
    }
}
