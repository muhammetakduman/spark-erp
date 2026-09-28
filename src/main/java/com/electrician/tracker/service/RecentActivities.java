package com.electrician.tracker.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.RecentActivity;

/**
 * The main screen's "son hareketler": the newest jobs opened, material lines
 * and payments of an already-loaded board, newest first. Entries only carry a
 * date, so on the same day a payment comes before a material line, which comes
 * before the job opening.
 */
public class RecentActivities {

    private static final Comparator<RecentActivity> NEWEST_FIRST = Comparator
            .comparing(RecentActivity::date, Comparator.reverseOrder())
            .thenComparing(RecentActivity::kind, Comparator.reverseOrder());

    public List<RecentActivity> latest(JobBoard board, Set<RecentActivity.Kind> kinds, int limit) {
        return board.jobs().stream()
                .flatMap(job -> activitiesOf(board, job))
                .filter(activity -> activity.date() != null && kinds.contains(activity.kind()))
                .sorted(NEWEST_FIRST)
                .limit(limit)
                .toList();
    }

    private static Stream<RecentActivity> activitiesOf(JobBoard board, Job job) {
        String label = JobLabels.full(job);
        Stream<RecentActivity> opened = Stream.of(new RecentActivity(job.getStartDate(),
                RecentActivity.Kind.JOB_OPENED, job.getId(), job.getType(), label, null, null));
        Stream<RecentActivity> materials = board.materialsFor(job.getId()).stream()
                .map(item -> new RecentActivity(item.getItemDate(), RecentActivity.Kind.MATERIAL, job.getId(),
                        job.getType(), label, item.getProduct().getDisplayName(), item.getQuantity()));
        Stream<RecentActivity> payments = board.paymentsFor(job.getId()).stream()
                .filter(Objects::nonNull)
                .map(payment -> new RecentActivity(payment.getPaymentDate(), RecentActivity.Kind.PAYMENT,
                        job.getId(), job.getType(), label, null, payment.getAmount()));
        return Stream.of(opened, materials, payments).flatMap(stream -> stream);
    }
}
