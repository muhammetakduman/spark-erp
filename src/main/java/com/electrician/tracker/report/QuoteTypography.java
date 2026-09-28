package com.electrician.tracker.report;

/**
 * Font size of the quote's item table by the number of lines, so that even
 * 25 lines fit on one page: 1–15 → 9 pt, 16–20 → 8 pt, 21–25 → 7 pt. Cell
 * padding shrinks with the font.
 */
final class QuoteTypography {

    static final float LARGE = 9;
    static final float MEDIUM = 8;
    static final float SMALL = 7;
    private static final int LARGE_UP_TO = 15;
    private static final int MEDIUM_UP_TO = 20;
    private static final float PADDING_RATIO = 0.3f;

    private QuoteTypography() {
    }

    static float itemFontSize(int lineCount) {
        if (lineCount <= LARGE_UP_TO) {
            return LARGE;
        }
        return lineCount <= MEDIUM_UP_TO ? MEDIUM : SMALL;
    }

    static float cellPadding(float fontSize) {
        return fontSize * PADDING_RATIO;
    }
}
