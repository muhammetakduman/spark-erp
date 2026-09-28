package com.electrician.tracker.domain;

/**
 * State of a daily job: planned, done ("Gidildi"), not visited ("Gidilmedi"),
 * moved to another day (a new PLANNED entry was created for that day) or
 * cancelled.
 */
public enum DailyJobStatus {
    PLANNED,
    COMPLETED,
    NOT_VISITED,
    POSTPONED,
    CANCELLED
}
