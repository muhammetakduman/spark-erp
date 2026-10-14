package com.electrician.tracker.service.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.electrician.tracker.dto.BulkMessageResult;
import com.electrician.tracker.dto.MessageTarget;
import com.electrician.tracker.ui.util.SmsLength;
import org.junit.jupiter.api.Test;

class BulkMessageSenderTest {

    private final RecordingGateway gateway = new RecordingGateway();
    private final BulkMessageSender sender = new BulkMessageSender(gateway);

    private static MessageTarget target(String name, String phone, String balance) {
        return MessageTarget.of(name, phone, Map.of(
                MessagePlaceholders.CUSTOMER, name,
                MessagePlaceholders.BALANCE, balance));
    }

    @Test
    void everyRecipientGetsTheMessageFilledWithTheirOwnDetails() {
        List<MessageTarget> targets = List.of(
                target("Ahmet Yılmaz", "0532 418 77 50", "1.000,00 ₺"),
                target("Zehra Aydın", "0545 229 36 81", "2.500,00 ₺"));

        BulkMessageResult result = sender.send("Sayın {musteri}, {bakiye} borcunuz var.", targets, this::reason);

        assertThat(result.sent()).isEqualTo(2);
        assertThat(result.isCompleteSuccess()).isTrue();
        assertThat(gateway.messages).containsExactly(
                "Sayın Ahmet Yılmaz, 1.000,00 ₺ borcunuz var.",
                "Sayın Zehra Aydın, 2.500,00 ₺ borcunuz var.");
    }

    @Test
    void thePhoneIsHandedOverInTheInternationalForm() {
        sender.send("Merhaba", List.of(target("Ahmet", "0532 418 77 50", "0")), this::reason);

        assertThat(gateway.phones).containsExactly("905324187750");
    }

    @Test
    void oneUnreadableRecordDoesNotCostTheOthersTheirMessage() {
        List<MessageTarget> targets = List.of(
                target("Ahmet", "0532 418 77 50", "0"),
                target("Bozuk Kayıt", "532", "0"),
                target("Zehra", "0545 229 36 81", "0"));

        BulkMessageResult result = sender.send("Merhaba", targets, this::reason);

        assertThat(result.sent()).isEqualTo(2);
        assertThat(result.unreadable()).containsExactly("Bozuk Kayıt");
        assertThat(gateway.phones).hasSize(2);
    }

    @Test
    void aRefusalIsRecordedWithItsReasonAndTheRunCarriesOn() {
        gateway.refuse("905330000000");
        List<MessageTarget> targets = List.of(
                target("Limit Dolu", "0533 000 00 00", "0"),
                target("Ahmet", "0532 418 77 50", "0"));

        BulkMessageResult result = sender.send("Merhaba", targets, this::reason);

        assertThat(result.sent()).isEqualTo(1);
        assertThat(result.refused()).singleElement()
                .satisfies(failure -> {
                    assertThat(failure.name()).isEqualTo("Limit Dolu");
                    assertThat(failure.reason()).isEqualTo("error.sms.netgsm.80");
                });
        assertThat(result.attempted()).isEqualTo(2);
        assertThat(result.isCompleteSuccess()).isFalse();
    }

    @Test
    void unreadableRecordsAreLeftOutOfWhatTheRunWillCost() {
        List<MessageTarget> targets = List.of(
                target("Ahmet", "0532 418 77 50", "0"),
                target("Bozuk", "532", "0"));

        assertThat(BulkMessageSender.reachable(targets)).hasSize(1);
        assertThat(BulkMessageSender.totalParts("Kısa mesaj", targets, SmsLength::partCount)).isEqualTo(1);
    }

    @Test
    void theCostCountsEachRecipientsOwnFilledText() {
        // The name goes into the message, so a long one can push its own
        // message into a second part while the others still fit in one.
        List<MessageTarget> targets = List.of(
                target("Al", "0532 418 77 50", "0"),
                target("A".repeat(120), "0545 229 36 81", "0"));

        int parts = BulkMessageSender.totalParts("a".repeat(100) + " {musteri}", targets, SmsLength::partCount);

        assertThat(parts).isEqualTo(3);
    }

    private String reason(SmsResult result) {
        return result.code();
    }

    /** Stands in for the provider: records what it was asked to send. */
    private static final class RecordingGateway implements SmsGateway {

        private final List<String> phones = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();
        private final List<String> refused = new ArrayList<>();

        void refuse(String phone) {
            refused.add(phone);
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public SmsResult send(String phone, String message) {
            if (refused.contains(phone)) {
                return SmsResult.failed("error.sms.netgsm.80", null);
            }
            phones.add(phone);
            messages.add(message);
            return SmsResult.sent("job" + phones.size());
        }
    }
}
