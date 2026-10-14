package com.electrician.tracker.service.messaging;

/**
 * Sends one text message through whichever provider the user has an account
 * with. Implementations talk to that provider over HTTP and never throw for a
 * refusal: a provider saying no is an answer, not a program fault.
 */
public interface SmsGateway {

    /** True once the user has entered everything the provider needs. */
    boolean isConfigured();

    /**
     * @param phone   international digits, as {@code PhoneNumbers} produces them
     * @param message the text, already composed
     */
    SmsResult send(String phone, String message);
}
