package com.electrician.tracker.dto;

/** A grouped count per record id (e.g. material lines per product), from one aggregate query. */
public record IdCount(Long id, long count) {
}
