package com.electrician.tracker.dto.dailyjob;

/** A daily job created by postponing another one: {@code id} came from {@code sourceId}. */
public record DailyJobLink(Long id, Long sourceId) {
}
