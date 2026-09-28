package com.electrician.tracker.dto;

import java.math.BigDecimal;

/**
 * A VAT-exclusive purchase unit price and the supplier it was bought from
 * ({@code null} when no supplier was entered).
 */
public record SupplierPrice(BigDecimal unitPrice, String supplierName) {
}
