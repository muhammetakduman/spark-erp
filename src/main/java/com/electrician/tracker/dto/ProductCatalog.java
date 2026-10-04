package com.electrician.tracker.dto;

import java.util.List;

/**
 * Everything the product screen needs in one load: every product with its
 * statistics, the values the category, brand and supplier filters offer, and
 * the material groups used on job lines (with whether any line has none).
 */
public record ProductCatalog(
        List<ProductOverview> products,
        List<String> categories,
        List<String> brands,
        List<String> suppliers,
        List<String> groups,
        boolean hasUngroupedLines) {
}
