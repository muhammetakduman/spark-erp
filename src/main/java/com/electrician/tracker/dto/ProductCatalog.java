package com.electrician.tracker.dto;

import java.util.List;

/**
 * Everything the product screen needs in one load: every product with its
 * statistics, and the values the category, brand and supplier filters offer.
 */
public record ProductCatalog(
        List<ProductOverview> products,
        List<String> categories,
        List<String> brands,
        List<String> suppliers) {
}
