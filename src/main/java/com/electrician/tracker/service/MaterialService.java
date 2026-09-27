package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {

    private final MaterialItemRepository materialItemRepository;

    public MaterialService(MaterialItemRepository materialItemRepository) {
        this.materialItemRepository = materialItemRepository;
    }

    @Transactional(readOnly = true)
    public List<MaterialItem> findByJob(Long jobId) {
        return materialItemRepository.findByJobIdOrderByItemDateAsc(jobId);
    }

    @Transactional(readOnly = true)
    public List<String> findDistinctSupplierNames() {
        return SupplierNames.distinct(materialItemRepository.findDistinctSupplierNames());
    }

    @Transactional
    public MaterialItem addItem(MaterialItem item) {
        requireJob(item);
        prepare(item);
        return materialItemRepository.save(item);
    }

    /**
     * Validates and normalizes a line that cannot be saved yet because its
     * job does not exist (the service form creates the job and its materials
     * together); the caller attaches the job and calls {@link #addItem} later.
     */
    public MaterialItem prepareDraft(MaterialItem item) {
        prepare(item);
        return item;
    }

    public BigDecimal sumSaleTotals(List<MaterialItem> items) {
        return items.stream()
                .map(MaterialPriceCalculator::saleTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public MaterialItem update(Long id, MaterialItem changes) {
        requireJob(changes);
        prepare(changes);
        MaterialItem existing = materialItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.materialItem.notFound"));
        existing.setProduct(changes.getProduct());
        existing.setItemDate(changes.getItemDate());
        existing.setQuantity(changes.getQuantity());
        existing.setPurchaseUnitPrice(changes.getPurchaseUnitPrice());
        existing.setSupplierName(changes.getSupplierName());
        existing.setPriceEntryType(changes.getPriceEntryType());
        existing.setSaleUnitPrice(changes.getSaleUnitPrice());
        existing.setSaleTotalAmount(changes.getSaleTotalAmount());
        existing.setVatRate(changes.getVatRate());
        existing.setVatIncluded(changes.getVatIncluded());
        existing.setPurchaseVat(changes.getPurchaseVatRate(), changes.getPurchaseVatIncluded());
        existing.setNote(changes.getNote());
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        materialItemRepository.deleteById(id);
    }

    private void prepare(MaterialItem item) {
        validate(item);
        applyPriceEntry(item);
        item.setSupplierName(canonicalSupplierName(item.getSupplierName()));
        item.setVatIncluded(VatRules.includedOrNull(item.getVatRate(), item.getVatIncluded()));
        // Purchase VAT means nothing without a purchase price.
        Integer purchaseRate = item.getPurchaseUnitPrice() == null ? null : item.getPurchaseVatRate();
        item.setPurchaseVat(purchaseRate, VatRules.includedOrNull(purchaseRate, item.getPurchaseVatIncluded()));
    }

    /** A saved line belongs to a job and has a date (draft lines get both later). */
    private void requireJob(MaterialItem item) {
        if (item.getJob() == null) {
            throw new ValidationException("error.materialItem.job.required");
        }
        if (item.getItemDate() == null) {
            throw new ValidationException("error.materialItem.date.required");
        }
    }

    private void validate(MaterialItem item) {
        if (item.getProduct() == null) {
            throw new ValidationException("error.materialItem.product.required");
        }
        if (item.getQuantity() == null || item.getQuantity().signum() <= 0) {
            throw new ValidationException("error.materialItem.quantity.mustBePositive");
        }
        validateSalePrice(item);
        if (isNegative(item.getPurchaseUnitPrice())) {
            throw new ValidationException("error.materialItem.purchaseUnitPrice.negative");
        }
        VatRules.validate(item.getVatRate(), item.getVatIncluded());
        VatRules.validate(item.getPurchaseVatRate(), item.getPurchaseVatIncluded());
    }

    private void validateSalePrice(MaterialItem item) {
        if (item.getPriceEntryType() == PriceEntryType.TOTAL) {
            requireNonNegative(item.getSaleTotalAmount(), "error.materialItem.saleTotalAmount.required",
                    "error.materialItem.saleTotalAmount.negative");
        } else {
            requireNonNegative(item.getSaleUnitPrice(), "error.materialItem.saleUnitPrice.required",
                    "error.materialItem.saleUnitPrice.negative");
        }
    }

    private void requireNonNegative(BigDecimal value, String requiredKey, String negativeKey) {
        if (value == null) {
            throw new ValidationException(requiredKey);
        }
        if (isNegative(value)) {
            throw new ValidationException(negativeKey);
        }
    }

    /**
     * Keeps both price columns consistent with the entry type: a TOTAL line
     * stores its derived unit price for display, a UNIT line has no total.
     */
    private void applyPriceEntry(MaterialItem item) {
        if (item.getPriceEntryType() == PriceEntryType.TOTAL) {
            item.setSaleUnitPrice(MaterialPriceCalculator.unitPriceFromTotal(
                    item.getSaleTotalAmount(), item.getQuantity()));
        } else {
            item.setPriceEntryType(PriceEntryType.UNIT);
            item.setSaleTotalAmount(null);
        }
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    /** A new supplier is accepted as typed; a known one keeps the spelling already in use. */
    private String canonicalSupplierName(String typed) {
        if (typed == null || typed.isBlank()) {
            return null;
        }
        return SupplierNames.canonical(typed, materialItemRepository.findDistinctSupplierNames());
    }
}
