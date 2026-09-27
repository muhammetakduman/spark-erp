package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Calendar view of attendance: one row per employee, one column per date,
 * each cell listing the jobs worked that day.
 */
public record AttendanceMatrix(List<LocalDate> dates, List<Row> rows) {

    public record Row(Long employeeId, String employeeName, Map<LocalDate, List<Cell>> cells) {
    }

    public record Cell(String jobShortName, BigDecimal dayFactor) {
    }
}
