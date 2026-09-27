package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A single attendance day and its factor (1.0 or 0.5). */
public record WorkedDay(LocalDate date, BigDecimal dayFactor) {
}
