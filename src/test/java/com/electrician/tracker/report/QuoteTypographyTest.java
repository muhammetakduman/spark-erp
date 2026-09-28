package com.electrician.tracker.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuoteTypographyTest {

    @Test
    void fontShrinksWithTheNumberOfLines() {
        assertThat(QuoteTypography.itemFontSize(1)).isEqualTo(9);
        assertThat(QuoteTypography.itemFontSize(15)).isEqualTo(9);
        assertThat(QuoteTypography.itemFontSize(16)).isEqualTo(8);
        assertThat(QuoteTypography.itemFontSize(20)).isEqualTo(8);
        assertThat(QuoteTypography.itemFontSize(21)).isEqualTo(7);
        assertThat(QuoteTypography.itemFontSize(25)).isEqualTo(7);
    }
}
