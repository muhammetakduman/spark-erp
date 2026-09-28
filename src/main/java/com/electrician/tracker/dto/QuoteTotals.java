package com.electrician.tracker.dto;

import java.math.BigDecimal;

/**
 * The totals block of a quote, in print order:
 * <pre>
 * TOPLAM        (items)
 * İSKONTO       (amount, or percentage of the items)
 * İŞÇİLİK
 * ARA TOPLAM    = items − discount + labor
 * KDV (%rate)   = subtotal × rate
 * GENEL TOPLAM  = subtotal + VAT
 * </pre>
 */
public record QuoteTotals(
        BigDecimal itemsTotal,
        BigDecimal discount,
        BigDecimal labor,
        BigDecimal subtotal,
        Integer vatRate,
        BigDecimal vatAmount,
        BigDecimal grandTotal) {
}
