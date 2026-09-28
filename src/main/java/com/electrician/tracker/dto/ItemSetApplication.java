package com.electrician.tracker.dto;

import java.util.List;

/**
 * The quote lines after an item-set template was added below them, and how
 * many template lines were left out because their product was deleted.
 */
public record ItemSetApplication(List<QuoteLine> lines, int skippedCount) {
}
