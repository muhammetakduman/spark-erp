package com.electrician.tracker.dto;

import java.math.BigDecimal;

/**
 * Suggested values for a new {@link com.electrician.tracker.domain.MaterialItem}
 * of a product, taken from its most recent entry. All fields are {@code null}
 * when the product has no history yet.
 */
public record PriceSuggestion(
        BigDecimal lastPurchaseUnitPrice,
        Integer lastPurchaseVatRate,
        Boolean lastPurchaseVatIncluded,
        String lastSupplierName,
        BigDecimal lastSaleUnitPrice,
        Integer lastVatRate,
        Boolean lastVatIncluded) {

    public static PriceSuggestion empty() {
        return new PriceSuggestion(null, null, null, null, null, null, null);
    }
}
