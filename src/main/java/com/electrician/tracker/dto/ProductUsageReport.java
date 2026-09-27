package com.electrician.tracker.dto;

import java.util.List;

/** A product's usage rows (newest first) and their summary. */
public record ProductUsageReport(List<ProductUsageRow> rows, ProductUsageSummary summary) {
}
