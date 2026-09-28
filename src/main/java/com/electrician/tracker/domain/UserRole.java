package com.electrician.tracker.domain;

/**
 * ADMIN is the owner and sees everything. MANAGER runs the jobs day to day
 * but never sees cost, profit, receivables, payments or wages.
 */
public enum UserRole {
    ADMIN,
    MANAGER
}
