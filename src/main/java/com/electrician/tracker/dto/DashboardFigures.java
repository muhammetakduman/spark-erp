package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.Job;

/**
 * Main-screen summary cards plus the "completed but not fully collected"
 * site list. {@code monthlyProfitEstimated} is set when a material line of
 * the month has no purchase price, so its cost is missing from the profit.
 */
public record DashboardFigures(
        long activeSiteCount,
        BigDecimal monthlyProfit,
        boolean monthlyProfitEstimated,
        BigDecimal pendingSiteReceivable,
        BigDecimal pendingServicePayment,
        List<Job> completedSitesWithBalance) {
}
