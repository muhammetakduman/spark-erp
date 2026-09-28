package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Totals over a product's usage rows. Price fields are {@code null} when no
 * row carries that price. {@code totalCost} is quantity × VAT-exclusive
 * purchase price over the rows that have one; the cheapest and most expensive
 * supplier only consider rows with both a supplier and a purchase price,
 * compared without VAT.
 */
public record ProductUsageSummary(
        int usageCount,
        BigDecimal totalQuantity,
        BigDecimal totalCost,
        String lastCustomerName,
        LocalDate lastDate,
        BigDecimal minSaleUnitPrice,
        BigDecimal maxSaleUnitPrice,
        BigDecimal lastSaleUnitPrice,
        BigDecimal lastPurchaseUnitPrice,
        SupplierPrice cheapestSupplier,
        SupplierPrice mostExpensiveSupplier) {

    public static ProductUsageSummary empty() {
        return new ProductUsageSummary(0, BigDecimal.ZERO, null, null, null, null, null, null, null, null, null);
    }

    /** Sale side only; cost, purchase prices and suppliers are removed. */
    public ProductUsageSummary withoutPurchaseInfo() {
        return new ProductUsageSummary(usageCount, totalQuantity, null, lastCustomerName, lastDate, minSaleUnitPrice,
                maxSaleUnitPrice, lastSaleUnitPrice, null, null, null);
    }
}
