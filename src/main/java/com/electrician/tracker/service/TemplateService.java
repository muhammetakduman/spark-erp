package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TemplateService(TemplateRepository templateRepository, ProductRepository productRepository, Clock clock) {
        this.templateRepository = templateRepository;
        this.productRepository = productRepository;
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
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("error.template.lines.required");
        }
        List<ItemSetLine> content = QuoteLineNumbering.normalize(lines).stream().map(ItemSetLine::of).toList();
        Template template = newTemplate(name, TemplateType.QUOTE_ITEM_SET, write(content));
        return toView(templateRepository.save(template));
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
     * skipped.
     */
    @Transactional(readOnly = true)
    public ItemSetApplication applyItemSet(Long id, List<QuoteLine> lines) {
        Template template = findEntity(id);
        if (template.getType() != TemplateType.QUOTE_ITEM_SET) {
            throw new ValidationException("error.template.type.invalid");
        }
        List<ItemSetLine> setLines = read(template.getContent());
        Map<Long, Product> products = productRepository.findAllById(setLines.stream()
                        .map(ItemSetLine::productId).filter(Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<QuoteLine> added = new ArrayList<>();
        int skipped = 0;
        for (ItemSetLine setLine : setLines) {
            if (setLine.productId() != null && !products.containsKey(setLine.productId())) {
                skipped++;
            } else {
                added.add(setLine.toQuoteLine(products.get(setLine.productId())));
            }
        }
        return new ItemSetApplication(QuoteLineNumbering.appendAll(lines, added), skipped);
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
                itemSet ? read(template.getContent()).size() : 0);
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
}
