package com.electrician.tracker.ui.util;

import java.util.concurrent.CompletableFuture;

import com.electrician.tracker.domain.ThemeMode;
import com.electrician.tracker.service.UserPreferenceService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import org.springframework.stereotype.Component;

/**
 * Applies the logged-in user's theme (light, dark or following Windows) to
 * every open window and remembers the choice per user. With "Sistem" the
 * Windows setting is checked again on every minute tick, so the program
 * follows a change without a restart. PDFs are not affected: they are always
 * printed on white.
 */
@Component
public class ThemeService {

    private final UserPreferenceService preferences;
    private final SystemThemeDetector systemTheme;
    private final ReadOnlyBooleanWrapper dark = new ReadOnlyBooleanWrapper(Stylesheets.isDark());

    public ThemeService(UserPreferenceService preferences, SystemThemeDetector systemTheme) {
        this.preferences = preferences;
        this.systemTheme = systemTheme;
    }

    /** After login (the user's choice) and on the login screen (no user: Windows setting). */
    public void applyCurrentUserTheme() {
        apply(preferences.themeMode());
    }

    public ThemeMode mode() {
        return preferences.themeMode();
    }

    public void setMode(ThemeMode mode) {
        preferences.setThemeMode(mode);
        apply(mode);
    }

    /** The sun/moon button: light ↔ dark (leaves "Sistem"). */
    public void toggle() {
        setMode(isDark() ? ThemeMode.LIGHT : ThemeMode.DARK);
    }

    public boolean isDark() {
        return dark.get();
    }

    public ReadOnlyBooleanProperty darkProperty() {
        return dark.getReadOnlyProperty();
    }

    /** Minute tick: follows a Windows dark-mode change while the theme is "Sistem". */
    public void refreshSystemTheme() {
        if (preferences.themeMode() == ThemeMode.SYSTEM) {
            CompletableFuture.supplyAsync(systemTheme::isDark)
                    .thenAccept(useDark -> Platform.runLater(() -> applyDark(useDark)));
        }
    }

    private void apply(ThemeMode mode) {
        applyDark(switch (mode) {
            case LIGHT -> false;
            case DARK -> true;
            case SYSTEM -> systemTheme.isDark();
        });
    }

    private void applyDark(boolean useDark) {
        Stylesheets.useDark(useDark);
        dark.set(useDark);
    }
}
