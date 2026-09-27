package com.electrician.tracker.domain;

/**
 * How a {@link MaterialItem}'s sale price was entered: per unit, or as a
 * ready-made line total that the unit price is derived from.
 */
public enum PriceEntryType {
    UNIT,
    TOTAL
}
