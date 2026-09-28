package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.Job;

/**
 * Main-screen summary cards, the "completed but not fully collected" site
 * list and the latest activities. {@code monthlyProfitEstimated} is set when
 * a material line of the month has no purchase price, so its cost is missing
 * from the profit. Money figures are {@code null} for a user who may not see
 * them ({@link #withoutFinancials()}).
 */
public record DashboardFigures(
        long activeSiteCount,
        BigDecimal monthlyRevenue,
        BigDecimal monthlyProfit,
        boolean monthlyProfitEstimated,
        BigDecimal pendingSiteReceivable,
        BigDecimal pendingServicePayment,
        List<Job> completedSitesWithBalance,
        List<RecentActivity> recentActivities) {

    /**
     * Profit, receivables and the unpaid-site list removed; revenue stays. The
     * activities must already have been collected without payments.
     */
    public DashboardFigures withoutFinancials() {
        return new DashboardFigures(activeSiteCount, monthlyRevenue, null, false, null, null, List.of(),
                recentActivities);
    }
}
