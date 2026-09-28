package com.electrician.tracker.dto;

import java.util.List;

/** A product's usage rows (newest first), their summary and the per-supplier price comparison. */
public record ProductUsageReport(
        List<ProductUsageRow> rows,
        ProductUsageSummary summary,
        List<SupplierPriceComparison> suppliers) {

    /** The same report without purchase prices, suppliers and cost. */
    public ProductUsageReport withoutPurchaseInfo() {
        return new ProductUsageReport(rows.stream().map(ProductUsageRow::withoutPurchaseInfo).toList(),
                summary.withoutPurchaseInfo(), List.of());
    }
}
