package com.electrician.tracker.dto.report;

import com.electrician.tracker.domain.job.JobType;

/** Which jobs a monthly/yearly report covers. */
public enum ReportJobFilter {
    ALL,
    SITES,
    SERVICES;

    public boolean matches(JobType type) {
        return switch (this) {
            case ALL -> true;
            case SITES -> type == JobType.SITE;
            case SERVICES -> type == JobType.SERVICE;
        };
    }
}
