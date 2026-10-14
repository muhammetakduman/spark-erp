package com.electrician.tracker.dto.quote;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.electrician.tracker.domain.quote.QuoteStatus;

/**
 * One line of the quote list. {@code expired}: sent, still unanswered and
 * past its validity date (shown with a yellow warning badge).
 */
public record QuoteRow(
        Long id,
        String quoteNo,
        LocalDate quoteDate,
        String companyName,
        String subject,
        BigDecimal grandTotal,
        QuoteStatus status,
        LocalDate validUntil,
        boolean expired,
        Long jobId) {
}
