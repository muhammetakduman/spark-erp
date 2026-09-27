package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;

/**
 * The single place where a material line's sale total is derived from how
 * its price was entered: {@code UNIT} → quantity × unit price, {@code TOTAL}
 * → the entered total, with the unit price shown as total / quantity.
 */
public final class MaterialPriceCalculator {

    private static final int DISPLAY_SCALE = 2;

    private MaterialPriceCalculator() {
    }

    public static BigDecimal saleTotal(MaterialItem item) {
        if (item.getPriceEntryType() == PriceEntryType.TOTAL) {
            return item.getSaleTotalAmount();
        }
        return totalFromUnitPrice(item.getQuantity(), item.getSaleUnitPrice());
    }

    /** Returns {@code null} when either input is missing. */
    public static BigDecimal totalFromUnitPrice(BigDecimal quantity, BigDecimal unitPrice) {
        if (quantity == null || unitPrice == null) {
            return null;
        }
        return quantity.multiply(unitPrice);
    }

    /** Returns {@code null} when an input is missing or the quantity is not positive. */
    public static BigDecimal unitPriceFromTotal(BigDecimal total, BigDecimal quantity) {
        if (total == null || quantity == null || quantity.signum() <= 0) {
            return null;
        }
        return total.divide(quantity, DISPLAY_SCALE, RoundingMode.HALF_UP);
    }
}
