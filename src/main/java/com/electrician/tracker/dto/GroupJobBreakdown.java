package com.electrician.tracker.dto;

import java.util.List;
import java.util.Set;

/**
 * Where one material group's money went: a row per job (most spent first),
 * the total over every line and the products used in the group.
 * {@code groupName == null} is the "Kategorisiz" group.
 */
public record GroupJobBreakdown(String groupName, List<GroupJobRow> rows, MaterialGroupTotal total,
        Set<Long> productIds) {

    public int jobCount() {
        return rows.size();
    }

    public boolean hasFinancials() {
        return total.hasFinancials();
    }
}
