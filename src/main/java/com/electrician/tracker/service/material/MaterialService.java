package com.electrician.tracker.service.material;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.electrician.tracker.domain.material.MaterialItem;
import com.electrician.tracker.domain.product.CurrencyCode;
import com.electrician.tracker.domain.product.PriceEntryType;
import com.electrician.tracker.dto.common.BulkDeletionResult;
import com.electrician.tracker.dto.material.MaterialGroupBreakdown;
import com.electrician.tracker.repository.material.MaterialItemRepository;
import com.electrician.tracker.repository.product.ProductRepository;
import com.electrician.tracker.service.common.CanonicalNames;
import com.electrician.tracker.service.common.VatRules;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import com.electrician.tracker.service.product.CurrencyConverter;
import com.electrician.tracker.service.product.ExchangeRateService;
import com.electrician.tracker.service.user.AccessControl;
import com.electrician.tracker.service.user.FinancialDataMasker;
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
 * <p>
 * Each line has a group on its job ("Diafon"): left blank it takes the
 * product's category when saved, and it never follows later catalogue
 * changes. A typed variant of a group already in use keeps that spelling.
 */
@Service
public class MaterialService {

    private final MaterialItemRepository materialItemRepository;
    private final AccessControl accessControl;
    private final FinancialDataMasker masker;
    private final ExchangeRateService exchangeRateService;
    private final ProductRepository productRepository;
    private final MaterialGroupCalculator groupCalculator = new MaterialGroupCalculator();

    public MaterialService(MaterialItemRepository materialItemRepository, AccessControl accessControl,
            FinancialDataMasker masker, ExchangeRateService exchangeRateService,
            ProductRepository productRepository) {
        this.materialItemRepository = materialItemRepository;
        this.accessControl = accessControl;
        this.masker = masker;
        this.exchangeRateService = exchangeRateService;
        this.productRepository = productRepository;
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

    /**
     * Group suggestions for a line of {@code jobId} ({@code null} for a job not
     * saved yet): groups of this job first, then groups of other jobs, then
     * catalogue categories; each name once, with the spelling seen first.
     */
    @Transactional(readOnly = true)
    public List<String> findGroupSuggestions(Long jobId) {
        List<String> names = new ArrayList<>();
        if (jobId != null) {
            names.addAll(CanonicalNames.distinctInOrder(materialItemRepository.findGroupNamesByJobId(jobId)));
        }
        names.addAll(CanonicalNames.distinct(materialItemRepository.findDistinctGroupNames()));
        names.addAll(CanonicalNames.distinct(productRepository.findDistinctCategories()));
        return CanonicalNames.distinctInOrder(names);
    }

    /**
     * Already-loaded lines of one job by group, with cost and profit only for
     * users who may see them (no database access).
     */
    public MaterialGroupBreakdown groupBreakdown(List<MaterialItem> items) {
        return masker.mask(new MaterialGroupBreakdown(groupCalculator.group(items),
                MaterialGroupCalculator.bySpending(groupCalculator.totals(items)), groupCalculator.total(items)));
    }

    /**
     * "Gruba Taşı": puts every given line into one group in a single
     * transaction; a blank name gives each line its product's category back.
     */
    @Transactional
    public void moveToGroup(Collection<Long> ids, String groupName) {
        List<MaterialItem> lines = materialItemRepository.findWithProductAndJobByIdIn(ids.stream().distinct().toList());
        lines.forEach(line -> accessControl.requireAccess(line.getJob().getType()));
        List<String> knownGroups = materialItemRepository.findDistinctGroupNames();
        String typed = CanonicalNames.canonical(groupName, knownGroups);
        lines.forEach(line -> line.setGroupName(typed != null ? typed
                : CanonicalNames.canonical(line.getProduct().getCategory(), knownGroups)));
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
        existing.setGroupName(changes.getGroupName());
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
        item.setGroupName(groupOrCategory(item));
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

    /** The typed group in its known spelling, or the product's category when left blank. */
    private String groupOrCategory(MaterialItem item) {
        String typed = item.getGroupName();
        String name = typed == null || typed.isBlank() ? item.getProduct().getCategory() : typed;
        if (name == null || name.isBlank()) {
            return null;
        }
        return CanonicalNames.canonical(name, materialItemRepository.findDistinctGroupNames());
    }

    /** A new supplier is accepted as typed; a known one keeps the spelling already in use. */
    private String canonicalSupplierName(String typed) {
        if (typed == null || typed.isBlank()) {
            return null;
        }
        return CanonicalNames.canonical(typed, materialItemRepository.findDistinctSupplierNames());
    }

    /** Deletes the selected material lines of a site in one statement; ADMIN only. */
    @Transactional
    public BulkDeletionResult deleteAll(Collection<Long> ids) {
        accessControl.requireAdmin();
        List<Long> distinct = ids.stream().distinct().toList();
        materialItemRepository.deleteAllByIdInBatch(distinct);
        return BulkDeletionResult.allDeleted(distinct.size());
    }
}
