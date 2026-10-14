package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "0532 418 77 50",
            "05324187750",
            "+90 532 418 77 50",
            "+905324187750",
            "905324187750",
            "90 532 418 7750",
            "5324187750",
            "532 418 77 50",
            "(0532) 418-77-50",
            "0090 532 418 77 50",
            "  0532 418 77 50  " })
    void everyWayOfTypingOneMobileNumberGivesTheSameDigits(String typed) {
        assertThat(PhoneNumbers.toWhatsApp(typed)).contains("905324187750");
    }

    @Test
    void landlinesAreAcceptedToo() {
        assertThat(PhoneNumbers.toWhatsApp("0216 455 12 30")).contains("902164551230");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "   ",
            "abc",
            "532418775",
            "05324187750123" })
    void aRecordThatCannotBeReadGivesNothing(String typed) {
        assertThat(PhoneNumbers.toWhatsApp(typed)).isEmpty();
    }

    @Test
    void aNumberThatLostACharacterToATypoIsRejectedRatherThanGuessed() {
        // The letter leaves ten digits behind, but they still open with the
        // trunk code: reading them as a national number would dial a stranger.
        assertThat(PhoneNumbers.toWhatsApp("0532 418 77 5A")).isEmpty();
    }

    @Test
    void twoNumbersInOneFieldAreRejected() {
        assertThat(PhoneNumbers.toWhatsApp("0532 418 77 50 / 0216 455 12 30")).isEmpty();
    }
}
