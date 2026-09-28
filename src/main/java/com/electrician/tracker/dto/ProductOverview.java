package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.Product;

/**
 * One catalog product with its purchase statistics, for the product list:
 * how often and how much was bought, the lowest and highest VAT-exclusive
 * purchase price with their suppliers, the last purchase price and the total
 * expense (quantity × VAT-exclusive purchase price). {@code suppliers} lists
 * everybody it was bought from, for the supplier filter.
 */
public record ProductOverview(
        Product product,
        int purchaseCount,
        BigDecimal totalQuantity,
        SupplierPrice lowestPurchase,
        SupplierPrice highestPurchase,
        BigDecimal lastPurchasePrice,
        BigDecimal totalExpense,
        List<String> suppliers) {

    /** Counts and quantities only; every purchase price, supplier and expense is removed. */
    public ProductOverview withoutPurchaseInfo() {
        return new ProductOverview(product, purchaseCount, totalQuantity, null, null, null, null, List.of());
    }

    public BigDecimal lowestPurchasePrice() {
        return lowestPurchase == null ? null : lowestPurchase.unitPrice();
    }

    public BigDecimal highestPurchasePrice() {
        return highestPurchase == null ? null : highestPurchase.unitPrice();
    }
}
