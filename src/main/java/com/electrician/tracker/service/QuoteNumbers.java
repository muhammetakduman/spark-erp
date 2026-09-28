package com.electrician.tracker.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quote numbers "YYYY/NNNN" (e.g. 2026/0001): a counter per year, so the
 * first quote of a new year starts at 0001 again. Numbers typed by the user
 * in another format are simply ignored when counting.
 */
public final class QuoteNumbers {

    private static final String FORMAT = "%d/%04d";
    private static final Pattern NUMBER = Pattern.compile("(\\d{4})/(\\d+)");
    private static final int SEQUENCE_GROUP = 2;

    private QuoteNumbers() {
    }

    /** "2026/%" for a LIKE query over the year's numbers. */
    public static String yearPrefixPattern(int year) {
        return year + "/%";
    }

    /** The number after the highest one already used in {@code year}. */
    public static String next(int year, List<String> numbersOfYear) {
        int highest = numbersOfYear.stream()
                .map(NUMBER::matcher)
                .filter(Matcher::matches)
                .filter(matcher -> Integer.parseInt(matcher.group(1)) == year)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(SEQUENCE_GROUP)))
                .max()
                .orElse(0);
        return String.format(FORMAT, year, highest + 1);
    }
}
