package com.electrician.tracker.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Pauses logins for {@link #LOCK_DURATION} after {@value #MAX_FAILED_ATTEMPTS}
 * wrong passwords in a row. A successful login resets the count.
 */
public class LoginThrottle {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofSeconds(30);

    private final Clock clock;
    private int failedAttempts;
    private Instant lockedUntil = Instant.MIN;

    public LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    /** Time left before the next attempt is allowed; zero when not locked. */
    public synchronized Duration remainingLock() {
        Duration remaining = Duration.between(clock.instant(), lockedUntil);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public synchronized void recordFailure() {
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = clock.instant().plus(LOCK_DURATION);
            failedAttempts = 0;
        }
    }

    public synchronized void recordSuccess() {
        failedAttempts = 0;
        lockedUntil = Instant.MIN;
    }
}
