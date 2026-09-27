package com.electrician.tracker.service;

import java.util.Locale;

/**
 * Normalizes free-typed Turkish text for case/character-insensitive matching
 * (product names, search boxes) without adding any database column: folds
 * Turkish characters to their ASCII equivalents, lowercases, trims and
 * collapses internal whitespace.
 */
public final class MetinKarsilastirici {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    private MetinKarsilastirici() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(text.length());
        for (char c : text.toLowerCase(TURKISH).toCharArray()) {
            result.append(foldTurkishChar(c));
        }
        return result.toString().trim().replaceAll("\\s+", " ");
    }

    private static char foldTurkishChar(char c) {
        return switch (c) {
            case 'ı', 'i' -> 'i';
            case 'ş' -> 's';
            case 'ğ' -> 'g';
            case 'ü' -> 'u';
            case 'ö' -> 'o';
            case 'ç' -> 'c';
            default -> c;
        };
    }
}
