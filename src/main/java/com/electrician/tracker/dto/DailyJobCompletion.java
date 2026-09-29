package com.electrician.tracker.dto;

/**
 * Outcome of "Gidildi": the attendance rows written (or skipped as already
 * present) and whether a new service was opened for an unlinked entry.
 */
public record DailyJobCompletion(AttendanceSaveResult attendance, boolean serviceCreated) {
}
