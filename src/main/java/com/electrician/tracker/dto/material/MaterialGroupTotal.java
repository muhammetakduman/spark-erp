package com.electrician.tracker.dto.material;

import java.math.BigDecimal;

/**
 * One material group of a job: "Diafon · 4 kalem · Maliyet 18.450,00 ₺ ·
 * Satış 24.800,00 ₺". Amounts exclude VAT; the purchase total uses the lira
 * value frozen on each line. {@code groupName == null} is the
 * "Kategorisiz" group. Purchase total, profit and the missing-price count are
 * {@code null} for a user who may not see purchase prices.
 */
public record MaterialGroupTotal(String groupName, int lineCount, BigDecimal purchaseTotal, BigDecimal saleTotal,
        BigDecimal profit, Integer purchasePriceMissingCount) {

    public boolean isUncategorized() {
        return groupName == null;
    }

    public boolean hasFinancials() {
        return purchaseTotal != null;
    }

    public boolean hasMissingPurchasePrice() {
        return purchasePriceMissingCount != null && purchasePriceMissingCount > 0;
    }

    /** The same group without cost, profit and anything about purchase prices. */
    public MaterialGroupTotal withoutFinancials() {
        return new MaterialGroupTotal(groupName, lineCount, null, saleTotal, null, null);
    }
}
