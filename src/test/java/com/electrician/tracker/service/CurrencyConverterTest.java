package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.electrician.tracker.domain.CurrencyCode;
import org.junit.jupiter.api.Test;

class CurrencyConverterTest {

    @Test
    void dollarsTimesRateGiveLira() {
        assertThat(CurrencyConverter.toLira(new BigDecimal("120"), CurrencyCode.USD, new BigDecimal("34.25")))
                .isEqualByComparingTo("4110.00");
    }

    @Test
    void liraNeedsNoRate() {
        assertThat(CurrencyConverter.toLira(new BigDecimal("12.5"), CurrencyCode.TRY, null))
                .isEqualByComparingTo("12.5");
        assertThat(CurrencyConverter.effectiveRate(null, new BigDecimal("40"))).isEqualByComparingTo("1");
    }

    @Test
    void foreignCurrencyRequiresPositiveRate() {
        assertThatThrownBy(() -> CurrencyConverter.toLira(BigDecimal.ONE, CurrencyCode.EUR, null))
                .hasMessage("error.exchangeRate.required");
        assertThatThrownBy(() -> CurrencyConverter.toLira(BigDecimal.ONE, CurrencyCode.EUR, BigDecimal.ZERO))
                .hasMessage("error.exchangeRate.required");
    }

    @Test
    void keepsKurusForSmallUnitPrices() {
        assertThat(CurrencyConverter.toLira(new BigDecimal("0.35"), CurrencyCode.USD, new BigDecimal("34.25")))
                .isEqualByComparingTo("11.9875");
    }
}
