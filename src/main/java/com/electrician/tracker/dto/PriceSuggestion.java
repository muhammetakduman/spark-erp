package com.electrician.tracker.dto;

import java.math.BigDecimal;

import com.electrician.tracker.domain.CurrencyCode;

/**
 * Suggested values for a new {@link com.electrician.tracker.domain.MaterialItem}
 * of a product, taken from its most recent entry. The purchase price is
 * suggested as it was entered, in {@code lastPurchaseCurrency}. All fields
 * are {@code null} when the product has no history yet.
 */
public record PriceSuggestion(
        BigDecimal lastPurchaseUnitPrice,
        Integer lastPurchaseVatRate,
        Boolean lastPurchaseVatIncluded,
        String lastSupplierName,
        BigDecimal lastSaleUnitPrice,
        Integer lastVatRate,
        Boolean lastVatIncluded,
        CurrencyCode lastPurchaseCurrency) {

    public static PriceSuggestion empty() {
        return new PriceSuggestion(null, null, null, null, null, null, null, null);
    }

    /** Only the sale price and its VAT are suggested. */
    public PriceSuggestion withoutPurchaseInfo() {
        return new PriceSuggestion(null, null, null, null, lastSaleUnitPrice, lastVatRate, lastVatIncluded, null);
    }
}
