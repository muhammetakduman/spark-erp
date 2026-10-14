package com.electrician.tracker.service.messaging;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills "{musteri}", "{bakiye}" and friends in a message template.
 *
 * <p>Values arrive already formatted, because how money and dates read is a
 * screen concern and this layer must not depend on the user interface. A
 * placeholder the caller knows but has no value for becomes empty; one nobody
 * knows is left standing, so a typo shows up in the preview instead of
 * silently eating part of the sentence.
 */
public final class MessagePlaceholders {

    public static final String CUSTOMER = "musteri";
    public static final String COMPANY = "firma";
    public static final String BALANCE = "bakiye";
    public static final String AMOUNT = "tutar";
    public static final String DATE = "tarih";
    public static final String JOB = "is";
    public static final String STATUS = "durum";

    /** Offered to the user while writing a template, in this order. */
    public static final List<String> ALL = List.of(CUSTOMER, COMPANY, BALANCE, AMOUNT, DATE, JOB, STATUS);

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-ZğüşıöçĞÜŞİÖÇ]+)}");

    private MessagePlaceholders() {
    }

    public static String fill(String template, Map<String, String> values) {
        if (template == null || template.isBlank()) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder filled = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = values.containsKey(name) ? nullToEmpty(values.get(name)) : matcher.group();
            matcher.appendReplacement(filled, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(filled);
        return filled.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
