package com.electrician.tracker.service;

import static org.mockito.Mockito.mock;

import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.dto.SessionUser;
import jakarta.persistence.EntityManager;

/** Logged-in sessions and a masker for unit tests that build services by hand. */
public final class TestAccess {

    private TestAccess() {
    }

    public static SessionService admin() {
        return session(UserRole.ADMIN);
    }

    public static SessionService manager() {
        return session(UserRole.MANAGER);
    }

    public static FinancialDataMasker masker(AccessControl accessControl) {
        return new FinancialDataMasker(accessControl, mock(EntityManager.class));
    }

    private static SessionService session(UserRole role) {
        SessionService session = new SessionService();
        session.start(new SessionUser(1L, "test", "Test Kullanıcı", null, role));
        return session;
    }
}
