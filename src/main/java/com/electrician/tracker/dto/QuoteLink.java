package com.electrician.tracker.dto;

/** A job created from a quote: "Bu iş 2026/0001 numaralı tekliften oluşturuldu". */
public record QuoteLink(Long quoteId, String quoteNo, Long jobId) {
}
