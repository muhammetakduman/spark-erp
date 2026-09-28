package com.electrician.tracker.service;

import java.time.Clock;
import java.time.LocalDateTime;

import com.electrician.tracker.domain.AppUser;
import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.dto.UserDraft;
import com.electrician.tracker.repository.AppUserRepository;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Validates a new user (unique username, required fields, password rules)
 * and builds it with a BCrypt hash, for both the first-run setup and the
 * user management screen.
 */
@Component
public class UserFactory {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserFactory(AppUserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public AppUser create(UserDraft draft) {
        String username = trimmed(draft.username());
        if (username.isEmpty()) {
            throw new ValidationException("error.user.username.required");
        }
        if (draft.fullName() == null || draft.fullName().isBlank()) {
            throw new ValidationException("error.user.fullName.required");
        }
        if (draft.role() == null) {
            throw new ValidationException("error.user.role.required");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ValidationException("error.user.username.duplicate");
        }
        PasswordRules.validate(draft.password(), draft.passwordConfirmation());
        return new AppUser(username, passwordEncoder.encode(draft.password()), draft.fullName().trim(),
                blankToNull(draft.title()), draft.role(), LocalDateTime.now(clock));
    }

    public static SessionUser toSessionUser(AppUser user) {
        return new SessionUser(user.getId(), user.getUsername(), user.getFullName(), user.getTitle(), user.getRole());
    }

    static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    static String trimmed(String text) {
        return text == null ? "" : text.trim();
    }
}
