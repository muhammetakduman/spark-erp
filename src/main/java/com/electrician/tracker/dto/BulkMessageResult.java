package com.electrician.tracker.dto;

import java.util.List;

/**
 * What came of sending one message to a whole list: how many left, and the
 * names behind every one that did not, so the user can chase those by hand.
 *
 * <p>{@code unreadable} are the records whose phone number could not be read
 * at all; they never reached the provider and cost nothing. {@code refused}
 * are the ones the provider turned down, each with its reason.
 */
public record BulkMessageResult(int sent, List<String> unreadable, List<Failure> refused) {

    /** One recipient the provider refused, with the message to show for it. */
    public record Failure(String name, String reason) {
    }

    public int attempted() {
        return sent + refused.size();
    }

    public boolean isCompleteSuccess() {
        return refused.isEmpty() && unreadable.isEmpty();
    }
}
