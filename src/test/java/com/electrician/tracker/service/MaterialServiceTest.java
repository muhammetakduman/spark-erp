package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.CurrencyCode;
import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.service.exception.AccessDeniedException;
import com.electrician.tracker.service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MaterialServiceTest {

    private final Job job = new Job(new Customer("Ahmet Bey", null, null, null, null), JobType.SITE, "Şantiye",
            null, LocalDate.of(2026, 9, 1), null, JobStatus.ACTIVE, null, null, false, null);
    private final Product product = new Product("Kablo", ProductUnit.METER);

    private MaterialItemRepository repository;
    private MaterialService service;
    private ExchangeRateService exchangeRates;

    @BeforeEach
    void setUp() {
        repository = mock(MaterialItemRepository.class);
        when(repository.save(any(MaterialItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        exchangeRates = mock(ExchangeRateService.class);
        service = new MaterialService(repository, TestAccess.admin(), TestAccess.masker(TestAccess.admin()),
                exchangeRates);
    }

    @Test
    void rejectsLineWithoutProduct() {
        MaterialItem item = unitLine(null, "1", "10");

        assertThatThrownBy(() -> service.addItem(item))
                .isInstanceOf(ValidationException.class)
                .hasMessage("error.materialItem.product.required");
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsZeroQuantity() {
        assertThatThrownBy(() -> service.addItem(unitLine(product, "0", "10")))
                .hasMessage("error.materialItem.quantity.mustBePositive");
    }

    @Test
    void unitEntryRequiresUnitPrice() {
        assertThatThrownBy(() -> service.addItem(unitLine(product, "2", null)))
                .hasMessage("error.materialItem.saleUnitPrice.required");
    }

    @Test
    void totalEntryRequiresTotalAmount() {
        assertThatThrownBy(() -> service.addItem(totalLine("2", null)))
                .hasMessage("error.materialItem.saleTotalAmount.required");
    }

    @Test
    void rejectsNegativePrices() {
        assertThatThrownBy(() -> service.addItem(unitLine(product, "2", "-1")))
                .hasMessage("error.materialItem.saleUnitPrice.negative");
        assertThatThrownBy(() -> service.addItem(totalLine("2", "-5")))
                .hasMessage("error.materialItem.saleTotalAmount.negative");
    }

    @Test
    void totalEntryStoresDerivedUnitPrice() {
        MaterialItem saved = service.addItem(totalLine("3", "100"));

        assertThat(saved.getSaleUnitPrice()).isEqualByComparingTo("33.33");
        assertThat(saved.getSaleTotalAmount()).isEqualByComparingTo("100");
        assertThat(MaterialPriceCalculator.saleTotal(saved)).isEqualByComparingTo("100");
    }

    @Test
    void unitEntryDropsAnyStaleTotalAndDefaultsMissingEntryType() {
        MaterialItem item = new MaterialItem(job, product, LocalDate.of(2026, 9, 2), new BigDecimal("2"), null, null,
                new BigDecimal("5"), null, new BigDecimal("999"), null, null);

        MaterialItem saved = service.addItem(item);

        assertThat(saved.getPriceEntryType()).isEqualTo(PriceEntryType.UNIT);
        assertThat(saved.getSaleTotalAmount()).isNull();
        assertThat(MaterialPriceCalculator.saleTotal(saved)).isEqualByComparingTo("10");
    }

    @Test
    void purchasePriceAndSupplierAreOptionalAndSupplierIsTrimmed() {
        MaterialItem blankSupplier = unitLine(product, "1", "10");
        blankSupplier.setSupplierName("   ");
        MaterialItem paddedSupplier = unitLine(product, "1", "10");
        paddedSupplier.setSupplierName("  Elektrik Pazarlama ");

        assertThat(service.addItem(blankSupplier).getSupplierName()).isNull();
        assertThat(service.addItem(paddedSupplier).getSupplierName()).isEqualTo("Elektrik Pazarlama");
    }

    @Test
    void draftLineIsValidatedWithoutJobAndNotSaved() {
        MaterialItem draft = new MaterialItem(null, product, null, new BigDecimal("4"), null, null,
                null, PriceEntryType.TOTAL, new BigDecimal("10"), null, null);

        MaterialItem prepared = service.prepareDraft(draft);

        assertThat(prepared.getSaleUnitPrice()).isEqualByComparingTo("2.50");
        verify(repository, never()).save(any());
        assertThatThrownBy(() -> service.prepareDraft(unitLine(null, "1", "1")))
                .hasMessage("error.materialItem.product.required");
    }

    @Test
    void savedLineRequiresJob() {
        MaterialItem noJob = new MaterialItem(null, product, null, BigDecimal.ONE, null, null,
                BigDecimal.ONE, PriceEntryType.UNIT, null, null, null);

        assertThatThrownBy(() -> service.addItem(noJob)).hasMessage("error.materialItem.job.required");
    }

    @Test
    void sumsSaleTotalsOfMixedEntryTypes() {
        List<MaterialItem> items = List.of(
                service.prepareDraft(unitLine(product, "2", "7.5")),
                service.prepareDraft(totalLine("3", "100")));

        assertThat(service.sumSaleTotals(items)).isEqualByComparingTo("115");
    }

    private MaterialItem unitLine(Product lineProduct, String quantity, String unitPrice) {
        return new MaterialItem(job, lineProduct, LocalDate.of(2026, 9, 2), new BigDecimal(quantity), null, null,
                unitPrice == null ? null : new BigDecimal(unitPrice), PriceEntryType.UNIT, null, null, null);
    }

    private MaterialItem totalLine(String quantity, String total) {
        return new MaterialItem(job, product, LocalDate.of(2026, 9, 2), new BigDecimal(quantity), null, null,
                null, PriceEntryType.TOTAL, total == null ? null : new BigDecimal(total), null, null);
    }

    @Test
    void savedLineRequiresDate() {
        MaterialItem item = unitLine(product, "1", "10");
        item.setItemDate(null);

        assertThatThrownBy(() -> service.addItem(item)).hasMessage("error.materialItem.date.required");
        verify(repository, never()).save(any());
    }

    @Test
    void keepsPurchaseVatIndependentOfSaleVat() {
        MaterialItem item = unitLine(product, "10", "120");
        item.setPurchaseUnitPrice(new BigDecimal("80"));
        item.setVatRate(20);
        item.setVatIncluded(true);
        item.setPurchaseVat(20, false);

        MaterialItem saved = service.addItem(item);

        assertThat(saved.getVatIncluded()).isTrue();
        assertThat(saved.getPurchaseVatRate()).isEqualTo(20);
        assertThat(saved.getPurchaseVatIncluded()).isFalse();
    }

    @Test
    void dropsPurchaseVatWhenThereIsNoPurchasePrice() {
        MaterialItem item = unitLine(product, "1", "10");
        item.setPurchaseVat(20, false);

        MaterialItem saved = service.addItem(item);

        assertThat(saved.getPurchaseVatRate()).isNull();
        assertThat(saved.getPurchaseVatIncluded()).isNull();
    }

    @Test
    void rejectsInvalidPurchaseVatRate() {
        MaterialItem item = unitLine(product, "1", "10");
        item.setPurchaseUnitPrice(BigDecimal.ONE);
        item.setPurchaseVat(18, false);

        assertThatThrownBy(() -> service.addItem(item)).hasMessage("error.vat.rate.invalid");
    }

    @Test
    void foreignPurchaseIsFrozenInLiraAndItsRateRemembered() {
        MaterialItem item = unitLine(product, "2", "5000");
        item.setPurchaseUnitPrice(new BigDecimal("120"));
        item.setPurchaseCurrency(CurrencyCode.USD, new BigDecimal("34.25"), null);

        MaterialItem saved = service.addItem(item);

        assertThat(saved.getPurchaseUnitPriceTl()).isEqualByComparingTo("4110.00");
        assertThat(saved.getPurchaseUnitPrice()).isEqualByComparingTo("120");
        assertThat(saved.getPurchaseExchangeRate()).isEqualByComparingTo("34.25");
        assertThat(saved.isForeignCurrencyPurchase()).isTrue();
        verify(exchangeRates).remember(CurrencyCode.USD, new BigDecimal("34.25"));
    }

    @Test
    void foreignPurchaseNeedsAPositiveRate() {
        MaterialItem item = unitLine(product, "1", "10");
        item.setPurchaseUnitPrice(new BigDecimal("3"));
        item.setPurchaseCurrency(CurrencyCode.EUR, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> service.addItem(item)).hasMessage("error.exchangeRate.required");
        verify(repository, never()).save(any());
    }

    @Test
    void liraPurchaseHasRateOneAndItsOwnLiraValue() {
        MaterialItem item = unitLine(product, "1", "10");
        item.setPurchaseUnitPrice(new BigDecimal("7.5"));

        MaterialItem saved = service.addItem(item);

        assertThat(saved.getPurchaseCurrency()).isEqualTo(CurrencyCode.TRY);
        assertThat(saved.getPurchaseExchangeRate()).isEqualByComparingTo("1");
        assertThat(saved.getPurchaseUnitPriceTl()).isEqualByComparingTo("7.50");
        verify(exchangeRates, never()).remember(any(), any());
    }

    @Test
    void managerCannotAddMaterialToASite() {
        MaterialService managerService = new MaterialService(repository, TestAccess.manager(),
                TestAccess.masker(TestAccess.manager()), exchangeRates);

        assertThatThrownBy(() -> managerService.addItem(unitLine(product, "1", "10")))
                .isInstanceOf(AccessDeniedException.class);
    }
}
