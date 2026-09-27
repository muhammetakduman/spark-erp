package com.electrician.tracker.ui.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.electrician.tracker.dto.WorkedDay;
import com.electrician.tracker.service.AttendanceMath;

/**
 * The single place for tr-TR display text: money ("12.450,50 ₺"), dates
 * ("23.09.2026", "23.09"), quantities ("2,5"), day counts ("2,5 gün") and
 * worked-day lists ("12.09, 13.09 (½)"). Independent of the runtime's
 * default locale.
 */
public final class Bicimlendirici {

    public static final Locale TURKISH = Locale.forLanguageTag("tr-TR");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd.MM");

    private static final String MONEY_PATTERN = "#,##0.00 ₺";
    private static final String QUANTITY_PATTERN = "#,##0.###";
    private static final String DAY_COUNT_PATTERN = "#,##0.#";
    private static final String DAY_FACTOR_PATTERN = "0.0";
    private static final String CURRENCY_SYMBOL = "₺";
    private static final String HALF_DAY_MARK = " (½)";
    private static final String LIST_SEPARATOR = ", ";
    private static final char DECIMAL_COMMA = ',';
    private static final char GROUP_DOT = '.';
    private static final Pattern GROUPED_INTEGER = Pattern.compile("\\d{1,3}(\\.\\d{3})+");
    private static final Pattern DOT_DECIMAL = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*|\\d+)\\.(\\d{1,2})");
    private static final int DOT_DECIMAL_FRACTION_GROUP = 2;

    private Bicimlendirici() {
    }

    // ---- Money -------------------------------------------------------------

    /** "12.450,50 ₺"; empty for {@code null}. */
    public static String money(BigDecimal value) {
        return value == null ? "" : pattern(MONEY_PATTERN).format(value);
    }

    /**
     * Parses tr-TR number text: "12.450,50 ₺", "12450,5", "1.000" (thousands)
     * and, for pasted/typed English style, "1.000.50" or "12.5" (a last dot
     * followed by one or two digits is the decimal separator). Dots anywhere
     * else must be proper thousands groups, so "1.000.00" and "1000,00" are the
     * same amount and an ambiguous text is rejected instead of guessed.
     * {@code null} when blank.
     *
     * @throws NumberFormatException when the text is not a number
     */
    public static BigDecimal parseMoney(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = text.replace(CURRENCY_SYMBOL, "").replaceAll("\\s", "");
        int comma = cleaned.indexOf(DECIMAL_COMMA);
        if (comma >= 0) {
            if (cleaned.indexOf(DECIMAL_COMMA, comma + 1) >= 0) {
                throw new NumberFormatException(text);
            }
            return toDecimal(plainInteger(cleaned.substring(0, comma)), cleaned.substring(comma + 1));
        }
        Matcher dotDecimal = DOT_DECIMAL.matcher(cleaned);
        if (!GROUPED_INTEGER.matcher(cleaned).matches() && dotDecimal.matches()) {
            return toDecimal(plainInteger(dotDecimal.group(1)), dotDecimal.group(DOT_DECIMAL_FRACTION_GROUP));
        }
        return toDecimal(plainInteger(cleaned), "");
    }

    private static String plainInteger(String integerPart) {
        if (integerPart.isEmpty()) {
            return "0";
        }
        if (integerPart.indexOf(GROUP_DOT) >= 0 && !GROUPED_INTEGER.matcher(integerPart).matches()) {
            throw new NumberFormatException(integerPart);
        }
        return integerPart.replace(String.valueOf(GROUP_DOT), "");
    }

    private static BigDecimal toDecimal(String integerPart, String fraction) {
        if (!fraction.chars().allMatch(Character::isDigit)) {
            throw new NumberFormatException(fraction);
        }
        return new BigDecimal(fraction.isEmpty() ? integerPart : integerPart + "." + fraction);
    }

    // ---- Dates -------------------------------------------------------------

    /** "23.09.2026"; empty for {@code null}. */
    public static String date(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    /** "23.09"; empty for {@code null}. */
    public static String shortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE);
    }

    // ---- Quantities and days ------------------------------------------------

    /** "2,5" / "1.200"; empty for {@code null}. */
    public static String quantity(BigDecimal value) {
        return value == null ? "" : pattern(QUANTITY_PATTERN).format(value);
    }

    /** "2,5" / "3" — use with the "gün" suffix from the bundle. */
    public static String days(BigDecimal dayCount) {
        return pattern(DAY_COUNT_PATTERN).format(dayCount == null ? BigDecimal.ZERO : dayCount);
    }

    /** "2,5 gün". */
    public static String daysWithUnit(BigDecimal dayCount) {
        return DialogUtil.message("attendance.days", days(dayCount));
    }

    /** "1,0" or "0,5". */
    public static String factor(BigDecimal factor) {
        return pattern(DAY_FACTOR_PATTERN).format(factor == null ? AttendanceMath.FULL_DAY : factor);
    }

    public static boolean isHalfDay(BigDecimal factor) {
        return factor != null && factor.compareTo(AttendanceMath.HALF_DAY) == 0;
    }

    /** "12.09, 13.09 (½), 14.09". */
    public static String workedDays(List<WorkedDay> days) {
        return days.stream()
                .map(day -> shortDate(day.date()) + (isHalfDay(day.dayFactor()) ? HALF_DAY_MARK : ""))
                .collect(Collectors.joining(LIST_SEPARATOR));
    }

    private static DecimalFormat pattern(String pattern) {
        DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(TURKISH));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}
