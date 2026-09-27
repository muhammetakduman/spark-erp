package com.electrician.tracker.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;

/**
 * Everything the main screen needs, loaded with a handful of aggregated
 * queries instead of one round trip per job (see the "no N+1" rule in
 * CLAUDE.md).
 */
public record JobBoard(
        List<Job> jobs,
        Map<Long, JobSummary> summaries,
        Map<Long, List<MaterialItem>> materialsByJob,
        Map<Long, List<Attendance>> attendancesByJob,
        Map<Long, List<Payment>> paymentsByJob) {

    public List<MaterialItem> materialsFor(Long jobId) {
        return materialsByJob.getOrDefault(jobId, List.of());
    }

    public List<Attendance> attendancesFor(Long jobId) {
        return attendancesByJob.getOrDefault(jobId, List.of());
    }

    public List<Payment> paymentsFor(Long jobId) {
        return paymentsByJob.getOrDefault(jobId, List.of());
    }

    /** A copy of this board with one job's data replaced by a fresh reload. */
    public JobBoard withJob(JobDetail detail) {
        Long jobId = detail.job().getId();
        List<Job> updatedJobs = jobs.stream()
                .map(job -> job.getId().equals(jobId) ? detail.job() : job)
                .toList();
        return new JobBoard(updatedJobs,
                replaced(summaries, jobId, detail.summary()),
                replaced(materialsByJob, jobId, detail.materials()),
                replaced(attendancesByJob, jobId, detail.attendances()),
                replaced(paymentsByJob, jobId, detail.payments()));
    }

    private static <V> Map<Long, V> replaced(Map<Long, V> source, Long jobId, V value) {
        Map<Long, V> copy = new HashMap<>(source);
        copy.put(jobId, value);
        return copy;
    }
}
