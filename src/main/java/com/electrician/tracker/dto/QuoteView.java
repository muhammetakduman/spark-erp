package com.electrician.tracker.dto;

import com.electrician.tracker.domain.JobType;

/**
 * A saved (or new, {@code id == null}) quote as the editor and the PDF show
 * it: its content, who prepared it, the job it became (if any) and totals.
 */
public record QuoteView(
        Long id,
        QuoteDraft draft,
        String preparedByName,
        String preparedByTitle,
        Long jobId,
        JobType jobType,
        QuoteTotals totals) {

    public boolean isConvertedToJob() {
        return jobId != null;
    }
}
