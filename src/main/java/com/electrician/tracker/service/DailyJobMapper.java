package com.electrician.tracker.service;

import java.util.Comparator;
import java.util.List;

import com.electrician.tracker.domain.DailyJob;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.dto.DailyJobDraft;
import com.electrician.tracker.dto.JobOption;

/** Turns daily job entities (with employees, customer and job loaded) into what the screens use. */
final class DailyJobMapper {

    private DailyJobMapper() {
    }

    static DailyJobCard toCard(DailyJob entry, int postponeCount) {
        List<Employee> employees = entry.getEmployees().stream()
                .sorted(Comparator.comparing(Employee::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Job job = entry.getJob();
        return new DailyJobCard(entry.getId(), entry.getJobDate(), entry.getTimeOfDay(), entry.getTitle(),
                customerName(entry), entry.getAddress(), entry.getPhone(),
                employees.stream().map(Employee::getId).toList(),
                employees.stream().map(Employee::getName).toList(),
                entry.getStatus(), entry.getPriority(), postponeCount,
                job == null ? null : job.getId(), job == null ? null : JobLabels.full(job),
                entry.getNote(), entry.getCompletionNote(), entry.getPostponedTo());
    }

    static DailyJobDraft toDraft(DailyJob entry) {
        return new DailyJobDraft(entry.getJobDate(), entry.getTimeOfDay(), entry.getTitle(),
                entry.getCustomer() == null ? null : entry.getCustomer().getId(), customerName(entry),
                entry.getAddress(), entry.getPhone(),
                entry.getEmployees().stream().map(Employee::getId).toList(), entry.getPriority(), entry.getNote(),
                entry.getJob() == null ? null : entry.getJob().getId());
    }

    static JobOption toOption(Job job) {
        return new JobOption(job.getId(), job.getType(), JobLabels.full(job), JobLabels.shortName(job),
                job.getCustomer().getId(), job.getCustomer().getName(),
                job.getAddress() != null ? job.getAddress() : job.getCustomer().getAddress(),
                job.getCustomer().getPhone());
    }

    private static String customerName(DailyJob entry) {
        return entry.getCustomer() == null ? entry.getCustomerName() : entry.getCustomer().getName();
    }
}
