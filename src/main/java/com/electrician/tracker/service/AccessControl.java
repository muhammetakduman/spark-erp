package com.electrician.tracker.service;

import java.util.Optional;

import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.SessionUser;

/**
 * What the logged-in user may see and do. Services ask this before returning
 * money figures (so a MANAGER never receives them, whatever the screen shows)
 * and before admin-only operations. Sites (and everything about them) are
 * admin-only; services are open to both roles.
 */
public interface AccessControl {

    Optional<SessionUser> currentUser();

    boolean isAdmin();

    /** Cost, profit, receivables, payments, purchase prices, suppliers and wages. */
    boolean canViewFinancials();

    /** @throws com.electrician.tracker.service.exception.AccessDeniedException unless an ADMIN is logged in */
    void requireAdmin();

    /** Whether the logged-in user may open jobs of this type (sites only for an ADMIN). */
    default boolean canAccess(JobType type) {
        return type != JobType.SITE || isAdmin();
    }

    /** @throws com.electrician.tracker.service.exception.AccessDeniedException for a site unless ADMIN */
    default void requireAccess(JobType type) {
        if (!canAccess(type)) {
            requireAdmin();
        }
    }
}
