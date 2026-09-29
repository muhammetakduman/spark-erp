package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.electrician.tracker.dto.BulkDeletionResult;
import com.electrician.tracker.dto.IdCount;
import org.junit.jupiter.api.Test;

class BulkDeletionsTest {

    @Test
    void freeRecordsAreDeletedTogetherAndReferencedOnesSkippedWithTheFirstReason() {
        Map<String, List<IdCount>> references = new LinkedHashMap<>();
        references.put("material", List.of(new IdCount(2L, 5), new IdCount(3L, 0)));
        references.put("quote", List.of(new IdCount(2L, 1), new IdCount(4L, 3)));
        List<Long> deleted = new ArrayList<>();

        BulkDeletionResult result = BulkDeletions.deleteUnreferenced(List.of(1L, 2L, 3L, 4L, 1L), references,
                deleted::addAll);

        assertThat(deleted).containsExactly(1L, 3L);
        assertThat(result.deletedCount()).isEqualTo(2);
        assertThat(result.skipped()).containsExactly(
                new BulkDeletionResult.Skipped(2L, "material", 5),
                new BulkDeletionResult.Skipped(4L, "quote", 3));
    }

    @Test
    void nothingIsDeletedWhenEverySelectedRecordIsInUse() {
        List<Long> deleted = new ArrayList<>();

        BulkDeletionResult result = BulkDeletions.deleteUnreferenced(List.of(7L),
                Map.of("attendance", List.of(new IdCount(7L, 2))), deleted::addAll);

        assertThat(deleted).isEmpty();
        assertThat(result.deletedCount()).isZero();
        assertThat(result.skippedIds()).containsExactly(7L);
    }

    @Test
    void relatedCountsKeepTheirOrderWithoutZeros() {
        Map<String, Long> related = new LinkedHashMap<>();
        related.put("materials", 148L);
        related.put("attendance", 0L);
        related.put("payments", 31L);

        assertThat(BulkDeletions.withoutZeros(related)).containsExactly(Map.entry("materials", 148L),
                Map.entry("payments", 31L));
    }
}
