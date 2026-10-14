package com.electrician.tracker.dto.material;

import java.math.BigDecimal;

import com.electrician.tracker.domain.product.ProductUnit;

/** A product's total quantity, purchase and sale (both VAT-exclusive) within a report. */
public record MaterialSummaryLine(
        String productName,
        ProductUnit unit,
        BigDecimal quantity,
        BigDecimal purchaseTotal,
        BigDecimal saleTotal) {
}
