package com.electrician.tracker.dto;

import java.math.BigDecimal;

/**
 * One selected employee in an attendance batch: full-day wage and day factor
 * (1.0 full day, 0.5 half day) applied to every selected date.
 */
public record AttendanceEntry(Long employeeId, BigDecimal dailyWage, BigDecimal dayFactor) {

    public AttendanceEntry(Long employeeId, BigDecimal dailyWage) {
        this(employeeId, dailyWage, BigDecimal.ONE);
    }
}
