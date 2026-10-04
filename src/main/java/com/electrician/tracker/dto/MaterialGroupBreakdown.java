package com.electrician.tracker.dto;

import java.util.List;

/**
 * A job's material lines by group: {@code groups} for the grouped list (A–Z
 * in Turkish, "Kategorisiz" last), {@code summary} for the group table (most
 * spent first, "Kategorisiz" last) and {@code total} over every line, equal
 * to the job's material totals.
 */
public record MaterialGroupBreakdown(List<MaterialGroup> groups, List<MaterialGroupTotal> summary,
        MaterialGroupTotal total) {

    public boolean hasFinancials() {
        return total.hasFinancials();
    }
}
