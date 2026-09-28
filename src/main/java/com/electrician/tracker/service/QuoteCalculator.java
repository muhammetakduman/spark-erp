package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import com.electrician.tracker.domain.DiscountType;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.dto.QuoteTotals;

/**
 * Pure quote arithmetic. Quote prices are VAT-exclusive (the usual quote
 * form): line total = quantity × unit price; subtotal = items − discount +
 * labor; VAT is added on the subtotal. Every figure is rounded HALF_UP to
 * 2 digits.
 */
public final class QuoteCalculator {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private QuoteCalculator() {
    }

    /** quantity × unit price; {@code null} while either is missing. */
    public static BigDecimal lineTotal(BigDecimal quantity, BigDecimal unitPrice) {
        if (quantity == null || unitPrice == null) {
            return null;
        }
        return round(quantity.multiply(unitPrice));
    }

    public static QuoteTotals calculate(List<QuoteLine> lines, DiscountType discountType, BigDecimal discountValue,
            BigDecimal laborAmount, Integer vatRate) {
        BigDecimal itemsTotal = round(lines.stream()
                .map(line -> lineTotal(line.quantity(), line.unitPrice()))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal discount = discount(itemsTotal, discountType, discountValue);
        BigDecimal labor = round(orZero(laborAmount));
        BigDecimal subtotal = itemsTotal.subtract(discount).add(labor);
        BigDecimal vat = vatRate == null ? round(BigDecimal.ZERO)
                : round(subtotal.multiply(BigDecimal.valueOf(vatRate)).divide(ONE_HUNDRED));
        return new QuoteTotals(itemsTotal, discount, labor, subtotal, vatRate, vat, subtotal.add(vat));
    }

    static BigDecimal discount(BigDecimal itemsTotal, DiscountType type, BigDecimal value) {
        if (type == null || type == DiscountType.NONE || value == null) {
            return round(BigDecimal.ZERO);
        }
        if (type == DiscountType.PERCENT) {
            return round(itemsTotal.multiply(value).divide(ONE_HUNDRED));
        }
        return round(value);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
