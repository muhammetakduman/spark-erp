package com.electrician.tracker.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class NumberInputTest {

    private static final int MONEY = NumberInput.MONEY_SCALE;
    private static final int QUANTITY = 3;

    @Test
    void groupsThousandsWhileTyping() {
        assertThat(NumberInput.normalizeTyping("1000", MONEY)).isEqualTo("1.000");
        assertThat(NumberInput.normalizeTyping("1234567,5", MONEY)).isEqualTo("1.234.567,5");
        assertThat(NumberInput.normalizeTyping("12,", MONEY)).isEqualTo("12,");
        assertThat(NumberInput.normalizeTyping(",5", MONEY)).isEqualTo("0,5");
        assertThat(NumberInput.normalizeTyping("007", MONEY)).isEqualTo("7");
        assertThat(NumberInput.normalizeTyping("", MONEY)).isEmpty();
    }

    @Test
    void typedDotsAreIgnoredSoEveryAmountLooksTheSame() {
        assertThat(NumberInput.normalizeTyping("1.000", MONEY)).isEqualTo("1.000");
        assertThat(NumberInput.normalizeTyping("1.0000", MONEY)).isEqualTo("10.000");
    }

    @Test
    void rejectsInvalidInput() {
        assertThat(NumberInput.normalizeTyping("12a", MONEY)).isNull();
        assertThat(NumberInput.normalizeTyping("1,2,3", MONEY)).isNull();
        assertThat(NumberInput.normalizeTyping("1,234", MONEY)).isNull();
        assertThat(NumberInput.normalizeTyping("-5", MONEY)).isNull();
        assertThat(NumberInput.normalizeTyping("1,234", QUANTITY)).isEqualTo("1,234");
    }

    @Test
    void pastedTextIsReadAsTheSameNumber() {
        assertThat(NumberInput.canonicalInsert("1.250,00 ₺")).isEqualTo("1250,00");
        assertThat(NumberInput.canonicalInsert("1000.50")).isEqualTo("1000,50");
        assertThat(NumberInput.canonicalInsert("abc")).isEqualTo("abc");
    }

    @Test
    void keepsCaretAfterTheSameDigit() {
        // "1000|" typed → "1.000|"
        assertThat(NumberInput.caretFor("1.000", NumberInput.significantCount("1000"))).isEqualTo(5);
        // caret after the first digit stays after it
        assertThat(NumberInput.caretFor("1.000", 1)).isEqualTo(1);
        assertThat(NumberInput.caretFor("1.000", 0)).isZero();
    }

    @Test
    void formatsFinalValue() {
        assertThat(NumberInput.format(new BigDecimal("1000"), MONEY, true)).isEqualTo("1.000,00");
        assertThat(NumberInput.format(new BigDecimal("2.5"), QUANTITY, false)).isEqualTo("2,5");
        assertThat(NumberInput.format(null, MONEY, true)).isEmpty();
    }
}
