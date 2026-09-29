package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.util.Set;

import com.electrician.tracker.domain.DiscountType;
import com.electrician.tracker.dto.QuoteDraft;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.service.exception.ValidationException;

/** Validation of a quote before it is saved. */
final class QuoteRules {

    private static final Set<Integer> ALLOWED_VAT_RATES = Set.of(1, 10, 20);
    private static final BigDecimal MAX_PERCENT = BigDecimal.valueOf(100);

    private QuoteRules() {
    }

    static void validate(QuoteDraft draft) {
        requireText(draft.quoteNo(), "error.quote.number.required");
        requireText(draft.companyName(), "error.quote.companyName.required");
        if (draft.quoteDate() == null) {
            throw new ValidationException("error.quote.date.required");
        }
        if (draft.validityDays() < 0) {
            throw new ValidationException("error.quote.validity.negative");
        }
        if (draft.status() == null) {
            throw new ValidationException("error.quote.status.required");
        }
        if (draft.vatRate() != null && !ALLOWED_VAT_RATES.contains(draft.vatRate())) {
            throw new ValidationException("error.vat.rate.invalid");
        }
        if (isNegative(draft.laborAmount())) {
            throw new ValidationException("error.quote.labor.negative");
        }
        validateDiscount(draft.discountType(), draft.discountValue());
        validateLengths(draft);
        if (draft.lines() == null || draft.lines().isEmpty()) {
            throw new ValidationException("error.quote.lines.required");
        }
        if (draft.lines().size() > QuoteLineNumbering.MAX_LINES) {
            throw new ValidationException("error.quote.lines.tooMany");
        }
        draft.lines().forEach(QuoteRules::validateLine);
    }

    static void validateLine(QuoteLine line) {
        if (line.productId() == null && (line.productName() == null || line.productName().isBlank())) {
            throw new ValidationException("error.quote.line.product.required");
        }
        if (line.quantity() == null || line.quantity().signum() <= 0) {
            throw new ValidationException("error.quote.line.quantity.mustBePositive");
        }
        if (line.unit() == null) {
            throw new ValidationException("error.product.unit.required");
        }
        if (line.unitPrice() == null) {
            throw new ValidationException("error.quote.line.price.required");
        }
        if (line.unitPrice().signum() < 0) {
            throw new ValidationException("error.quote.line.price.negative");
        }
        if (line.isFreeItem()) {
            requireMaxLength(line.productName(), QuoteFieldLimits.PRODUCT_NAME);
        }
        requireMaxLength(line.brand(), QuoteFieldLimits.BRAND);
        requireMaxLength(line.description(), QuoteFieldLimits.DESCRIPTION);
    }

    /** The editor already stops typing at these limits; this guards every other caller. */
    private static void validateLengths(QuoteDraft draft) {
        requireMaxLength(draft.companyName(), QuoteFieldLimits.COMPANY_NAME);
        requireMaxLength(draft.address(), QuoteFieldLimits.ADDRESS);
        requireMaxLength(draft.contactPerson(), QuoteFieldLimits.CONTACT_PERSON);
        requireMaxLength(draft.phone(), QuoteFieldLimits.PHONE);
        requireMaxLength(draft.fax(), QuoteFieldLimits.PHONE);
        requireMaxLength(draft.email(), QuoteFieldLimits.EMAIL);
        requireMaxLength(draft.subject(), QuoteFieldLimits.SUBJECT);
        requireMaxLength(draft.preparedByName(), QuoteFieldLimits.PREPARED_BY_NAME);
        requireMaxLength(draft.preparedByTitle(), QuoteFieldLimits.PREPARED_BY_TITLE);
        requireMaxLength(draft.notes(), QuoteFieldLimits.NOTES);
    }

    private static void validateDiscount(DiscountType type, BigDecimal value) {
        if (type == null || type == DiscountType.NONE) {
            return;
        }
        if (value == null || value.signum() < 0) {
            throw new ValidationException("error.quote.discount.invalid");
        }
        if (type == DiscountType.PERCENT && value.compareTo(MAX_PERCENT) > 0) {
            throw new ValidationException("error.quote.discount.percentTooHigh");
        }
    }

    private static void requireText(String value, String messageKey) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(messageKey);
        }
    }

    private static void requireMaxLength(String value, int maxLength) {
        if (value != null && value.trim().length() > maxLength) {
            throw new ValidationException("error.quote.text.tooLong");
        }
    }

    private static boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }
}
