package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An employee already recorded on another job for a date being entered.
 * {@code dayFactorTotal} is that employee's day-factor sum on the date if the
 * new row is added too.
 */
public record AttendanceConflict(String employeeName, LocalDate date, String jobLabel, BigDecimal dayFactorTotal) {

    /** More than one full day of work on a single date is suspicious (warned, not blocked). */
    public boolean exceedsFullDay() {
        return dayFactorTotal.compareTo(BigDecimal.ONE) > 0;
    }
}
