package com.electrician.tracker.service;

import java.util.Optional;

/**
 * Turns a free-typed Turkish phone number into the digits-only international
 * form a WhatsApp link needs ("0532 418 77 50" becomes "905324187750"):
 * country code first, no plus sign, no separators.
 *
 * <p>Customer records hold whatever was typed, so the input may carry spaces,
 * dashes, parentheses, a leading plus, the "00" dialling prefix, or nothing
 * usable at all. Anything that cannot be read as one complete Turkish number
 * yields an empty result rather than a guess: a half-read record must never
 * become a message to a stranger.
 */
public final class PhoneNumbers {

    private static final String COUNTRY_CODE = "90";
    private static final String DIALLING_PREFIX = "00";
    private static final int NATIONAL_LENGTH = 10;
    private static final int INTERNATIONAL_LENGTH = NATIONAL_LENGTH + 2;
    private static final int TRUNK_LENGTH = NATIONAL_LENGTH + 1;
    private static final char LOWEST_AREA_DIGIT = '2';
    private static final char HIGHEST_AREA_DIGIT = '5';

    private PhoneNumbers() {
    }

    /** The number as "905XXXXXXXXX", or empty when it cannot be read with certainty. */
    public static Optional<String> toWhatsApp(String raw) {
        String national = nationalPart(digitsOf(raw));
        return isComplete(national) ? Optional.of(COUNTRY_CODE + national) : Optional.empty();
    }

    private static String digitsOf(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder digits = new StringBuilder(raw.length());
        for (char character : raw.toCharArray()) {
            if (Character.isDigit(character)) {
                digits.append(character);
            }
        }
        return digits.toString();
    }

    /**
     * Peels off the prefixes people type in front of the ten national digits:
     * the "00" dialling prefix, the "90" country code and the "0" trunk code.
     * Each is removed only at the length it can legitimately appear at, so a
     * number that is merely too short or too long falls through untouched and
     * is rejected below.
     */
    private static String nationalPart(String digits) {
        String rest = digits;
        if (rest.startsWith(DIALLING_PREFIX)) {
            rest = rest.substring(DIALLING_PREFIX.length());
        }
        if (rest.length() == INTERNATIONAL_LENGTH && rest.startsWith(COUNTRY_CODE)) {
            rest = rest.substring(COUNTRY_CODE.length());
        }
        if (rest.length() == TRUNK_LENGTH && rest.charAt(0) == '0') {
            rest = rest.substring(1);
        }
        return rest;
    }

    /**
     * Turkish numbers are ten digits opening with a mobile prefix (5) or an
     * area code (2 to 4). The opening digit is what catches a number that lost
     * a character to a typo: "0532418775" is ten digits too, but starts with
     * the trunk code and is therefore incomplete.
     */
    private static boolean isComplete(String national) {
        if (national.length() != NATIONAL_LENGTH) {
            return false;
        }
        char opening = national.charAt(0);
        return opening >= LOWEST_AREA_DIGIT && opening <= HIGHEST_AREA_DIGIT;
    }
}
