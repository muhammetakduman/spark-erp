package com.electrician.tracker.service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.DiscountType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.Quote;
import com.electrician.tracker.domain.QuoteItem;
import com.electrician.tracker.domain.QuoteStatus;
import com.electrician.tracker.dto.BulkDeletionResult;
import com.electrician.tracker.dto.QuoteDraft;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.dto.QuoteLink;
import com.electrician.tracker.dto.QuoteRow;
import com.electrician.tracker.dto.QuoteTotals;
import com.electrician.tracker.dto.QuoteView;
import com.electrician.tracker.report.QuotePdfGenerator;
import com.electrician.tracker.repository.ProductRepository;
import com.electrician.tracker.repository.QuoteItemRepository;
import com.electrician.tracker.repository.QuoteRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Written price quotes ("Teklif Ver"): numbering per year, saving with the
 * lines in their numbered order, copying, status changes, the default note
 * template and the PDF. A quote may be given to someone who is not a customer
 * yet: the typed name is then added to the customer list on request (or
 * linked to the customer who already has that name). A free item typed into
 * a quote is added to the product catalog on save (or linked to the product
 * with the same name and brand), so its supplier and cost can be kept on the
 * product. Quotes only carry sale
 * prices, so both roles may prepare them; only an ADMIN deletes one (turning
 * one into a job is {@link QuoteConversionService}).
 */
@Service
public class QuoteService {

    public static final int DEFAULT_VALIDITY_DAYS = 15;
    public static final Integer DEFAULT_VAT_RATE = 20;
    private static final String LAST_PREPARED_BY_NAME = "quote.lastPreparedByName";
    private static final String LAST_PREPARED_BY_TITLE = "quote.lastPreparedByTitle";

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final CustomerService customerService;
    private final TemplateService templateService;
    private final CompanyService companyService;
    private final SettingService settingService;
    private final QuotePdfGenerator pdfGenerator;
    private final AccessControl accessControl;
    private final Clock clock;

    public QuoteService(QuoteRepository quoteRepository, QuoteItemRepository quoteItemRepository,
            ProductRepository productRepository,
            ProductService productService, CustomerService customerService, TemplateService templateService, CompanyService companyService, SettingService settingService,
            QuotePdfGenerator pdfGenerator, AccessControl accessControl, Clock clock) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.customerService = customerService;
        this.templateService = templateService;
        this.companyService = companyService;
        this.settingService = settingService;
        this.pdfGenerator = pdfGenerator;
        this.accessControl = accessControl;
        this.clock = clock;
    }

    /** Every quote with its grand total, newest first (two joins, one query). */
    @Transactional(readOnly = true)
    public List<QuoteRow> findAllRows() {
        LocalDate today = LocalDate.now(clock);
        return quoteRepository.findAllWithItems().stream().map(quote -> QuoteMapper.toRow(quote, today)).toList();
    }

    @Transactional(readOnly = true)
    public QuoteView findById(Long id) {
        return QuoteMapper.toView(findEntity(id));
    }

    /**
     * An unsaved quote for today: next number, 15 days validity, 20 % VAT, the
     * default note template (or {@code fallbackNotes} when there is none) and
     * the last used "Hazırlayan" / "Unvan" (the logged-in user the first time).
     */
    @Transactional(readOnly = true)
    public QuoteView newQuote(String fallbackNotes) {
        LocalDate today = LocalDate.now(clock);
        QuotePreparer preparer = QuotePreparer.resolve(settingService.getValue(LAST_PREPARED_BY_NAME).orElse(null),
                settingService.getValue(LAST_PREPARED_BY_TITLE).orElse(null), accessControl.currentUser().orElse(null));
        QuoteDraft draft = new QuoteDraft(suggestNumber(today), today, DEFAULT_VALIDITY_DAYS, null, null, null, null,
                null, null, null, null, DiscountType.NONE, null, null, DEFAULT_VAT_RATE,
                templateService.defaultQuoteNote().orElse(fallbackNotes), QuoteStatus.DRAFT, List.of(), false,
                preparer.name(), preparer.title());
        return new QuoteView(null, draft, preparer.name(), preparer.title(), null, null, QuoteMapper.totals(draft));
    }

    /** The next free number in the year of {@code date}. */
    @Transactional(readOnly = true)
    public String suggestNumber(LocalDate date) {
        int year = date.getYear();
        return QuoteNumbers.next(year, quoteRepository.findQuoteNosStartingWith(QuoteNumbers.yearPrefixPattern(year)));
    }

    /** Validates one line as the item dialog closes; returns it unchanged. */
    public QuoteLine checkLine(QuoteLine line) {
        QuoteRules.validateLine(line);
        return line;
    }

    /** Live totals for the editor while it is being filled in. */
    public QuoteTotals calculateTotals(QuoteDraft draft) {
        return QuoteMapper.totals(draft);
    }

    /** Creates ({@code id == null}) or updates a quote; lines are stored in their numbered order. */
    @Transactional
    public QuoteView save(Long id, QuoteDraft draft) {
        QuoteRules.validate(draft);
        if (QuoteMapper.totals(draft).subtotal().signum() < 0) {
            throw new ValidationException("error.quote.discount.tooHigh");
        }
        String quoteNo = draft.quoteNo().trim();
        boolean duplicate = id == null ? quoteRepository.existsByQuoteNo(quoteNo)
                : quoteRepository.existsByQuoteNoAndIdNot(quoteNo, id);
        if (duplicate) {
            throw new ValidationException("error.quote.number.duplicate");
        }
        Quote quote = id == null ? newEntity() : findEntity(id);
        apply(quote, draft, quoteNo);
        return QuoteMapper.toView(quoteRepository.save(quote));
    }

    /** A new draft with the same content, today's date and the next number. */
    @Transactional
    public QuoteView copy(Long id) {
        QuoteDraft source = QuoteMapper.toDraft(findEntity(id));
        LocalDate today = LocalDate.now(clock);
        QuoteDraft copy = new QuoteDraft(suggestNumber(today), today, source.validityDays(), source.customerId(),
                source.companyName(), source.address(), source.contactPerson(), source.phone(), source.fax(),
                source.email(), source.subject(), source.discountType(), source.discountValue(),
                source.laborAmount(), source.vatRate(), source.notes(), QuoteStatus.DRAFT, source.lines(), false,
                source.preparedByName(), source.preparedByTitle());
        return save(null, copy);
    }

    @Transactional
    public void changeStatus(Long id, QuoteStatus status) {
        if (status == null) {
            throw new ValidationException("error.quote.status.required");
        }
        findEntity(id).setStatus(status);
    }

    @Transactional
    public void delete(Long id) {
        accessControl.requireAdmin();
        quoteRepository.delete(findEntity(id));
    }

    @Transactional(readOnly = true)
    public Optional<QuoteLink> findLinkForJob(Long jobId) {
        return quoteRepository.findJobLink(jobId);
    }

    /** Job id → the quote it was created from, for every converted quote. */
    @Transactional(readOnly = true)
    public Map<Long, QuoteLink> findJobLinks() {
        return quoteRepository.findJobLinks().stream()
                .collect(Collectors.toMap(QuoteLink::jobId, Function.identity(), (first, second) -> first));
    }

    @Transactional(readOnly = true)
    public void exportPdf(Long id, Path outputFile) {
        pdfGenerator.generate(findById(id), companyService.get(), outputFile);
    }

    private Quote newEntity() {
        return new Quote(null, null, LocalDateTime.now(clock));
    }

    /** Blank fields take the logged-in user's name and title; the values are remembered for the next quote. */
    private void applyPreparer(Quote quote, QuoteDraft draft) {
        QuotePreparer preparer = QuotePreparer.resolve(draft.preparedByName(), draft.preparedByTitle(),
                accessControl.currentUser().orElse(null));
        quote.setPreparedBy(preparer.name(), preparer.title());
        settingService.setValue(LAST_PREPARED_BY_NAME, Objects.requireNonNullElse(preparer.name(), ""));
        settingService.setValue(LAST_PREPARED_BY_TITLE, Objects.requireNonNullElse(preparer.title(), ""));
    }

    private void apply(Quote quote, QuoteDraft draft, String quoteNo) {
        quote.setQuoteNo(quoteNo);
        quote.setQuoteDate(draft.quoteDate());
        quote.setValidityDays(draft.validityDays());
        quote.setCustomer(resolveCustomer(draft));
        quote.setCompanyName(draft.companyName().trim());
        quote.setAddress(blankToNull(draft.address()));
        quote.setContactPerson(blankToNull(draft.contactPerson()));
        quote.setPhone(blankToNull(draft.phone()));
        quote.setFax(blankToNull(draft.fax()));
        quote.setEmail(blankToNull(draft.email()));
        quote.setSubject(blankToNull(draft.subject()));
        DiscountType discountType = draft.discountType() == null ? DiscountType.NONE : draft.discountType();
        quote.setDiscount(discountType, discountType == DiscountType.NONE ? null : draft.discountValue());
        quote.setLaborAmount(draft.laborAmount());
        quote.setVatRate(draft.vatRate());
        quote.setNotes(draft.notes());
        quote.setStatus(draft.status());
        applyPreparer(quote, draft);
        quote.replaceItems(toItems(QuoteLineNumbering.normalize(draft.lines())));
    }

    /**
     * The chosen customer; or, when asked, the customer with the typed name
     * (created from the quote's address, phone and e-mail if there is none);
     * otherwise no customer record at all.
     */
    private Customer resolveCustomer(QuoteDraft draft) {
        if (draft.customerId() != null) {
            return customerService.findById(draft.customerId());
        }
        if (!draft.addCustomerToList()) {
            return null;
        }
        return customerService.findOrCreate(draft.companyName(), draft.phone(), draft.address(), draft.email())
                .customer();
    }

    private List<QuoteItem> toItems(List<QuoteLine> lines) {
        List<Long> productIds = lines.stream().map(QuoteLine::productId).filter(Objects::nonNull).toList();
        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<QuoteLine> freeLines = lines.stream().filter(QuoteLine::isFreeItem).toList();
        List<Product> freeProducts = productService.createOrReuseAll(freeLines.stream()
                .map(line -> new Product(line.productName(), line.unit(), blankToNull(line.brand()), null))
                .toList());
        Map<QuoteLine, Product> catalogOfFree = new IdentityHashMap<>();
        for (int index = 0; index < freeLines.size(); index++) {
            catalogOfFree.put(freeLines.get(index), freeProducts.get(index));
        }
        return lines.stream().map(line -> toItem(line,
                line.isFreeItem() ? catalogOfFree.get(line) : products.get(line.productId()))).toList();
    }

    /** The brand is the quote's own copy, so later catalog changes never alter a quote. */
    private static QuoteItem toItem(QuoteLine line, Product product) {
        if (product == null) {
            throw new NotFoundException("error.product.notFound");
        }
        return new QuoteItem(product, null, blankToNull(line.brand()), line.quantity(), line.unit(), line.unitPrice(),
                blankToNull(line.description()));
    }

    Quote findEntity(Long id) {
        return quoteRepository.findWithItemsById(id).orElseThrow(() -> new NotFoundException("error.quote.notFound"));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
    /** Lines of the selected quotes that go with them. */
    @Transactional(readOnly = true)
    public Map<String, Long> bulkDeletionImpact(Collection<Long> ids) {
        accessControl.requireAdmin();
        Map<String, Long> related = new LinkedHashMap<>();
        related.put("bulk.count.quoteLines", quoteItemRepository.countByQuoteIdIn(ids));
        return BulkDeletions.withoutZeros(related);
    }

    /** Deletes many quotes (and their lines, by the database) at once; ADMIN only. */
    @Transactional
    public BulkDeletionResult deleteAll(Collection<Long> ids) {
        accessControl.requireAdmin();
        List<Long> distinct = ids.stream().distinct().toList();
        quoteRepository.deleteAllByIdInBatch(distinct);
        return BulkDeletionResult.allDeleted(distinct.size());
    }
}
