package com.electrician.tracker.service;

import java.util.List;

import com.electrician.tracker.domain.AppUser;
import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.dto.UserDraft;
import com.electrician.tracker.repository.AppUserRepository;
import com.electrician.tracker.service.exception.AccessDeniedException;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management in Settings ("Kullanıcılar"): only an ADMIN lists, adds,
 * edits and deactivates users; everybody may change their own password. The
 * last active ADMIN can never be deactivated or demoted.
 */
@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final AccessControl accessControl;
    private final SessionService sessionService;

    public UserService(AppUserRepository userRepository, UserFactory userFactory, PasswordEncoder passwordEncoder,
            AccessControl accessControl, SessionService sessionService) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
        this.accessControl = accessControl;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public List<AppUser> findAll() {
        accessControl.requireAdmin();
        return userRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional
    public AppUser create(UserDraft draft) {
        accessControl.requireAdmin();
        return userRepository.save(userFactory.create(draft));
    }

    /** Name, title and role; demoting the last active ADMIN is refused. */
    @Transactional
    public AppUser update(Long id, String fullName, String title, UserRole role) {
        accessControl.requireAdmin();
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("error.user.fullName.required");
        }
        if (role == null) {
            throw new ValidationException("error.user.role.required");
        }
        AppUser user = findUser(id);
        if (user.isAdmin() && user.isActive() && role != UserRole.ADMIN) {
            requireAnotherActiveAdmin();
        }
        user.setFullName(fullName.trim());
        user.setTitle(UserFactory.blankToNull(title));
        user.changeRole(role);
        refreshSessionIfSelf(user);
        return user;
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        accessControl.requireAdmin();
        AppUser user = findUser(id);
        if (!active && user.isAdmin() && user.isActive()) {
            requireAnotherActiveAdmin();
        }
        user.setActive(active);
    }

    /**
     * An ADMIN may reset anybody's password; everybody else only their own,
     * and then the current password must be given.
     */
    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword, String confirmation) {
        SessionUser actor = accessControl.currentUser()
                .orElseThrow(() -> new AccessDeniedException("error.access.adminOnly"));
        boolean own = actor.id().equals(id);
        if (!own) {
            accessControl.requireAdmin();
        }
        AppUser user = findUser(id);
        if (own && (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash()))) {
            throw new ValidationException("error.user.password.currentWrong");
        }
        PasswordRules.validate(newPassword, confirmation);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    private void requireAnotherActiveAdmin() {
        if (userRepository.countByRoleAndActiveTrue(UserRole.ADMIN) <= 1) {
            throw new ValidationException("error.user.lastAdmin");
        }
    }

    private void refreshSessionIfSelf(AppUser user) {
        accessControl.currentUser()
                .filter(current -> current.id().equals(user.getId()))
                .ifPresent(current -> sessionService.start(UserFactory.toSessionUser(user)));
    }

    private AppUser findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("error.user.notFound"));
    }
}
