package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One supplier of a product: how often it was bought there and at which
 * VAT-exclusive unit prices (lowest, highest, plain average of the purchases).
 */
public record SupplierPriceComparison(
        String supplierName,
        int purchaseCount,
        BigDecimal lowestPrice,
        BigDecimal highestPrice,
        BigDecimal averagePrice,
        LocalDate lastPurchaseDate) {
}
