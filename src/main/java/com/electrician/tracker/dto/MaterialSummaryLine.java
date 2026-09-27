package com.electrician.tracker.dto;

import java.math.BigDecimal;

import com.electrician.tracker.domain.ProductUnit;

/** A product's total quantity, purchase and sale (both VAT-exclusive) within a report. */
public record MaterialSummaryLine(
        String productName,
        ProductUnit unit,
        BigDecimal quantity,
        BigDecimal purchaseTotal,
        BigDecimal saleTotal) {
}
