package com.electrician.tracker.service;

/**
 * Maximum lengths of quote texts, so the quote PDF always fits on one page.
 * The editor stops typing at these limits; saving checks them again.
 */
public final class QuoteFieldLimits {

    public static final int COMPANY_NAME = 60;
    public static final int ADDRESS = 120;
    public static final int CONTACT_PERSON = 40;
    public static final int PHONE = 20;
    public static final int EMAIL = 60;
    public static final int SUBJECT = 80;
    public static final int PRODUCT_NAME = 60;
    public static final int BRAND = 25;
    public static final int UNIT = 10;
    public static final int DESCRIPTION = 80;
    public static final int NOTES = 600;
    public static final int PREPARED_BY_NAME = 60;
    public static final int PREPARED_BY_TITLE = 40;

    /** Above this many lines the PDF switches to a smaller font (a notice is shown). */
    public static final int SMALL_FONT_LINE_COUNT = 20;

    private QuoteFieldLimits() {
    }
}
