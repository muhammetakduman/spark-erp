package com.electrician.tracker.service.product;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.job.Job;
import com.electrician.tracker.domain.material.MaterialItem;
import com.electrician.tracker.dto.material.GroupJobBreakdown;
import com.electrician.tracker.dto.material.GroupJobRow;
import com.electrician.tracker.dto.product.ProductCatalog;
import com.electrician.tracker.dto.product.ProductOverview;
import com.electrician.tracker.repository.material.MaterialItemRepository;
import com.electrician.tracker.repository.product.ProductRepository;
import com.electrician.tracker.service.common.CanonicalNames;
import com.electrician.tracker.service.common.MetinKarsilastirici;
import com.electrician.tracker.service.material.MaterialGroupCalculator;
import com.electrician.tracker.service.user.AccessControl;
import com.electrician.tracker.service.user.FinancialDataMasker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The product screen's data: every product with its purchase statistics,
 * loaded with two queries (products, material lines with their product) and
 * aggregated in memory, plus the values the filters offer. A material
 * group's spending per job ("bu gruba hangi şantiyelerde ne kadar") groups by
 * the line's own group, not the product's category, so it matches the site
 * screen. A MANAGER receives no purchase prices, suppliers, expenses or
 * profits, and only the jobs they may open (services).
 */
@Service
public class ProductCatalogService {

    private final ProductRepository productRepository;
    private final MaterialItemRepository materialItemRepository;
    private final AccessControl accessControl;
    private final FinancialDataMasker masker;
    private final MaterialGroupCalculator groupCalculator = new MaterialGroupCalculator();

    public ProductCatalogService(ProductRepository productRepository, MaterialItemRepository materialItemRepository,
            AccessControl accessControl, FinancialDataMasker masker) {
        this.productRepository = productRepository;
        this.materialItemRepository = materialItemRepository;
        this.accessControl = accessControl;
        this.masker = masker;
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
                CanonicalNames.distinct(productRepository.findDistinctBrands()), suppliers,
                CanonicalNames.distinct(materialItemRepository.findDistinctGroupNames()),
                materialItemRepository.existsByGroupNameIsNull());
    }

    /**
     * One material group's spending per job between two optional dates (by
     * line date), most spent first; {@code groupName} blank is "Kategorisiz".
     */
    @Transactional(readOnly = true)
    public GroupJobBreakdown groupDistribution(String groupName, LocalDate from, LocalDate to) {
        String key = MetinKarsilastirici.normalize(groupName);
        List<MaterialItem> lines = materialItemRepository.findAllWithProductAndJob().stream()
                .filter(line -> MetinKarsilastirici.normalize(line.getGroupName()).equals(key))
                .filter(line -> accessControl.canAccess(line.getJob().getType()))
                .filter(line -> from == null || (line.getItemDate() != null && !line.getItemDate().isBefore(from)))
                .filter(line -> to == null || (line.getItemDate() != null && !line.getItemDate().isAfter(to)))
                .toList();
        Map<Job, List<MaterialItem>> byJob = lines.stream()
                .collect(Collectors.groupingBy(MaterialItem::getJob, LinkedHashMap::new, Collectors.toList()));
        List<GroupJobRow> rows = byJob.entrySet().stream()
                .map(entry -> new GroupJobRow(entry.getKey().getId(), entry.getKey().getType(),
                        entry.getKey().getCustomer().getName(), entry.getKey().getName(),
                        groupCalculator.total(entry.getValue())))
                .sorted(Comparator.comparing((GroupJobRow row) -> row.total().purchaseTotal()).reversed()
                        .thenComparing(row -> row.total().saleTotal(), Comparator.reverseOrder()))
                .toList();
        Set<Long> productIds = lines.stream().map(line -> line.getProduct().getId()).collect(Collectors.toSet());
        String name = groupName == null || groupName.isBlank() ? null : groupName.trim();
        return masker.mask(new GroupJobBreakdown(name, rows, groupCalculator.total(lines), productIds));
    }
}
