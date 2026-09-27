package com.electrician.tracker.service;

import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.dto.ProductCreationResult;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.repository.ProductRepository;
import com.electrician.tracker.service.exception.DuplicateNameException;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final MaterialItemRepository materialItemRepository;

    public ProductService(ProductRepository productRepository, MaterialItemRepository materialItemRepository) {
        this.productRepository = productRepository;
        this.materialItemRepository = materialItemRepository;
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

    @Transactional
    public Product create(Product product) {
        validate(product);
        if (findByNormalizedName(product.getName()).isPresent()) {
            throw new DuplicateNameException("error.product.name.duplicate");
        }
        product.setName(product.getName().trim());
        return productRepository.save(product);
    }

    /**
     * Adds a product typed into a picker. If the catalog already holds a
     * product whose normalized name is identical, that one is returned
     * instead of creating a near-duplicate.
     */
    @Transactional
    public ProductCreationResult createOrReuse(String name, ProductUnit unit) {
        Product candidate = new Product(name, unit);
        validate(candidate);
        Optional<Product> existing = findByNormalizedName(name);
        if (existing.isPresent()) {
            return new ProductCreationResult(existing.get(), true);
        }
        candidate.setName(name.trim());
        return new ProductCreationResult(productRepository.save(candidate), false);
    }

    @Transactional
    public Product update(Long id, Product changes) {
        validate(changes);
        Product existing = findById(id);
        Optional<Product> sameName = findByNormalizedName(changes.getName());
        if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
            throw new DuplicateNameException("error.product.name.duplicate");
        }
        existing.setName(changes.getName().trim());
        existing.setUnit(changes.getUnit());
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        long materialCount = materialItemRepository.countByProductId(id);
        if (materialCount > 0) {
            throw new ReferencedEntityException("error.product.delete.hasMaterialItems", materialCount);
        }
        productRepository.deleteById(id);
    }

    /**
     * The catalog is small, so matching happens in memory; this avoids an
     * extra normalized-name column in the schema.
     */
    private Optional<Product> findByNormalizedName(String name) {
        String normalized = MetinKarsilastirici.normalize(name);
        return productRepository.findAll().stream()
                .filter(p -> MetinKarsilastirici.normalize(p.getName()).equals(normalized))
                .findFirst();
    }

    private void validate(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ValidationException("error.product.name.required");
        }
        if (product.getUnit() == null) {
            throw new ValidationException("error.product.unit.required");
        }
    }
}
