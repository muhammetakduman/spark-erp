package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.dto.ProductUsageReport;
import com.electrician.tracker.dto.ProductUsageRow;
import com.electrician.tracker.dto.ProductUsageSummary;
import com.electrician.tracker.report.ProductUsageExcelExportGenerator;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceHistoryServiceTest {

    private static final Long PRODUCT_ID = 4L;

    private final Product cable = new Product("NYA Kablo 2,5", ProductUnit.METER);
    private PriceHistoryService service;

    @BeforeEach
    void setUp() {
        Job site = job(1L, "Ahmet Bey", JobType.SITE, "Ümraniye Blok B");
        Job service = job(2L, "Mehmet Bey", JobType.SERVICE, "Pano arızası");
        MaterialItemRepository repository = mock(MaterialItemRepository.class);
        // The repository returns history newest first.
        when(repository.findHistoryByProductId(PRODUCT_ID)).thenReturn(List.of(
                item(service, LocalDate.of(2026, 9, 20), "5", "12", "Pazar A", "20"),
                item(site, LocalDate.of(2026, 9, 10), "10", "10", "Pazar B", "18"),
                item(site, LocalDate.of(2026, 8, 1), "3", "9", null, "25")));
        this.service = new PriceHistoryService(repository, mock(ProductUsageExcelExportGenerator.class));
    }

    @Test
    void listsWhoGotTheProductWhenAndForHowMuch() {
        ProductUsageReport report = service.usageReport(PRODUCT_ID, null, null);

        assertThat(report.rows()).extracting(ProductUsageRow::customerName)
                .containsExactly("Mehmet Bey", "Ahmet Bey", "Ahmet Bey");
        ProductUsageRow newest = report.rows().get(0);
        assertThat(newest.jobId()).isEqualTo(2L);
        assertThat(newest.jobType()).isEqualTo(JobType.SERVICE);
        assertThat(newest.saleTotal()).isEqualByComparingTo("100");
    }

    @Test
    void summarizesCountsPricesAndCheapestSupplier() {
        ProductUsageSummary summary = service.usageReport(PRODUCT_ID, null, null).summary();

        assertThat(summary.usageCount()).isEqualTo(3);
        assertThat(summary.totalQuantity()).isEqualByComparingTo("18");
        assertThat(summary.lastCustomerName()).isEqualTo("Mehmet Bey");
        assertThat(summary.lastDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(summary.minSaleUnitPrice()).isEqualByComparingTo("18");
        assertThat(summary.maxSaleUnitPrice()).isEqualByComparingTo("25");
        assertThat(summary.lastSaleUnitPrice()).isEqualByComparingTo("20");
        // The 9 ₺ purchase has no supplier, so it cannot name the cheapest one.
        assertThat(summary.cheapestSupplierName()).isEqualTo("Pazar B");
        assertThat(summary.cheapestPurchaseUnitPrice()).isEqualByComparingTo("10");
    }

    @Test
    void comparesSuppliersWithoutVat() {
        Job site = job(3L, "Veli Bey", JobType.SITE, "Depo");
        MaterialItem includedVat = item(site, LocalDate.of(2026, 9, 2), "1", "11.80", "Pazar C", "20");
        includedVat.setPurchaseVat(20, true);
        MaterialItem noVat = item(site, LocalDate.of(2026, 9, 1), "1", "10.50", "Pazar D", "20");
        MaterialItemRepository repository = mock(MaterialItemRepository.class);
        when(repository.findHistoryByProductId(PRODUCT_ID)).thenReturn(List.of(includedVat, noVat));
        PriceHistoryService vatService = new PriceHistoryService(repository,
                mock(ProductUsageExcelExportGenerator.class));

        ProductUsageSummary summary = vatService.usageReport(PRODUCT_ID, null, null).summary();

        // 11,80 incl. 20% = 9,83 excl., cheaper than 10,50 without VAT.
        assertThat(summary.cheapestSupplierName()).isEqualTo("Pazar C");
        assertThat(summary.cheapestPurchaseUnitPrice()).isEqualByComparingTo("9.83");
    }

    @Test
    void filtersByInclusiveDateRange() {
        ProductUsageReport report = service.usageReport(PRODUCT_ID, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 10));

        assertThat(report.rows()).extracting(ProductUsageRow::date).containsExactly(LocalDate.of(2026, 9, 10));
        assertThat(report.summary().usageCount()).isEqualTo(1);
    }

    @Test
    void emptyRangeGivesEmptySummary() {
        ProductUsageReport report = service.usageReport(PRODUCT_ID, LocalDate.of(2026, 10, 1), null);

        assertThat(report.rows()).isEmpty();
        assertThat(report.summary()).isEqualTo(ProductUsageSummary.empty());
    }

    @Test
    void rejectsRangeEndingBeforeStart() {
        assertThatThrownBy(() -> service.usageReport(PRODUCT_ID, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("error.productUsage.range.endBeforeStart");
    }

    private Job job(Long id, String customerName, JobType type, String name) {
        Job job = spy(new Job(new Customer(customerName, null, null, null, null), type, name, null, null, null,
                JobStatus.ACTIVE, null, null, false, null));
        doReturn(id).when(job).getId();
        return job;
    }

    private MaterialItem item(Job job, LocalDate date, String quantity, String purchase, String supplier,
            String sale) {
        return new MaterialItem(job, cable, date, new BigDecimal(quantity), new BigDecimal(purchase), supplier,
                new BigDecimal(sale), PriceEntryType.UNIT, null, null, null);
    }
}
