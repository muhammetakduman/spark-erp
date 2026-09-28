package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.dto.CategoryTotals;
import com.electrician.tracker.dto.ProductCatalog;
import com.electrician.tracker.dto.ProductOverview;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The product screen's data: every product with its purchase statistics,
 * loaded with two queries (products, material lines with their product) and
 * aggregated in memory, plus the values the filters offer. A MANAGER receives
 * no purchase prices, suppliers or expenses.
 */
@Service
public class ProductCatalogService {

    private final ProductRepository productRepository;
    private final MaterialItemRepository materialItemRepository;
    private final AccessControl accessControl;

    public ProductCatalogService(ProductRepository productRepository, MaterialItemRepository materialItemRepository,
            AccessControl accessControl) {
        this.productRepository = productRepository;
        this.materialItemRepository = materialItemRepository;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public ProductCatalog loadCatalog() {
        Map<Long, List<MaterialItem>> linesByProduct = materialItemRepository.findAllWithProduct().stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getId()));
        boolean withPurchaseInfo = accessControl.canViewFinancials();
        List<ProductOverview> products = productRepository.findAll().stream()
                .map(product -> PurchaseStatistics.overview(product,
                        linesByProduct.getOrDefault(product.getId(), List.of())))
                .map(overview -> withPurchaseInfo ? overview : overview.withoutPurchaseInfo())
                .toList();
        List<String> suppliers = withPurchaseInfo
                ? CanonicalNames.distinct(materialItemRepository.findDistinctSupplierNames()) : List.of();
        return new ProductCatalog(products, CanonicalNames.distinct(productRepository.findDistinctCategories()),
                CanonicalNames.distinct(productRepository.findDistinctBrands()), suppliers);
    }

    /** Total expense and purchase count of the given (already loaded) products of one category. */
    public CategoryTotals categoryTotals(String category, List<ProductOverview> products) {
        List<ProductOverview> inCategory = products.stream()
                .filter(product -> ProductFilter.sameText(category, product.product().getCategory()))
                .toList();
        int purchaseCount = inCategory.stream().mapToInt(ProductOverview::purchaseCount).sum();
        BigDecimal expense = accessControl.canViewFinancials()
                ? inCategory.stream().map(ProductOverview::totalExpense).filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;
        return new CategoryTotals(category, expense, purchaseCount);
    }
}
