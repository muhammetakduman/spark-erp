package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Totals over a product's usage rows. Price fields are {@code null} when no
 * row carries that price; the cheapest supplier only considers rows that
 * have both a supplier and a purchase price, compared without VAT.
 */
public record ProductUsageSummary(
        int usageCount,
        BigDecimal totalQuantity,
        String lastCustomerName,
        LocalDate lastDate,
        BigDecimal minSaleUnitPrice,
        BigDecimal maxSaleUnitPrice,
        BigDecimal lastSaleUnitPrice,
        String cheapestSupplierName,
        BigDecimal cheapestPurchaseUnitPrice) {

    public static ProductUsageSummary empty() {
        return new ProductUsageSummary(0, BigDecimal.ZERO, null, null, null, null, null, null, null);
    }
}
