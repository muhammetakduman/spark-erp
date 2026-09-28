package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.electrician.tracker.domain.CurrencyCode;

/** The last known lira value of one unit of {@code currency}, entered by hand (the program works offline). */
public record ExchangeRate(CurrencyCode currency, BigDecimal rate, LocalDate updatedOn) {
}
