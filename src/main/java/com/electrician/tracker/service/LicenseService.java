package com.electrician.tracker.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

import com.electrician.tracker.config.AppInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The end-user licence: its text and whether the user accepted it. Acceptance
 * is stored per version, so a new release asks again (its terms may change).
 */
@Service
public class LicenseService {

    static final String ACCEPTED_VERSION_KEY = "license.accepted.version";
    static final String ACCEPTED_AT_KEY = "license.accepted.at";
    private static final String LICENSE_RESOURCE = "/license/LICENSE.txt";

    private final SettingService settingService;
    private final AppInfo appInfo;

    public LicenseService(SettingService settingService, AppInfo appInfo) {
        this.settingService = settingService;
        this.appInfo = appInfo;
    }

    public boolean isAccepted() {
        return settingService.getValue(ACCEPTED_VERSION_KEY)
                .filter(appInfo.version()::equals)
                .isPresent();
    }

    @Transactional
    public void accept() {
        settingService.setValue(ACCEPTED_VERSION_KEY, appInfo.version());
        settingService.setValue(ACCEPTED_AT_KEY, LocalDateTime.now().toString());
    }

    public String licenseText() {
        try (InputStream in = Objects.requireNonNull(getClass().getResourceAsStream(LICENSE_RESOURCE),
                LICENSE_RESOURCE)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
