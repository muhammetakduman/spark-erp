package com.electrician.tracker.dto;

import com.electrician.tracker.domain.UserRole;

/** A new user as typed in the form; the password is hashed before it is stored. */
public record UserDraft(String username, String fullName, String title, UserRole role, String password,
        String passwordConfirmation) {
}
