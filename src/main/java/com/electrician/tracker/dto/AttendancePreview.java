package com.electrician.tracker.dto;

import java.math.BigDecimal;

/** Live preview of an attendance batch: "3 tarih × 2 kişi = 4,5 gün · 11.250 ₺". */
public record AttendancePreview(int dateCount, int personCount, BigDecimal dayCount, BigDecimal amount) {
}
