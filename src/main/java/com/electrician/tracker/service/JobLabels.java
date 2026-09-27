package com.electrician.tracker.service;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobType;

/**
 * How a job is named in lists: "Müşteri – İş adı", and a short name for
 * tight spaces such as calendar cells (the site name, or the customer for a
 * service call).
 */
public final class JobLabels {

    private static final String SEPARATOR = " – ";

    private JobLabels() {
    }

    public static String full(Job job) {
        String customer = job.getCustomer().getName();
        if (job.getName() == null || job.getName().isBlank()) {
            return customer;
        }
        return customer + SEPARATOR + job.getName();
    }

    public static String shortName(Job job) {
        if (job.getType() == JobType.SITE && job.getName() != null && !job.getName().isBlank()) {
            return job.getName();
        }
        return job.getCustomer().getName();
    }
}
