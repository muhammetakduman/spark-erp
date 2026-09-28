package com.electrician.tracker.service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.PriceSuggestion;
import com.electrician.tracker.dto.ProductUsageReport;
import com.electrician.tracker.dto.ProductUsageRow;
import com.electrician.tracker.report.ProductUsageExcelExportGenerator;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product price history and usage ("Ürün Kullanım Geçmişi"): to whom, when
 * and at what price a product was given, and which supplier sold it cheapest.
 * A product's history is small, so it is loaded in one query and filtered by
 * date in memory. A MANAGER receives no purchase prices or suppliers.
 */
@Service
public class PriceHistoryService {

    private final MaterialItemRepository materialItemRepository;
    private final ProductUsageExcelExportGenerator usageExcelExportGenerator;
    private final AccessControl accessControl;

    public PriceHistoryService(MaterialItemRepository materialItemRepository,
            ProductUsageExcelExportGenerator usageExcelExportGenerator, AccessControl accessControl) {
        this.materialItemRepository = materialItemRepository;
        this.usageExcelExportGenerator = usageExcelExportGenerator;
        this.accessControl = accessControl;
    }

    /**
     * Usage rows newest first, limited to {@code from}–{@code to} (inclusive;
     * either may be {@code null} for an open end), their summary and the
     * supplier comparison.
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
        ProductUsageReport report = new ProductUsageReport(rows, PurchaseStatistics.summarize(rows),
                PurchaseStatistics.compareSuppliers(rows));
        return accessControl.canViewFinancials() ? report : report.withoutPurchaseInfo();
    }

    /** Writes the same rows {@link #usageReport} shows to an Excel file (without purchase columns for a MANAGER). */
    @Transactional(readOnly = true)
    public void exportUsage(Product product, LocalDate from, LocalDate to, Path outputFile) {
        usageExcelExportGenerator.export(product, usageReport(product.getId(), from, to), outputFile,
                accessControl.canViewFinancials());
    }

    @Transactional(readOnly = true)
    public PriceSuggestion suggestFor(Long productId) {
        List<MaterialItem> history = materialItemRepository.findHistoryByProductId(productId);
        if (history.isEmpty()) {
            return PriceSuggestion.empty();
        }
        MaterialItem mostRecent = history.get(0);
        PriceSuggestion suggestion = new PriceSuggestion(
                mostRecent.getPurchaseUnitPrice(),
                mostRecent.getPurchaseVatRate(),
                mostRecent.getPurchaseVatIncluded(),
                mostRecent.getSupplierName(),
                mostRecent.getSaleUnitPrice(),
                mostRecent.getVatRate(),
                mostRecent.getVatIncluded(),
                mostRecent.getPurchaseCurrency());
        return accessControl.canViewFinancials() ? suggestion : suggestion.withoutPurchaseInfo();
    }

    private static boolean isWithin(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return from == null && to == null;
        }
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private static ProductUsageRow toRow(MaterialItem item) {
        return new ProductUsageRow(
                item.getJob().getId(),
                item.getJob().getType(),
                item.getItemDate(),
                item.getJob().getCustomer().getName(),
                item.getJob().getName(),
                item.getQuantity(),
                item.getPurchaseUnitPriceTl(),
                item.getPurchaseVatRate(),
                item.getPurchaseVatIncluded(),
                PurchaseStatistics.unitPriceExcludingVat(item),
                item.getSupplierName(),
                item.getSaleUnitPrice(),
                item.getVatRate(),
                item.getVatIncluded(),
                MaterialPriceCalculator.saleTotal(item),
                item.getPurchaseCurrency(),
                item.getPurchaseUnitPrice(),
                item.getPurchaseExchangeRate());
    }
}
