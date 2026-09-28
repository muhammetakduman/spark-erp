package com.electrician.tracker.service;

import com.electrician.tracker.service.exception.ValidationException;

/** Rules every new password must meet, whoever sets it. */
final class PasswordRules {

    static final int MIN_LENGTH = 6;

    private PasswordRules() {
    }

    static void validate(String password, String confirmation) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("error.user.password.required");
        }
        if (password.length() < MIN_LENGTH) {
            throw new ValidationException("error.user.password.tooShort");
        }
        if (!password.equals(confirmation)) {
            throw new ValidationException("error.user.password.mismatch");
        }
    }
}
