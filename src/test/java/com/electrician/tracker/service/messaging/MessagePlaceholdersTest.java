package com.electrician.tracker.service.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MessagePlaceholdersTest {

    private final Map<String, String> values = Map.of(
            MessagePlaceholders.CUSTOMER, "Ahmet Yılmaz",
            MessagePlaceholders.COMPANY, "AYZEN GRUP",
            MessagePlaceholders.BALANCE, "2.500,00 ₺");

    @Test
    void everyKnownPlaceholderIsReplaced() {
        String filled = MessagePlaceholders.fill(
                "Sayın {musteri}, {firma} olarak {bakiye} tutarındaki bakiyenizi hatırlatırız.", values);

        assertThat(filled)
                .isEqualTo("Sayın Ahmet Yılmaz, AYZEN GRUP olarak 2.500,00 ₺ tutarındaki bakiyenizi hatırlatırız.");
    }

    @Test
    void aPlaceholderNobodyKnowsIsLeftStandingSoTheTypoIsVisible() {
        assertThat(MessagePlaceholders.fill("Sayın {musterii}, merhaba", values))
                .isEqualTo("Sayın {musterii}, merhaba");
    }

    @Test
    void aKnownPlaceholderWithoutAValueBecomesEmpty() {
        Map<String, String> withNull = new HashMap<>(values);
        withNull.put(MessagePlaceholders.BALANCE, null);

        assertThat(MessagePlaceholders.fill("Bakiye: {bakiye}", withNull)).isEqualTo("Bakiye: ");
    }

    @Test
    void theSamePlaceholderMayAppearMoreThanOnce() {
        assertThat(MessagePlaceholders.fill("{musteri}, tekrar merhaba {musteri}", values))
                .isEqualTo("Ahmet Yılmaz, tekrar merhaba Ahmet Yılmaz");
    }

    @Test
    void aTemplateWithoutPlaceholdersComesBackUntouched() {
        String greeting = "Kandiliniz mübarek olsun.";

        assertThat(MessagePlaceholders.fill(greeting, values)).isEqualTo(greeting);
    }

    @Test
    void aValueCarryingADollarOrBackslashDoesNotBreakTheReplacement() {
        // Both are special in regular-expression replacements; the text must survive them.
        String filled = MessagePlaceholders.fill("{musteri}", Map.of(MessagePlaceholders.CUSTOMER, "A$1\\B"));

        assertThat(filled).isEqualTo("A$1\\B");
    }

    @Test
    void anEmptyTemplateGivesAnEmptyMessage() {
        assertThat(MessagePlaceholders.fill(null, values)).isEmpty();
        assertThat(MessagePlaceholders.fill("   ", values)).isEmpty();
    }
}
