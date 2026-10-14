package com.electrician.tracker.service.messaging;

/**
 * What the SMS provider answered. {@code code} is the provider's own code and
 * {@code detail} whatever it said beside it (a job id when it worked, the raw
 * body when the code is one this program does not recognise), so an
 * unexpected answer reaches the user instead of being swallowed.
 */
public record SmsResult(boolean success, String code, String detail) {

    public static SmsResult sent(String jobId) {
        return new SmsResult(true, "00", jobId);
    }

    public static SmsResult failed(String code, String detail) {
        return new SmsResult(false, code, detail);
    }
}
