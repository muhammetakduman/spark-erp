package com.electrician.tracker.ui.util.messaging;

/**
 * How many SMS a text will be charged as.
 *
 * <p>A message of plain GSM letters fits 160 characters; one Turkish letter
 * anywhere in it ("ş", "ğ", "ı", "İ", "ö", "ü", "ç") forces the whole message
 * into the 16-bit alphabet, where only 70 fit. Longer messages are split, and
 * each part is charged, so a stray "ğ" can double the bill of a mailing. The
 * dialog shows this count while the user writes.
 */
public final class SmsLength {

    private static final int GSM_SINGLE = 160;
    private static final int GSM_MULTIPART = 153;
    private static final int UNICODE_SINGLE = 70;
    private static final int UNICODE_MULTIPART = 67;

    /** The Turkish letters the GSM alphabet does not carry. */
    private static final String UNICODE_FORCING = "şŞğĞıİ";

    private SmsLength() {
    }

    public static int partCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        boolean unicode = needsUnicode(text);
        int single = unicode ? UNICODE_SINGLE : GSM_SINGLE;
        if (text.length() <= single) {
            return 1;
        }
        int perPart = unicode ? UNICODE_MULTIPART : GSM_MULTIPART;
        return (text.length() + perPart - 1) / perPart;
    }

    /**
     * "ö", "ü" and "ç" are in the GSM alphabet; the dotted and dotless i, "ş"
     * and "ğ" are not, and one of them is enough to switch the whole message.
     */
    public static boolean needsUnicode(String text) {
        if (text == null) {
            return false;
        }
        return text.chars().anyMatch(c -> UNICODE_FORCING.indexOf(c) >= 0);
    }
}
