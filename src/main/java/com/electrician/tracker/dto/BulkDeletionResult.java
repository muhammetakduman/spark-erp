package com.electrician.tracker.dto;

import java.util.List;

/**
 * Outcome of deleting many records at once: how many were deleted and which
 * were skipped because other records still point to them (they can be made
 * inactive instead). A bulk delete never stops half-way over one such record.
 */
public record BulkDeletionResult(int deletedCount, List<Skipped> skipped) {

    public static BulkDeletionResult allDeleted(int count) {
        return new BulkDeletionResult(count, List.of());
    }

    public List<Long> skippedIds() {
        return skipped.stream().map(Skipped::id).toList();
    }

    /** A record left in place: {@code reasonKey} with {@code count}, e.g. "12 malzeme kaleminde kullanılmış". */
    public record Skipped(Long id, String reasonKey, long count) {
    }
}
