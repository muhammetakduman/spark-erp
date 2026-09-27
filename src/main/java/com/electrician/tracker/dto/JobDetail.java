package com.electrician.tracker.dto;

import java.util.List;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;

/** One job reloaded on its own, so a screen can refresh just that job's panel. */
public record JobDetail(Job job, JobSummary summary, List<MaterialItem> materials, List<Attendance> attendances,
        List<Payment> payments) {
}
