package com.electrician.tracker.ui.util.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmsLengthTest {

    @Test
    void plainTextFitsOneHundredSixtyCharactersInOneMessage() {
        assertThat(SmsLength.partCount("a".repeat(160))).isEqualTo(1);
        assertThat(SmsLength.partCount("a".repeat(161))).isEqualTo(2);
    }

    @Test
    void oneTurkishLetterHalvesWhatFits() {
        // "ş" is outside the GSM alphabet, so the whole message switches and
        // only seventy characters fit — the stray letter doubles the bill.
        assertThat(SmsLength.partCount("a".repeat(100))).isEqualTo(1);
        assertThat(SmsLength.partCount("ş" + "a".repeat(99))).isEqualTo(2);
    }

    @Test
    void theLettersGsmAlreadyCarriesDoNotSwitchTheMessage() {
        assertThat(SmsLength.needsUnicode("öğün")).isTrue();
        assertThat(SmsLength.needsUnicode("öküz çocuk")).isFalse();
        assertThat(SmsLength.needsUnicode("Isparta")).isFalse();
        assertThat(SmsLength.needsUnicode("İstanbul")).isTrue();
    }

    @Test
    void longMessagesLoseRoomToTheJoiningHeader() {
        // Split messages carry a header, so each part holds 153 instead of 160.
        assertThat(SmsLength.partCount("a".repeat(306))).isEqualTo(2);
        assertThat(SmsLength.partCount("a".repeat(307))).isEqualTo(3);
    }

    @Test
    void nothingToSendIsNoMessageAtAll() {
        assertThat(SmsLength.partCount(null)).isZero();
        assertThat(SmsLength.partCount("")).isZero();
    }
}
