package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.CurrencyCode;
import com.electrician.tracker.dto.ExchangeRate;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Güncel kurlar": the last dollar and euro rates, typed in Settings or
 * remembered from the last foreign-currency purchase. New material lines
 * suggest them; saved lines keep their own rate forever. Nothing is fetched
 * from the internet.
 */
@Service
public class ExchangeRateService {

    static final String RATE_KEY_PREFIX = "exchange_rate.";
    static final String UPDATED_ON_SUFFIX = ".updated_on";

    private final SettingService settingService;
    private final AccessControl accessControl;
    private final Clock clock;

    public ExchangeRateService(SettingService settingService, AccessControl accessControl, Clock clock) {
        this.settingService = settingService;
        this.accessControl = accessControl;
        this.clock = clock;
    }

    /** Last rate of every foreign currency (dollar, euro) that has one. */
    @Transactional(readOnly = true)
    public List<ExchangeRate> findAll() {
        return Arrays.stream(CurrencyCode.values())
                .filter(CurrencyCode::isForeign)
                .map(this::find)
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ExchangeRate> find(CurrencyCode currency) {
        if (currency == null || !currency.isForeign()) {
            return Optional.empty();
        }
        return settingService.getValue(rateKey(currency))
                .filter(text -> !text.isBlank())
                .map(text -> new ExchangeRate(currency, new BigDecimal(text),
                        settingService.getValue(rateKey(currency) + UPDATED_ON_SUFFIX)
                                .filter(date -> !date.isBlank())
                                .map(LocalDate::parse)
                                .orElse(null)));
    }

    /** Typed in Settings → Güncel kurlar (ADMIN only). */
    @Transactional
    public ExchangeRate update(CurrencyCode currency, BigDecimal rate) {
        accessControl.requireAdmin();
        return remember(currency, rate);
    }

    /** Stores {@code rate} as the latest one for {@code currency}, dated today. */
    @Transactional
    public ExchangeRate remember(CurrencyCode currency, BigDecimal rate) {
        if (currency == null || !currency.isForeign()) {
            throw new ValidationException("error.exchangeRate.currency");
        }
        CurrencyConverter.validateRate(rate);
        LocalDate today = LocalDate.now(clock);
        settingService.setValue(rateKey(currency), rate.stripTrailingZeros().toPlainString());
        settingService.setValue(rateKey(currency) + UPDATED_ON_SUFFIX, today.toString());
        return new ExchangeRate(currency, rate, today);
    }

    private static String rateKey(CurrencyCode currency) {
        return RATE_KEY_PREFIX + currency.name();
    }
}
