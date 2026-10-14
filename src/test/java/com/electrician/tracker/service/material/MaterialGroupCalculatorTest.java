package com.electrician.tracker.service.material;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.customer.Customer;
import com.electrician.tracker.domain.job.Job;
import com.electrician.tracker.domain.job.JobStatus;
import com.electrician.tracker.domain.job.JobType;
import com.electrician.tracker.domain.material.MaterialItem;
import com.electrician.tracker.domain.product.CurrencyCode;
import com.electrician.tracker.domain.product.PriceEntryType;
import com.electrician.tracker.domain.product.Product;
import com.electrician.tracker.domain.product.ProductUnit;
import com.electrician.tracker.dto.job.JobSummary;
import com.electrician.tracker.dto.material.MaterialGroup;
import com.electrician.tracker.dto.material.MaterialGroupTotal;
import com.electrician.tracker.service.job.JobSummaryCalculator;
import org.junit.jupiter.api.Test;

class MaterialGroupCalculatorTest {

    private final MaterialGroupCalculator calculator = new MaterialGroupCalculator();
    private final Job job = new Job(new Customer("Ahmet Bey", null, null, null, null), JobType.SITE, "Blok A",
            null, LocalDate.of(2026, 10, 1), null, JobStatus.ACTIVE, null, null, false, null);
    private final Product panel = new Product("Ön panel set", ProductUnit.SET);
    private final Product keypad = new Product("Tuş paneli", ProductUnit.PIECE);
    private final Product cable = new Product("NYM 4x0,75 kablo", ProductUnit.METER);

    @Test
    void namesDifferingInCaseOrTurkishLettersAreOneGroupWithTheFirstSpelling() {
        List<MaterialGroup> groups = calculator.group(List.of(
                line(panel, "1", "8200", "11000", "Diafon"),
                line(keypad, "6", "1150", "1600", "diafon"),
                line(cable, "80", "42", "58", "DİAFON ")));

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).groupName()).isEqualTo("Diafon");
        assertThat(groups.get(0).total().lineCount()).isEqualTo(3);
    }

    @Test
    void groupFiguresAreHandCalculatedExcludingVat() {
        // Purchase 8.200 + 6 × 1.150 + 80 × 42 = 18.460; sale 11.000 + 9.600 + 4.640 = 25.240
        MaterialGroupTotal total = calculator.totals(List.of(
                line(panel, "1", "8200", "11000", "Diafon"),
                line(keypad, "6", "1150", "1600", "Diafon"),
                line(cable, "80", "42", "58", "Diafon"))).get(0);

        assertThat(total.purchaseTotal()).isEqualByComparingTo("18460.00");
        assertThat(total.saleTotal()).isEqualByComparingTo("25240.00");
        assertThat(total.profit()).isEqualByComparingTo("6780.00");
        assertThat(total.purchasePriceMissingCount()).isZero();
    }

    @Test
    void vatInclusiveLinesCountWithoutVat() {
        MaterialItem item = line(panel, "1", "1200", "2400", "Diafon");
        item.setVatRate(20);
        item.setVatIncluded(true);
        item.setPurchaseVat(20, true);

        MaterialGroupTotal total = calculator.total(List.of(item));

        assertThat(total.saleTotal()).isEqualByComparingTo("2000.00");
        assertThat(total.purchaseTotal()).isEqualByComparingTo("1000.00");
    }

    @Test
    void lineWithoutPurchasePriceIsCountedAndLeftOutOfCost() {
        MaterialGroupTotal total = calculator.totals(List.of(
                line(panel, "1", "8200", "11000", "Diafon"),
                line(keypad, "2", null, "1600", "Diafon"))).get(0);

        assertThat(total.purchaseTotal()).isEqualByComparingTo("8200.00");
        assertThat(total.purchasePriceMissingCount()).isEqualTo(1);
        assertThat(total.hasMissingPurchasePrice()).isTrue();
    }

    @Test
    void foreignPurchaseUsesTheFrozenLiraValue() {
        MaterialItem item = line(panel, "2", "100", "5000", "Diafon");
        item.setPurchaseCurrency(CurrencyCode.USD, new BigDecimal("40"), new BigDecimal("4000"));

        assertThat(calculator.total(List.of(item)).purchaseTotal()).isEqualByComparingTo("8000.00");
    }

    @Test
    void groupsAreInTurkishOrderWithUncategorizedLast() {
        List<MaterialGroupTotal> totals = calculator.totals(List.of(
                line(panel, "1", "1", "1", null),
                line(panel, "1", "1", "1", "Şalt"),
                line(panel, "1", "1", "1", "Çatı"),
                line(panel, "1", "1", "1", "  "),
                line(panel, "1", "1", "1", "Aydınlatma"),
                line(panel, "1", "1", "1", "Saha")));

        assertThat(totals).extracting(MaterialGroupTotal::groupName)
                .containsExactly("Aydınlatma", "Çatı", "Saha", "Şalt", null);
        assertThat(totals.get(4).lineCount()).isEqualTo(2);
    }

    @Test
    void summaryPutsTheMostSpentFirstAndUncategorizedLast() {
        List<MaterialGroupTotal> totals = calculator.totals(List.of(
                line(panel, "1", "50000", "60000", null),
                line(panel, "1", "100", "200", "Pano"),
                line(panel, "1", "900", "1000", "Diafon")));

        assertThat(MaterialGroupCalculator.bySpending(totals)).extracting(MaterialGroupTotal::groupName)
                .containsExactly("Diafon", "Pano", null);
    }

    @Test
    void linesInAGroupAreByProductName() {
        MaterialGroup group = calculator.group(List.of(
                line(keypad, "1", "1", "1", "Diafon"),
                line(cable, "1", "1", "1", "Diafon"),
                line(panel, "1", "1", "1", "Diafon"))).get(0);

        assertThat(group.lines()).extracting(item -> item.getProduct().getName())
                .containsExactly("NYM 4x0,75 kablo", "Ön panel set", "Tuş paneli");
    }

    @Test
    void totalOverAllLinesEqualsTheJobsMaterialTotals() {
        MaterialItem included = line(keypad, "3", "333.33", "777.77", "Pano");
        included.setVatRate(20);
        included.setVatIncluded(true);
        List<MaterialItem> items = List.of(line(panel, "1", "8200", "11000", "Diafon"), included,
                line(cable, "7", null, "13.13", null));

        MaterialGroupTotal total = calculator.total(items);
        JobSummary summary = new JobSummaryCalculator().calculate(job, items, List.of(), List.of());

        assertThat(total.saleTotal()).isEqualByComparingTo(summary.materialSaleTotalExcludingVat());
        assertThat(total.purchaseTotal()).isEqualByComparingTo(summary.materialPurchaseTotal());
        assertThat(total.lineCount()).isEqualTo(3);
    }

    @Test
    void withoutFinancialsKeepsOnlyCountAndSale() {
        MaterialGroupTotal masked = calculator.total(List.of(line(panel, "1", null, "100", "Diafon")))
                .withoutFinancials();

        assertThat(masked.purchaseTotal()).isNull();
        assertThat(masked.profit()).isNull();
        assertThat(masked.purchasePriceMissingCount()).isNull();
        assertThat(masked.saleTotal()).isEqualByComparingTo("100.00");
        assertThat(masked.hasFinancials()).isFalse();
    }

    private MaterialItem line(Product product, String quantity, String purchase, String sale, String group) {
        MaterialItem item = new MaterialItem(job, product, LocalDate.of(2026, 10, 1), new BigDecimal(quantity),
                purchase == null ? null : new BigDecimal(purchase), null, new BigDecimal(sale), PriceEntryType.UNIT,
                null, null, null);
        item.setGroupName(group);
        return item;
    }
}
