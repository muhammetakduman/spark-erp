package com.electrician.tracker.dto;

import java.math.BigDecimal;

/**
 * "Pano kategorisi — toplam gider: 48.350,00 ₺ (12 alış)". {@code totalExpense}
 * is {@code null} for a user who may not see purchase prices.
 */
public record CategoryTotals(String category, BigDecimal totalExpense, int purchaseCount) {
}
