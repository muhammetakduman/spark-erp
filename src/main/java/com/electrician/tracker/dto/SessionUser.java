package com.electrician.tracker.dto;

import com.electrician.tracker.domain.UserRole;

/** The logged-in user as every screen and service sees it (never the password hash). */
public record SessionUser(Long id, String username, String fullName, String title, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
