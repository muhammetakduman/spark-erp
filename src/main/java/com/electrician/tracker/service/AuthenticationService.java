package com.electrician.tracker.service;

import java.time.Clock;
import java.time.Duration;

import com.electrician.tracker.domain.AppUser;
import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.dto.UserDraft;
import com.electrician.tracker.repository.AppUserRepository;
import com.electrician.tracker.service.exception.LoginBlockedException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * First-run setup (the very first user is always an ADMIN) and login. Only
 * BCrypt hashes are stored; after five wrong passwords logins pause for 30
 * seconds.
 */
@Service
public class AuthenticationService {

    private final AppUserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final LoginThrottle throttle;

    public AuthenticationService(AppUserRepository userRepository, UserFactory userFactory,
            PasswordEncoder passwordEncoder, SessionService sessionService, Clock clock) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.throttle = new LoginThrottle(clock);
    }

    /** No user exists yet: the setup screen must create the first administrator. */
    @Transactional(readOnly = true)
    public boolean isSetupRequired() {
        return userRepository.count() == 0;
    }

    /** Creates the first user as ADMIN and logs them in. */
    @Transactional
    public SessionUser setUpFirstAdmin(String username, String fullName, String title, String password,
            String passwordConfirmation) {
        if (!isSetupRequired()) {
            throw new ValidationException("error.setup.alreadyDone");
        }
        UserDraft draft = new UserDraft(username, fullName, title, UserRole.ADMIN, password, passwordConfirmation);
        return startSession(userRepository.save(userFactory.create(draft)));
    }

    @Transactional(readOnly = true)
    public SessionUser login(String username, String password) {
        Duration locked = throttle.remainingLock();
        if (!locked.isZero()) {
            throw new LoginBlockedException("error.login.blocked", Math.max(1, locked.toSeconds()));
        }
        AppUser user = userRepository.findByUsernameIgnoreCase(UserFactory.trimmed(username))
                .filter(AppUser::isActive)
                .filter(candidate -> password != null && passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            throttle.recordFailure();
            throw new ValidationException("error.login.invalid");
        }
        throttle.recordSuccess();
        return startSession(user);
    }

    public void logout() {
        sessionService.end();
    }

    private SessionUser startSession(AppUser user) {
        SessionUser sessionUser = UserFactory.toSessionUser(user);
        sessionService.start(sessionUser);
        return sessionUser;
    }
}
