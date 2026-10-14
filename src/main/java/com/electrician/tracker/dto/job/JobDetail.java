package com.electrician.tracker.dto.job;

import java.util.List;

import com.electrician.tracker.domain.employee.Attendance;
import com.electrician.tracker.domain.job.Job;
import com.electrician.tracker.domain.material.MaterialItem;
import com.electrician.tracker.domain.job.Payment;

/** One job reloaded on its own, so a screen can refresh just that job's panel. */
public record JobDetail(Job job, JobSummary summary, List<MaterialItem> materials, List<Attendance> attendances,
        List<Payment> payments) {
}
