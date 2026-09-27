package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.PriceSuggestion;
import com.electrician.tracker.dto.ProductUsageReport;
import com.electrician.tracker.dto.ProductUsageRow;
import com.electrician.tracker.dto.ProductUsageSummary;
import com.electrician.tracker.report.ProductUsageExcelExportGenerator;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product price history and usage ("Ürün Kullanım Geçmişi"): to whom, when
 * and at what price a product was given. A product's history is small, so it
 * is loaded in one query and filtered by date in memory.
 */
@Service
public class PriceHistoryService {

    private final MaterialItemRepository materialItemRepository;
    private final ProductUsageExcelExportGenerator usageExcelExportGenerator;

    public PriceHistoryService(MaterialItemRepository materialItemRepository,
            ProductUsageExcelExportGenerator usageExcelExportGenerator) {
        this.materialItemRepository = materialItemRepository;
        this.usageExcelExportGenerator = usageExcelExportGenerator;
    }

    /**
     * Usage rows newest first, limited to {@code from}–{@code to} (inclusive;
     * either may be {@code null} for an open end), plus their summary.
     */
    @Transactional(readOnly = true)
    public ProductUsageReport usageReport(Long productId, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new ValidationException("error.productUsage.range.endBeforeStart");
        }
        List<ProductUsageRow> rows = materialItemRepository.findHistoryByProductId(productId).stream()
                .filter(item -> isWithin(item.getItemDate(), from, to))
                .map(PriceHistoryService::toRow)
                .toList();
        return new ProductUsageReport(rows, summarize(rows));
    }

    /** Writes the same rows {@link #usageReport} shows to an Excel file. */
    @Transactional(readOnly = true)
    public void exportUsage(Product product, LocalDate from, LocalDate to, Path outputFile) {
        usageExcelExportGenerator.export(product, usageReport(product.getId(), from, to), outputFile);
    }

    @Transactional(readOnly = true)
    public PriceSuggestion suggestFor(Long productId) {
        List<MaterialItem> history = materialItemRepository.findHistoryByProductId(productId);
        if (history.isEmpty()) {
            return PriceSuggestion.empty();
        }
        MaterialItem mostRecent = history.get(0);
        return new PriceSuggestion(
                mostRecent.getPurchaseUnitPrice(),
                mostRecent.getPurchaseVatRate(),
                mostRecent.getPurchaseVatIncluded(),
                mostRecent.getSupplierName(),
                mostRecent.getSaleUnitPrice(),
                mostRecent.getVatRate(),
                mostRecent.getVatIncluded());
    }

    /** Rows must be newest first. */
    static ProductUsageSummary summarize(List<ProductUsageRow> rows) {
        if (rows.isEmpty()) {
            return ProductUsageSummary.empty();
        }
        ProductUsageRow last = rows.get(0);
        List<BigDecimal> salePrices = rows.stream().map(ProductUsageRow::saleUnitPrice)
                .filter(Objects::nonNull).toList();
        Optional<ProductUsageRow> cheapest = rows.stream()
                .filter(row -> row.supplierName() != null && row.purchaseUnitPrice() != null)
                .min(Comparator.comparing(ProductUsageRow::purchaseUnitPriceExcludingVat));
        return new ProductUsageSummary(
                rows.size(),
                rows.stream().map(ProductUsageRow::quantity).reduce(BigDecimal.ZERO, BigDecimal::add),
                last.customerName(),
                last.date(),
                salePrices.stream().min(Comparator.naturalOrder()).orElse(null),
                salePrices.stream().max(Comparator.naturalOrder()).orElse(null),
                salePrices.isEmpty() ? null : salePrices.get(0),
                cheapest.map(ProductUsageRow::supplierName).orElse(null),
                cheapest.map(ProductUsageRow::purchaseUnitPriceExcludingVat).orElse(null));
    }

    private static boolean isWithin(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return from == null && to == null;
        }
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private static BigDecimal purchaseExcludingVat(MaterialItem item) {
        if (item.getPurchaseUnitPrice() == null) {
            return null;
        }
        return KdvHesaplayici.calculate(List.of(new KdvHesaplayici.Line(item.getPurchaseUnitPrice(),
                item.getPurchaseVatRate(), item.getPurchaseVatIncluded()))).excludingVat();
    }

    private static ProductUsageRow toRow(MaterialItem item) {
        return new ProductUsageRow(
                item.getJob().getId(),
                item.getJob().getType(),
                item.getItemDate(),
                item.getJob().getCustomer().getName(),
                item.getJob().getName(),
                item.getQuantity(),
                item.getPurchaseUnitPrice(),
                item.getPurchaseVatRate(),
                item.getPurchaseVatIncluded(),
                purchaseExcludingVat(item),
                item.getSupplierName(),
                item.getSaleUnitPrice(),
                item.getVatRate(),
                item.getVatIncluded(),
                MaterialPriceCalculator.saleTotal(item));
    }
}
