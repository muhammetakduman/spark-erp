package com.electrician.tracker.dto;

import com.electrician.tracker.domain.Product;

/**
 * Outcome of adding a product by name from a picker: either a newly created
 * catalog entry, or an existing one whose normalized name matched.
 */
public record ProductCreationResult(Product product, boolean existing) {
}
