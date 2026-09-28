package com.electrician.tracker.service.exception;

/**
 * Thrown while logins are paused after too many wrong passwords. The message
 * is a resource bundle key whose {0} receives {@link #getRemainingSeconds()}.
 */
public class LoginBlockedException extends RuntimeException {

    private final long remainingSeconds;

    public LoginBlockedException(String messageKey, long remainingSeconds) {
        super(messageKey);
        this.remainingSeconds = remainingSeconds;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
