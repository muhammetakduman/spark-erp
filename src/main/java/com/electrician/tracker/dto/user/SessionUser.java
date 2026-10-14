package com.electrician.tracker.dto.user;

import com.electrician.tracker.domain.user.UserRole;

/** The logged-in user as every screen and service sees it (never the password hash). */
public record SessionUser(Long id, String username, String fullName, String title, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
