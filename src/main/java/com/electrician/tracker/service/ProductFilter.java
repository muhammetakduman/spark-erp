package com.electrician.tracker.service;

import java.util.List;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.dto.ProductOverview;

/**
 * The product search: free text matched against name and brand, plus
 * optional category, brand and supplier filters, all applied together. Every
 * comparison ignores case, Turkish letters and extra spaces. A {@code null}
 * or blank criterion means "Tümü". Filtering happens in memory; the catalog
 * is small and no extra database column is needed.
 */
public record ProductFilter(String text, String category, String brand, String supplier) {

    public static final ProductFilter NONE = new ProductFilter(null, null, null, null);

    public boolean isActive() {
        return !isBlank(text) || !isBlank(category) || !isBlank(brand) || !isBlank(supplier);
    }

    public List<ProductOverview> apply(List<ProductOverview> products) {
        return products.stream().filter(this::matches).toList();
    }

    public boolean matches(ProductOverview overview) {
        return matches(overview.product()) && (isBlank(supplier)
                || overview.suppliers().stream().anyMatch(name -> sameText(supplier, name)));
    }

    /** Text, category and brand only (a bare product has no supplier). */
    public boolean matches(Product product) {
        return matchesText(product)
                && (isBlank(category) || sameText(category, product.getCategory()))
                && (isBlank(brand) || sameText(brand, product.getBrand()));
    }

    private boolean matchesText(Product product) {
        if (isBlank(text)) {
            return true;
        }
        String typed = MetinKarsilastirici.normalize(text);
        return MetinKarsilastirici.normalize(product.getName()).contains(typed)
                || MetinKarsilastirici.normalize(product.getBrand()).contains(typed)
                || MetinKarsilastirici.normalize(product.getDisplayName()).contains(typed);
    }

    static boolean sameText(String left, String right) {
        return MetinKarsilastirici.normalize(left).equals(MetinKarsilastirici.normalize(right));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
