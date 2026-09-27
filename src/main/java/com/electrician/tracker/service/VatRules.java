package com.electrician.tracker.service;

import java.util.Set;

import com.electrician.tracker.service.exception.ValidationException;

/**
 * Validation shared by every priced field that can carry VAT (material
 * lines, service fee, labor fee): a rate is optional, but once given it must
 * be 1/10/20 and must say whether the price already includes it.
 */
final class VatRules {

    private static final Set<Integer> ALLOWED_RATES = Set.of(1, 10, 20);

    private VatRules() {
    }

    static void validate(Integer rate, Boolean included) {
        if (rate == null) {
            return;
        }
        if (!ALLOWED_RATES.contains(rate)) {
            throw new ValidationException("error.vat.rate.invalid");
        }
        if (included == null) {
            throw new ValidationException("error.vat.included.required");
        }
    }

    /** The "included" flag is meaningless without a rate, so it is dropped. */
    static Boolean includedOrNull(Integer rate, Boolean included) {
        return rate == null ? null : included;
    }
}
