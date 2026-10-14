package com.electrician.tracker.dto.product;

import java.math.BigDecimal;

/**
 * A VAT-exclusive purchase unit price and the supplier it was bought from
 * ({@code null} when no supplier was entered).
 */
public record SupplierPrice(BigDecimal unitPrice, String supplierName) {
}
