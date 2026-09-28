package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.electrician.tracker.domain.CurrencyCode;
import com.electrician.tracker.service.exception.ValidationException;

/**
 * The single place where a purchase price in dollars or euros becomes lira:
 * amount × rate, where the rate is the lira value of one unit of the currency
 * on the purchase day. Lira needs no rate (it is always 1). The lira unit
 * price keeps 4 decimals so that multiplying it by large quantities does not
 * lose kuruş; screens round it to 2.
 */
public final class CurrencyConverter {

    public static final BigDecimal LIRA_RATE = BigDecimal.ONE;
    private static final int TL_UNIT_PRICE_SCALE = 4;

    private CurrencyConverter() {
    }

    /** {@code null} currency counts as lira. */
    public static CurrencyCode orLira(CurrencyCode currency) {
        return currency == null ? CurrencyCode.TRY : currency;
    }

    /**
     * The rate that is stored with a line: 1 for lira, the given rate for a
     * foreign currency (which must be present and positive).
     */
    public static BigDecimal effectiveRate(CurrencyCode currency, BigDecimal rate) {
        if (!orLira(currency).isForeign()) {
            return LIRA_RATE;
        }
        validateRate(rate);
        return rate;
    }

    /** amount × rate in lira; {@code null} when there is no amount. */
    public static BigDecimal toLira(BigDecimal amount, CurrencyCode currency, BigDecimal rate) {
        if (amount == null) {
            return null;
        }
        return amount.multiply(effectiveRate(currency, rate)).setScale(TL_UNIT_PRICE_SCALE, RoundingMode.HALF_UP);
    }

    public static void validateRate(BigDecimal rate) {
        if (rate == null || rate.signum() <= 0) {
            throw new ValidationException("error.exchangeRate.required");
        }
    }
}
