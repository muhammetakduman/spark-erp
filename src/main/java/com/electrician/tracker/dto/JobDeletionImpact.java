package com.electrician.tracker.dto;

import com.electrician.tracker.domain.JobType;

/** What deleting a job takes with it, shown in the delete confirmation before anything is removed. */
public record JobDeletionImpact(
        Long jobId,
        JobType jobType,
        String jobLabel,
        long materialCount,
        long attendanceCount,
        long paymentCount) {
}
