package com.electrician.tracker.dto.material;

import com.electrician.tracker.domain.job.JobType;

/** One job's spending on one material group: "Zehra Aydın – 945/3 Adaparsel · 2 kalem · …". */
public record GroupJobRow(Long jobId, JobType jobType, String customerName, String jobName, MaterialGroupTotal total) {

    public String label() {
        return jobName == null || jobName.isBlank() ? customerName : customerName + " – " + jobName;
    }

    public GroupJobRow withoutFinancials() {
        return new GroupJobRow(jobId, jobType, customerName, jobName, total.withoutFinancials());
    }
}
