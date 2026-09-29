package com.electrician.tracker.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.electrician.tracker.dto.BulkDeletionResult;
import com.electrician.tracker.dto.IdCount;

/**
 * The shared rule of bulk deletes for records other records may point to:
 * every id is checked on its own against grouped counts (one query per kind
 * of reference, never one per record); the free ones are deleted together,
 * the referenced ones are skipped with the first reason found.
 */
final class BulkDeletions {

    private BulkDeletions() {
    }

    /**
     * @param references reason message key → counts per id, in the order the
     *                   reasons should be reported
     */
    static BulkDeletionResult deleteUnreferenced(Collection<Long> ids, Map<String, List<IdCount>> references,
            Consumer<List<Long>> deleteInBatch) {
        Map<Long, BulkDeletionResult.Skipped> skipped = new LinkedHashMap<>();
        references.forEach((reasonKey, counts) -> {
            Map<Long, Long> byId = toMap(counts);
            ids.stream().filter(id -> byId.getOrDefault(id, 0L) > 0).forEach(id ->
                    skipped.putIfAbsent(id, new BulkDeletionResult.Skipped(id, reasonKey, byId.get(id))));
        });
        List<Long> free = new ArrayList<>(ids.stream().distinct().filter(id -> !skipped.containsKey(id)).toList());
        if (!free.isEmpty()) {
            deleteInBatch.accept(free);
        }
        return new BulkDeletionResult(free.size(), List.copyOf(skipped.values()));
    }

    private static Map<Long, Long> toMap(List<IdCount> counts) {
        Map<Long, Long> byId = new HashMap<>();
        counts.forEach(count -> byId.put(count.id(), count.count()));
        return byId;
    }

    /** "Bu işlemle birlikte silinecekler": the related counts in their given order, zero counts left out. */
    static Map<String, Long> withoutZeros(Map<String, Long> related) {
        Map<String, Long> shown = new LinkedHashMap<>();
        related.forEach((key, count) -> {
            if (count > 0) {
                shown.put(key, count);
            }
        });
        return shown;
    }
}
