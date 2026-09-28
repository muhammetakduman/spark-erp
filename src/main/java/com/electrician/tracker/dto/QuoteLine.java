package com.electrician.tracker.dto;

import java.math.BigDecimal;

import com.electrician.tracker.domain.ProductUnit;

/**
 * One quote line as the editor holds it: a catalog product ({@code productId}
 * set) or a free item such as "Sarf malzeme" (only {@code productName}). The
 * unit price is VAT-exclusive and always typed by the user. The brand is the
 * quote's own copy (pre-filled from the product, freely editable).
 * {@code lineNo} is the only order of the lines: 1..n, set by
 * {@link com.electrician.tracker.service.QuoteLineNumbering}.
 */
public record QuoteLine(
        int lineNo,
        Long productId,
        String productName,
        String brand,
        BigDecimal quantity,
        ProductUnit unit,
        BigDecimal unitPrice,
        String description) {

    /** A line that has not been placed in a quote yet (numbered when added). */
    public static QuoteLine unnumbered(Long productId, String productName, String brand, BigDecimal quantity,
            ProductUnit unit, BigDecimal unitPrice, String description) {
        return new QuoteLine(0, productId, productName, brand, quantity, unit, unitPrice, description);
    }

    public boolean isFreeItem() {
        return productId == null;
    }

    public QuoteLine withLineNo(int newLineNo) {
        return new QuoteLine(newLineNo, productId, productName, brand, quantity, unit, unitPrice, description);
    }
}
