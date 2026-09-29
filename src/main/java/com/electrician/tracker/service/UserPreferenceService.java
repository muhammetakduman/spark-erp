package com.electrician.tracker.service;

import java.time.LocalDate;
import java.util.Optional;

import com.electrician.tracker.domain.ThemeMode;
import com.electrician.tracker.dto.SessionUser;
import org.springframework.stereotype.Service;

/**
 * Per-user choices kept in the setting table: the colour theme (default
 * {@link ThemeMode#SYSTEM}), whether pending daily jobs are shown at login
 * (default on) and the day that reminder was last shown. Without a logged-in
 * user the defaults apply and nothing is stored.
 */
@Service
public class UserPreferenceService {

    private static final String THEME_KEY = "theme.";
    private static final String REMINDER_ENABLED_KEY = "reminder.enabled.";
    private static final String REMINDER_LAST_SHOWN_KEY = "reminder.lastShown.";

    private final SettingService settingService;
    private final AccessControl accessControl;

    public UserPreferenceService(SettingService settingService, AccessControl accessControl) {
        this.settingService = settingService;
        this.accessControl = accessControl;
    }

    public ThemeMode themeMode() {
        return userKey(THEME_KEY).flatMap(settingService::getValue).flatMap(UserPreferenceService::parseTheme)
                .orElse(ThemeMode.SYSTEM);
    }

    public void setThemeMode(ThemeMode mode) {
        userKey(THEME_KEY).ifPresent(key -> settingService.setValue(key, mode.name()));
    }

    public boolean isReminderEnabled() {
        return userKey(REMINDER_ENABLED_KEY).flatMap(settingService::getValue).map(Boolean::parseBoolean)
                .orElse(true);
    }

    public void setReminderEnabled(boolean enabled) {
        userKey(REMINDER_ENABLED_KEY).ifPresent(key -> settingService.setValue(key, String.valueOf(enabled)));
    }

    /** Once per user and day: true when the reminder is on and has not been shown today. */
    public boolean isReminderDue(LocalDate today) {
        if (!isReminderEnabled()) {
            return false;
        }
        return userKey(REMINDER_LAST_SHOWN_KEY).flatMap(settingService::getValue).map(LocalDate::parse)
                .map(lastShown -> lastShown.isBefore(today)).orElse(true);
    }

    public void markReminderShown(LocalDate today) {
        userKey(REMINDER_LAST_SHOWN_KEY).ifPresent(key -> settingService.setValue(key, today.toString()));
    }

    private Optional<String> userKey(String prefix) {
        return accessControl.currentUser().map(SessionUser::id).map(id -> prefix + id);
    }

    private static Optional<ThemeMode> parseTheme(String value) {
        try {
            return Optional.of(ThemeMode.valueOf(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
