package com.electrician.tracker.service;

import java.util.Optional;

import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.service.exception.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Holds the user logged in to this desktop session ("OturumServisi"). Nobody
 * logged in means no rights at all. Read from background tasks too, hence
 * the volatile field.
 */
@Service
public class SessionService implements AccessControl {

    private volatile SessionUser currentUser;

    public void start(SessionUser user) {
        this.currentUser = user;
    }

    public void end() {
        this.currentUser = null;
    }

    @Override
    public Optional<SessionUser> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    @Override
    public boolean isAdmin() {
        SessionUser user = currentUser;
        return user != null && user.isAdmin();
    }

    @Override
    public boolean canViewFinancials() {
        return isAdmin();
    }

    @Override
    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("error.access.adminOnly");
        }
    }
}
