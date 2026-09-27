package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Pure text rules behind {@link DecimalField}, kept free of JavaFX so they
 * can be unit tested. While typing, only digits and one decimal comma are
 * kept and the integer part is grouped automatically ("1000" → "1.000"), so
 * every amount looks the same wherever it is entered: "1.000,00".
 */
public final class NumberInput {

    public static final int MONEY_SCALE = 2;

    private static final int MAX_INTEGER_DIGITS = 12;
    private static final int GROUP_SIZE = 3;
    private static final char DECIMAL_COMMA = ',';
    private static final char GROUP_DOT = '.';
    private static final String CURRENCY_SYMBOL = "₺";

    private NumberInput() {
    }

    /**
     * The text to show for what the user typed, e.g. "12345,5" → "12.345,5";
     * {@code null} when the input must be rejected (a letter, a second comma,
     * more decimals than {@code scale} or an absurdly long number). Typed dots
     * are dropped because grouping is added automatically.
     */
    public static String normalizeTyping(String text, int scale) {
        String raw = text.replace(CURRENCY_SYMBOL, "").replace(String.valueOf(GROUP_DOT), "").strip();
        if (raw.isEmpty()) {
            return "";
        }
        int comma = raw.indexOf(DECIMAL_COMMA);
        boolean hasComma = comma >= 0;
        String integer = hasComma ? raw.substring(0, comma) : raw;
        String fraction = hasComma ? raw.substring(comma + 1) : "";
        if (!isDigits(integer) || !isDigits(fraction) || (hasComma && scale == 0)
                || fraction.length() > scale || integer.length() > MAX_INTEGER_DIGITS) {
            return null;
        }
        String grouped = group(stripLeadingZeros(integer));
        return hasComma ? grouped + DECIMAL_COMMA + fraction : grouped;
    }

    /**
     * Pasted or programmatically set text ("1000.50", "1.250,00 ₺") in the
     * form typing expects ("1000,5"); unchanged when it is not a number.
     */
    public static String canonicalInsert(String inserted) {
        try {
            BigDecimal value = Bicimlendirici.parseMoney(inserted);
            return value == null ? inserted : value.toPlainString().replace(GROUP_DOT, DECIMAL_COMMA);
        } catch (NumberFormatException e) {
            return inserted;
        }
    }

    /** Digits and the comma before the caret; grouping dots do not count. */
    public static int significantCount(String textBeforeCaret) {
        return (int) textBeforeCaret.chars().filter(c -> Character.isDigit(c) || c == DECIMAL_COMMA).count();
    }

    /** Caret position in {@code formatted} just after its n-th significant character. */
    public static int caretFor(String formatted, int significantCount) {
        if (significantCount <= 0) {
            return 0;
        }
        int seen = 0;
        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (Character.isDigit(c) || c == DECIMAL_COMMA) {
                seen++;
                if (seen == significantCount) {
                    return i + 1;
                }
            }
        }
        return formatted.length();
    }

    /**
     * Final display once the field is left: "1.000,00" for money
     * ({@code padDecimals}), "2,5" for a quantity; empty for {@code null}.
     */
    public static String format(BigDecimal value, int scale, boolean padDecimals) {
        if (value == null) {
            return "";
        }
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Bicimlendirici.TURKISH));
        format.setMaximumFractionDigits(scale);
        format.setMinimumFractionDigits(padDecimals ? scale : 0);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    private static boolean isDigits(String text) {
        return text.chars().allMatch(Character::isDigit);
    }

    private static String stripLeadingZeros(String integer) {
        String stripped = integer.replaceFirst("^0+", "");
        return stripped.isEmpty() ? "0" : stripped;
    }

    private static String group(String digits) {
        StringBuilder grouped = new StringBuilder();
        int firstGroup = digits.length() % GROUP_SIZE == 0 ? GROUP_SIZE : digits.length() % GROUP_SIZE;
        grouped.append(digits, 0, firstGroup);
        for (int i = firstGroup; i < digits.length(); i += GROUP_SIZE) {
            grouped.append(GROUP_DOT).append(digits, i, i + GROUP_SIZE);
        }
        return grouped.toString();
    }
}
