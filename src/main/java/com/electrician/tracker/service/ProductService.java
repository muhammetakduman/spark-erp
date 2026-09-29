package com.electrician.tracker.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.dto.BulkDeletionResult;
import com.electrician.tracker.dto.IdCount;
import com.electrician.tracker.dto.ProductCreationResult;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.repository.ProductRepository;
import com.electrician.tracker.repository.QuoteItemRepository;
import com.electrician.tracker.service.exception.DuplicateNameException;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The product catalog. A product is unique by name and brand together,
 * compared without case, Turkish letters or extra spaces, so "ÖZNUR 1,5mm NYA"
 * and "HES 1,5mm NYA" are two products while "öznur" and "ÖZNUR" are the same
 * brand. Brands and categories are free text; a variant of a known one is
 * stored with the spelling already in use. The usual supplier and purchase
 * cost are only written by users who may see purchase prices.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final MaterialItemRepository materialItemRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final AccessControl accessControl;

    public ProductService(ProductRepository productRepository, MaterialItemRepository materialItemRepository,
            QuoteItemRepository quoteItemRepository, AccessControl accessControl) {
        this.productRepository = productRepository;
        this.materialItemRepository = materialItemRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.product.notFound"));
    }

    /** Brands used so far, once each, for the brand suggestions. */
    @Transactional(readOnly = true)
    public List<String> findDistinctBrands() {
        return CanonicalNames.distinct(productRepository.findDistinctBrands());
    }

    /** Categories used so far, once each, for the category suggestions and filters. */
    @Transactional(readOnly = true)
    public List<String> findDistinctCategories() {
        return CanonicalNames.distinct(productRepository.findDistinctCategories());
    }

    @Transactional
    public Product create(Product product) {
        normalize(product);
        if (!accessControl.canViewFinancials()) {
            product.setSupplierName(null);
            product.setPurchasePrice(null);
        }
        if (findSame(product.getName(), product.getBrand()).isPresent()) {
            throw new DuplicateNameException("error.product.name.duplicate");
        }
        return productRepository.save(product);
    }

    /**
     * Adds a product typed into a picker. If the catalog already holds the
     * same name with the same brand (normalized), that one is returned
     * instead of creating a near-duplicate.
     */
    @Transactional
    public ProductCreationResult createOrReuse(String name, String brand, String category, ProductUnit unit) {
        Product candidate = new Product(name, unit, brand, category);
        normalize(candidate);
        Optional<Product> existing = findSame(candidate.getName(), candidate.getBrand());
        if (existing.isPresent()) {
            return new ProductCreationResult(existing.get(), true);
        }
        return new ProductCreationResult(productRepository.save(candidate), false);
    }

    /**
     * The catalog product for each of {@code candidates} (same order): an
     * existing one with the same name and brand, else the candidate saved as
     * new. The catalog is read once for the whole list.
     */
    @Transactional
    public List<Product> createOrReuseAll(List<Product> candidates) {
        List<String> brands = new ArrayList<>(productRepository.findDistinctBrands());
        List<String> categories = new ArrayList<>(productRepository.findDistinctCategories());
        Map<String, Product> byKey = new HashMap<>();
        productRepository.findAll().forEach(product ->
                byKey.putIfAbsent(identityKey(product.getName(), product.getBrand()), product));
        List<Product> result = new ArrayList<>();
        for (Product candidate : candidates) {
            normalize(candidate, brands, categories);
            Product product = byKey.computeIfAbsent(identityKey(candidate.getName(), candidate.getBrand()),
                    key -> productRepository.save(candidate));
            addIfAbsent(brands, product.getBrand());
            addIfAbsent(categories, product.getCategory());
            result.add(product);
        }
        return result;
    }

    @Transactional
    public Product update(Long id, Product changes) {
        normalize(changes);
        Product existing = findById(id);
        Optional<Product> same = findSame(changes.getName(), changes.getBrand());
        if (same.isPresent() && !same.get().getId().equals(id)) {
            throw new DuplicateNameException("error.product.name.duplicate");
        }
        existing.setName(changes.getName());
        existing.setUnit(changes.getUnit());
        existing.setBrand(changes.getBrand());
        existing.setCategory(changes.getCategory());
        existing.setActive(changes.isActive());
        if (accessControl.canViewFinancials()) {
            existing.setSupplierName(changes.getSupplierName());
            existing.setPurchasePrice(changes.getPurchasePrice());
        }
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        long materialCount = materialItemRepository.countByProductId(id);
        if (materialCount > 0) {
            throw new ReferencedEntityException("error.product.delete.hasMaterialItems", materialCount);
        }
        long quoteLineCount = quoteItemRepository.countByProductId(id);
        if (quoteLineCount > 0) {
            throw new ReferencedEntityException("error.product.delete.hasQuoteItems", quoteLineCount);
        }
        productRepository.deleteById(id);
    }

    private void normalize(Product product) {
        normalize(product, productRepository.findDistinctBrands(), productRepository.findDistinctCategories());
    }

    /**
     * Validates, trims the name and supplier, and writes brand/category in
     * their known spelling (from {@code brands} and {@code categories}).
     */
    private static void normalize(Product product, List<String> brands, List<String> categories) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ValidationException("error.product.name.required");
        }
        if (product.getUnit() == null) {
            throw new ValidationException("error.product.unit.required");
        }
        if (product.getPurchasePrice() != null && product.getPurchasePrice().signum() < 0) {
            throw new ValidationException("error.product.purchasePrice.negative");
        }
        product.setName(product.getName().trim().replaceAll("\\s+", " "));
        product.setBrand(CanonicalNames.canonical(product.getBrand(), brands));
        product.setCategory(CanonicalNames.canonical(product.getCategory(), categories));
        String supplier = product.getSupplierName();
        product.setSupplierName(supplier == null || supplier.isBlank() ? null : supplier.trim());
    }

    private static void addIfAbsent(List<String> names, String name) {
        if (name != null && !names.contains(name)) {
            names.add(name);
        }
    }

    /**
     * The catalog is small, so matching happens in memory; this avoids an
     * extra normalized-name column in the schema.
     */
    private Optional<Product> findSame(String name, String brand) {
        String key = identityKey(name, brand);
        return productRepository.findAll().stream()
                .filter(product -> identityKey(product.getName(), product.getBrand()).equals(key))
                .findFirst();
    }

    static String identityKey(String name, String brand) {
        return MetinKarsilastirici.normalize(name) + "|" + MetinKarsilastirici.normalize(brand);
    }
    /** Products offered in pickers (inactive ones are left out). */
    @Transactional(readOnly = true)
    public List<Product> findAllActive() {
        return productRepository.findAll().stream().filter(Product::isActive).toList();
    }

    /**
     * Deletes the selected products that nothing uses; a product still on a
     * material line or quote line is skipped (it can be made inactive).
     */
    @Transactional
    public BulkDeletionResult deleteAll(Collection<Long> ids) {
        accessControl.requireAdmin();
        Map<String, List<IdCount>> references = new LinkedHashMap<>();
        references.put("error.product.delete.hasMaterialItems", materialItemRepository.countByProductIds(ids));
        references.put("error.product.delete.hasQuoteItems", quoteItemRepository.countByProductIds(ids));
        return BulkDeletions.deleteUnreferenced(ids, references, productRepository::deleteAllByIdInBatch);
    }

    /** "Seçilenleri pasife al": the products stay but are no longer offered in pickers. */
    @Transactional
    public void deactivateAll(Collection<Long> ids) {
        accessControl.requireAdmin();
        productRepository.findAllById(ids).forEach(product -> product.setActive(false));
    }
}
