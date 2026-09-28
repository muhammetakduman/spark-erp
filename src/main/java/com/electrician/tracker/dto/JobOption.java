package com.electrician.tracker.dto;

import com.electrician.tracker.domain.JobType;

/**
 * An active site or service a daily job can be linked to, with what the form
 * fills in from it (customer, address, phone, title).
 */
public record JobOption(Long jobId, JobType type, String label, String title, Long customerId, String customerName,
        String address, String phone) {

    @Override
    public String toString() {
        return label;
    }
}
