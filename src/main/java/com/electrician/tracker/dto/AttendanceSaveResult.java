package com.electrician.tracker.dto;

/** Outcome of an attendance batch: rows created and rows skipped as already present. */
public record AttendanceSaveResult(int createdCount, int skippedCount) {
}
