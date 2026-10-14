package com.electrician.tracker.service.messaging;

import java.util.Optional;

import com.electrician.tracker.service.SettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The user's own SMS provider account, kept in the settings table.
 *
 * <p>Each installation buys its own message credit and types its own
 * credentials here, so no shared secret ever travels inside the program and
 * nothing has to be sent through a server of ours. The password sits in the
 * local database like every other setting; the database is the user's own file
 * on their own machine, and nothing else in it is encrypted either.
 *
 * <p>The base address is a setting rather than a constant so a provider that
 * moves its endpoint can be followed without a new release.
 */
@Service
public class SmsSettings {

    public static final String DEFAULT_BASE_URL = "https://api.netgsm.com.tr";

    private static final String BASE_URL = "sms.provider.baseUrl";
    private static final String USERNAME = "sms.provider.username";
    private static final String PASSWORD = "sms.provider.password";
    private static final String HEADER = "sms.provider.header";

    private final SettingService settingService;

    public SmsSettings(SettingService settingService) {
        this.settingService = settingService;
    }

    @Transactional(readOnly = true)
    public String baseUrl() {
        return nonBlank(BASE_URL).orElse(DEFAULT_BASE_URL);
    }

    @Transactional(readOnly = true)
    public String username() {
        return nonBlank(USERNAME).orElse("");
    }

    @Transactional(readOnly = true)
    public String password() {
        return nonBlank(PASSWORD).orElse("");
    }

    /** The sender title the provider has approved ("AYZEN GRUP"). */
    @Transactional(readOnly = true)
    public String header() {
        return nonBlank(HEADER).orElse("");
    }

    /** Without all three the provider will refuse anyway, so the button stays off. */
    @Transactional(readOnly = true)
    public boolean isConfigured() {
        return !username().isEmpty() && !password().isEmpty() && !header().isEmpty();
    }

    @Transactional
    public void save(String baseUrl, String username, String password, String header) {
        settingService.setValue(BASE_URL, trimmed(baseUrl));
        settingService.setValue(USERNAME, trimmed(username));
        settingService.setValue(PASSWORD, password == null ? "" : password.trim());
        settingService.setValue(HEADER, trimmed(header));
    }

    private Optional<String> nonBlank(String key) {
        return settingService.getValue(key).map(String::trim).filter(value -> !value.isEmpty());
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }
}
