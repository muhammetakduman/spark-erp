package com.electrician.tracker.domain.settings;

/**
 * What a {@link Template} holds: quote notes/terms, a set of quote lines, a
 * daily job title, or a ready message to send a customer over WhatsApp or SMS.
 */
public enum TemplateType {
    QUOTE_NOTE,
    QUOTE_ITEM_SET,
    JOB_DESCRIPTION,
    MESSAGE;

    /** Plain-text templates can be written and edited by hand; item sets come from a quote. */
    public boolean isText() {
        return this != QUOTE_ITEM_SET;
    }
}
