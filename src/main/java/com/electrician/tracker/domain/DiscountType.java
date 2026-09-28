package com.electrician.tracker.domain;

/** How a quote's discount is given: none, a fixed amount, or a percentage of the items total. */
public enum DiscountType {
    NONE,
    AMOUNT,
    PERCENT
}
