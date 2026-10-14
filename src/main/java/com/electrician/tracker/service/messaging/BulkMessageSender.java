package com.electrician.tracker.service.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.electrician.tracker.dto.BulkMessageResult;
import com.electrician.tracker.dto.MessageTarget;
import com.electrician.tracker.service.PhoneNumbers;
import org.springframework.stereotype.Service;

/**
 * Sends one written message to a list of customers over SMS, filling each
 * recipient's own details into it on the way out.
 *
 * <p>This is what SMS is for: WhatsApp needs a press of send per chat, which
 * is fine for one reminder and hopeless for a greeting to the whole list.
 *
 * <p>The template is filled per recipient rather than once, so "{musteri}"
 * carries each name and "{bakiye}" each balance. A record whose phone cannot
 * be read is skipped and named at the end instead of stopping the run: one bad
 * record must not cost the other ninety-nine their message. A refusal from the
 * provider is recorded the same way, because the rest may still go through.
 */
@Service
public class BulkMessageSender {

    private final SmsGateway smsGateway;

    public BulkMessageSender(SmsGateway smsGateway) {
        this.smsGateway = smsGateway;
    }

    /**
     * @param template   the text as written, placeholders still in it
     * @param targets    everyone it is meant for
     * @param reasonText turns a provider code into words for the user
     */
    public BulkMessageResult send(String template, List<MessageTarget> targets,
            Function<SmsResult, String> reasonText) {
        List<String> unreadable = new ArrayList<>();
        List<BulkMessageResult.Failure> refused = new ArrayList<>();
        int sent = 0;
        for (MessageTarget target : targets) {
            String phone = PhoneNumbers.toWhatsApp(target.rawPhone()).orElse(null);
            if (phone == null) {
                unreadable.add(target.name());
                continue;
            }
            SmsResult result = smsGateway.send(phone, MessagePlaceholders.fill(template, target.values()));
            if (result.success()) {
                sent++;
            } else {
                refused.add(new BulkMessageResult.Failure(target.name(), reasonText.apply(result)));
            }
        }
        return new BulkMessageResult(sent, List.copyOf(unreadable), List.copyOf(refused));
    }

    /**
     * What the run will cost before it starts: every recipient is charged for
     * the parts their own filled text needs, and a name long enough to push
     * one of them over a boundary is charged for the extra part.
     */
    public static int totalParts(String template, List<MessageTarget> targets,
            Function<String, Integer> partsOf) {
        int total = 0;
        for (MessageTarget target : targets) {
            if (PhoneNumbers.toWhatsApp(target.rawPhone()).isPresent()) {
                total += partsOf.apply(MessagePlaceholders.fill(template, target.values()));
            }
        }
        return total;
    }

    /** Records this message can actually reach. */
    public static List<MessageTarget> reachable(List<MessageTarget> targets) {
        return targets.stream().filter(t -> PhoneNumbers.toWhatsApp(t.rawPhone()).isPresent()).toList();
    }
}
