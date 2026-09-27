package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * VAT split of a set of priced lines: the VAT-exclusive subtotal, one entry
 * per VAT rate in use (ascending), and the grand total. {@code rates} is
 * empty when no line carries VAT.
 */
public record VatBreakdown(
        BigDecimal excludingVat,
        List<VatRateAmount> rates,
        BigDecimal vatTotal,
        BigDecimal includingVat) {

    public boolean hasVat() {
        return !rates.isEmpty();
    }

    public record VatRateAmount(int rate, BigDecimal excludingVat, BigDecimal vatAmount) {
    }
}
