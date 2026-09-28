package com.electrician.tracker.domain;

/** What a {@link Template} holds: quote notes/terms, a set of quote lines, or a daily job title. */
public enum TemplateType {
    QUOTE_NOTE,
    QUOTE_ITEM_SET,
    JOB_DESCRIPTION;

    /** Plain-text templates can be written and edited by hand; item sets come from a quote. */
    public boolean isText() {
        return this != QUOTE_ITEM_SET;
    }
}
