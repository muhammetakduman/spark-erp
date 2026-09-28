package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.electrician.tracker.domain.CurrencyCode;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Material lines of a job. A MANAGER enters quantity and sale price only: a
 * line they add is stored without purchase price or supplier (the ADMIN adds
 * them later), and editing a line never touches its purchase data. Only an
 * ADMIN deletes lines or touches a site's lines.
 * <p>
 * A purchase price may be entered in dollars or euros with the day's rate;
 * its lira value is calculated once here and stored with the line, and the
 * rate becomes the suggestion for the next purchase in that currency.
 */
@Service
public class MaterialService {

    private final MaterialItemRepository materialItemRepository;
    private final AccessControl accessControl;
    private final FinancialDataMasker masker;
    private final ExchangeRateService exchangeRateService;

    public MaterialService(MaterialItemRepository materialItemRepository, AccessControl accessControl,
            FinancialDataMasker masker, ExchangeRateService exchangeRateService) {
        this.materialItemRepository = materialItemRepository;
        this.accessControl = accessControl;
        this.masker = masker;
        this.exchangeRateService = exchangeRateService;
    }

    @Transactional(readOnly = true)
    public List<MaterialItem> findByJob(Long jobId) {
        return masker.maskMaterials(materialItemRepository.findByJobIdOrderByItemDateAsc(jobId));
    }

    /** Ids of a job's saved lines (no prices, so nothing to mask). */
    @Transactional(readOnly = true)
    public List<Long> findLineIds(Long jobId) {
        return materialItemRepository.findByJobIdOrderByItemDateAsc(jobId).stream()
                .map(MaterialItem::getId)
                .toList();
    }

    /** Suppliers used before, for the supplier picker; empty for users who may not see suppliers. */
    @Transactional(readOnly = true)
    public List<String> findDistinctSupplierNames() {
        if (!accessControl.canViewFinancials()) {
            return List.of();
        }
        return CanonicalNames.distinct(materialItemRepository.findDistinctSupplierNames());
    }

    @Transactional
    public MaterialItem addItem(MaterialItem item) {
        requireJob(item);
        accessControl.requireAccess(item.getJob().getType());
        dropPurchaseInfoUnlessAllowed(item);
        prepare(item);
        MaterialItem saved = materialItemRepository.save(item);
        rememberExchangeRate(saved);
        return saved;
    }

    /**
     * Validates and normalizes a line that cannot be saved yet because its
     * job does not exist (the service form creates the job and its materials
     * together); the caller attaches the job and calls {@link #addItem} later.
     */
    public MaterialItem prepareDraft(MaterialItem item) {
        dropPurchaseInfoUnlessAllowed(item);
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
        MaterialItem existing = materialItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.materialItem.notFound"));
        accessControl.requireAccess(existing.getJob().getType());
        if (!accessControl.canViewFinancials()) {
            keepPurchaseInfo(existing, changes);
        }
        prepare(changes);
        existing.setProduct(changes.getProduct());
        existing.setItemDate(changes.getItemDate());
        existing.setQuantity(changes.getQuantity());
        existing.setPurchaseUnitPrice(changes.getPurchaseUnitPrice());
        existing.setPurchaseCurrency(changes.getPurchaseCurrency(), changes.getPurchaseExchangeRate(),
                changes.getPurchaseUnitPriceTl());
        existing.setSupplierName(changes.getSupplierName());
        existing.setPriceEntryType(changes.getPriceEntryType());
        existing.setSaleUnitPrice(changes.getSaleUnitPrice());
        existing.setSaleTotalAmount(changes.getSaleTotalAmount());
        existing.setVatRate(changes.getVatRate());
        existing.setVatIncluded(changes.getVatIncluded());
        existing.setPurchaseVat(changes.getPurchaseVatRate(), changes.getPurchaseVatIncluded());
        existing.setNote(changes.getNote());
        rememberExchangeRate(existing);
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        accessControl.requireAdmin();
        materialItemRepository.deleteById(id);
    }

    private void dropPurchaseInfoUnlessAllowed(MaterialItem item) {
        if (!accessControl.canViewFinancials()) {
            item.hidePurchaseInfo();
        }
    }

    /** A user who cannot see purchase data must not overwrite it either. */
    private static void keepPurchaseInfo(MaterialItem existing, MaterialItem changes) {
        changes.setPurchaseUnitPrice(existing.getPurchaseUnitPrice());
        changes.setPurchaseCurrency(existing.getPurchaseCurrency(), existing.getPurchaseExchangeRate(),
                existing.getPurchaseUnitPriceTl());
        changes.setSupplierName(existing.getSupplierName());
        changes.setPurchaseVat(existing.getPurchaseVatRate(), existing.getPurchaseVatIncluded());
    }

    private void prepare(MaterialItem item) {
        validate(item);
        applyPriceEntry(item);
        applyPurchaseCurrency(item);
        item.setSupplierName(canonicalSupplierName(item.getSupplierName()));
        item.setVatIncluded(VatRules.includedOrNull(item.getVatRate(), item.getVatIncluded()));
        // Purchase VAT means nothing without a purchase price.
        Integer purchaseRate = item.getPurchaseUnitPrice() == null ? null : item.getPurchaseVatRate();
        item.setPurchaseVat(purchaseRate, VatRules.includedOrNull(purchaseRate, item.getPurchaseVatIncluded()));
    }

    /** Freezes the lira value of the purchase price with the line's own rate (1 for lira). */
    private static void applyPurchaseCurrency(MaterialItem item) {
        CurrencyCode currency = CurrencyConverter.orLira(item.getPurchaseCurrency());
        if (item.getPurchaseUnitPrice() == null) {
            item.setPurchaseCurrency(CurrencyCode.TRY, CurrencyConverter.LIRA_RATE, null);
            return;
        }
        BigDecimal rate = CurrencyConverter.effectiveRate(currency, item.getPurchaseExchangeRate());
        item.setPurchaseCurrency(currency, rate,
                CurrencyConverter.toLira(item.getPurchaseUnitPrice(), currency, rate));
    }

    private void rememberExchangeRate(MaterialItem item) {
        if (item.isForeignCurrencyPurchase() && accessControl.canViewFinancials()) {
            exchangeRateService.remember(item.getPurchaseCurrency(), item.getPurchaseExchangeRate());
        }
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
        return CanonicalNames.canonical(typed, materialItemRepository.findDistinctSupplierNames());
    }
}
