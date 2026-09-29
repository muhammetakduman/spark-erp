package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.domain.Template;
import com.electrician.tracker.domain.TemplateType;
import com.electrician.tracker.dto.BulkDeletionResult;
import com.electrician.tracker.dto.ItemSetApplication;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.dto.TemplateView;
import com.electrician.tracker.repository.ProductRepository;
import com.electrician.tracker.repository.TemplateRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Templates ("Şablonlar"): quote notes (one may be the default of new
 * quotes), quote item sets ("Standart daire tesisatı" → 12 lines at once) and
 * daily job titles. An item set is added below a quote's existing lines with
 * the numbering continuing; lines whose product was deleted since are left
 * out and counted.
 */
@Service
public class TemplateService {

    private static final TypeReference<List<ItemSetLine>> ITEM_SET_TYPE = new TypeReference<>() {
    };

    private final TemplateRepository templateRepository;
    private final ProductRepository productRepository;
    private final AccessControl accessControl;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TemplateService(TemplateRepository templateRepository, ProductRepository productRepository,
            AccessControl accessControl, Clock clock) {
        this.templateRepository = templateRepository;
        this.productRepository = productRepository;
        this.accessControl = accessControl;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TemplateView> findAll() {
        return templateRepository.findAllByOrderByTypeAscNameAsc().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateView> findByType(TemplateType type) {
        return templateRepository.findByTypeOrderByNameAsc(type).stream().map(this::toView).toList();
    }

    /** Text of the default quote note, used by new quotes. */
    @Transactional(readOnly = true)
    public Optional<String> defaultQuoteNote() {
        return templateRepository.findFirstByTypeAndDefaultTemplateTrue(TemplateType.QUOTE_NOTE)
                .map(Template::getContent);
    }

    /** Creates ({@code id == null}) or changes a text template (quote note or job title). */
    @Transactional
    public TemplateView saveText(Long id, String name, TemplateType type, String content) {
        if (type == null || !type.isText()) {
            throw new ValidationException("error.template.type.invalid");
        }
        requireText(content, "error.template.content.required");
        if (type == TemplateType.QUOTE_NOTE && content.trim().length() > QuoteFieldLimits.NOTES) {
            throw new ValidationException("error.quote.text.tooLong");
        }
        Template template = id == null ? newTemplate(name, type, content.trim()) : findEntity(id);
        rename(template, name);
        template.setContent(content.trim());
        return toView(templateRepository.save(template));
    }

    /** Saves a quote's lines (in their order) as a new item set. */
    @Transactional
    public TemplateView saveItemSet(String name, List<QuoteLine> lines) {
        Template template = newTemplate(name, TemplateType.QUOTE_ITEM_SET, write(itemSetContent(lines)));
        return toView(templateRepository.save(template));
    }

    /** Changes the name and lines of an item set ("Hazır Malzeme Listesi" editor). */
    @Transactional
    public TemplateView updateItemSet(Long id, String name, List<QuoteLine> lines) {
        Template template = findItemSet(id);
        rename(template, name);
        template.setContent(write(itemSetContent(lines)));
        return toView(template);
    }

    /** The lines of an item set as quote lines, numbered from 1 (catalog lines take the current product name). */
    @Transactional(readOnly = true)
    public List<QuoteLine> itemSetLines(Long id) {
        List<ItemSetLine> setLines = read(findItemSet(id).getContent());
        Map<Long, Product> products = productsOf(setLines);
        return QuoteLineNumbering.appendAll(List.of(), setLines.stream()
                .map(line -> line.toQuoteLine(products.get(line.productId()))).toList());
    }

    /** One more use of a template (e.g. quote terms picked in a quote). */
    @Transactional
    public void markUsed(Long id) {
        findEntity(id).markUsed(LocalDateTime.now(clock));
    }

    /** A daily job titled like a "Hazır İş Tanımı" counts as a use of it. */
    @Transactional
    public void recordJobDescriptionUse(String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String wanted = MetinKarsilastirici.normalize(title);
        templateRepository.findByTypeOrderByNameAsc(TemplateType.JOB_DESCRIPTION).stream()
                .filter(template -> MetinKarsilastirici.normalize(template.getContent()).equals(wanted))
                .forEach(template -> template.markUsed(now));
    }

    private static List<ItemSetLine> itemSetContent(List<QuoteLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("error.template.lines.required");
        }
        return QuoteLineNumbering.normalize(lines).stream().map(ItemSetLine::of).toList();
    }

    @Transactional
    public TemplateView renameTemplate(Long id, String name) {
        Template template = findEntity(id);
        rename(template, name);
        return toView(template);
    }

    /** Makes this the only default of its type, or removes the mark when {@code isDefault} is false. */
    @Transactional
    public void setDefault(Long id, boolean isDefault) {
        Template template = findEntity(id);
        if (isDefault) {
            templateRepository.findByTypeOrderByNameAsc(template.getType())
                    .forEach(other -> other.setDefaultTemplate(false));
        }
        template.setDefaultTemplate(isDefault);
    }

    @Transactional
    public void delete(Long id) {
        templateRepository.delete(findEntity(id));
    }

    /**
     * {@code lines} with the set's lines added below them (numbering goes on
     * from the last line). Catalog lines whose product no longer exists are
     * skipped. Counts as a use of the set.
     */
    @Transactional
    public ItemSetApplication applyItemSet(Long id, List<QuoteLine> lines) {
        Template template = findItemSet(id);
        List<ItemSetLine> setLines = read(template.getContent());
        Map<Long, Product> products = productsOf(setLines);
        List<QuoteLine> added = new ArrayList<>();
        int skipped = 0;
        for (ItemSetLine setLine : setLines) {
            if (setLine.productId() != null && !products.containsKey(setLine.productId())) {
                skipped++;
            } else {
                added.add(setLine.toQuoteLine(products.get(setLine.productId())));
            }
        }
        template.markUsed(LocalDateTime.now(clock));
        return new ItemSetApplication(QuoteLineNumbering.appendAll(lines, added), skipped);
    }

    private Map<Long, Product> productsOf(List<ItemSetLine> setLines) {
        return productRepository.findAllById(setLines.stream()
                        .map(ItemSetLine::productId).filter(Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Template findItemSet(Long id) {
        Template template = findEntity(id);
        if (template.getType() != TemplateType.QUOTE_ITEM_SET) {
            throw new ValidationException("error.template.type.invalid");
        }
        return template;
    }

    private Template newTemplate(String name, TemplateType type, String content) {
        return new Template(requireName(name), type, content, LocalDateTime.now(clock));
    }

    private void rename(Template template, String name) {
        String trimmed = requireName(name);
        boolean duplicate = template.getId() == null
                ? templateRepository.existsByTypeAndNameIgnoreCase(template.getType(), trimmed)
                : templateRepository.existsByTypeAndNameIgnoreCaseAndIdNot(template.getType(), trimmed,
                        template.getId());
        if (duplicate) {
            throw new ValidationException("error.template.name.duplicate");
        }
        template.setName(trimmed);
    }

    private static String requireName(String name) {
        requireText(name, "error.template.name.required");
        return name.trim();
    }

    private static void requireText(String text, String messageKey) {
        if (text == null || text.isBlank()) {
            throw new ValidationException(messageKey);
        }
    }

    private Template findEntity(Long id) {
        return templateRepository.findById(id).orElseThrow(() -> new NotFoundException("error.template.notFound"));
    }

    private TemplateView toView(Template template) {
        boolean itemSet = template.getType() == TemplateType.QUOTE_ITEM_SET;
        return new TemplateView(template.getId(), template.getName(), template.getType(),
                itemSet ? "" : template.getContent(), template.isDefaultTemplate(),
                itemSet ? read(template.getContent()).size() : 0, template.getUseCount(), template.getLastUsedAt());
    }

    private String write(List<ItemSetLine> lines) {
        try {
            return objectMapper.writeValueAsString(lines);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not store item set", e);
        }
    }

    private List<ItemSetLine> read(String json) {
        try {
            return objectMapper.readValue(json, ITEM_SET_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored item set is not readable", e);
        }
    }

    /** One stored line of an item set (JSON). */
    record ItemSetLine(Long productId, String productName, String brand, BigDecimal quantity, ProductUnit unit,
            BigDecimal unitPrice, String description) {

        static ItemSetLine of(QuoteLine line) {
            return new ItemSetLine(line.productId(), line.productName(), line.brand(), line.quantity(), line.unit(),
                    line.unitPrice(), line.description());
        }

        /** A catalog line takes the product's current name; the brand is the template's copy. */
        QuoteLine toQuoteLine(Product product) {
            String name = product == null ? productName : product.getName();
            return QuoteLine.unnumbered(productId, name, brand, quantity, unit, unitPrice, description);
        }
    }
    /** Deletes the selected templates at once; ADMIN only. */
    @Transactional
    public BulkDeletionResult deleteAll(Collection<Long> ids) {
        accessControl.requireAdmin();
        List<Long> distinct = ids.stream().distinct().toList();
        templateRepository.deleteAllByIdInBatch(distinct);
        return BulkDeletionResult.allDeleted(distinct.size());
    }
}
