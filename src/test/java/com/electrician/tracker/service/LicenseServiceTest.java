package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.electrician.tracker.config.AppInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LicenseServiceTest {

    private SettingService settingService;
    private LicenseService licenseService;

    @BeforeEach
    void setUp() {
        settingService = mock(SettingService.class);
        licenseService = new LicenseService(settingService, new AppInfo("1.0.0", "Muhammet Akduman", 2026));
    }

    @Test
    void notAcceptedOnFirstRun() {
        when(settingService.getValue(LicenseService.ACCEPTED_VERSION_KEY)).thenReturn(Optional.empty());

        assertThat(licenseService.isAccepted()).isFalse();
    }

    @Test
    void acceptedForTheCurrentVersionOnly() {
        when(settingService.getValue(LicenseService.ACCEPTED_VERSION_KEY)).thenReturn(Optional.of("1.0.0"));
        assertThat(licenseService.isAccepted()).isTrue();

        when(settingService.getValue(LicenseService.ACCEPTED_VERSION_KEY)).thenReturn(Optional.of("0.9.0"));
        assertThat(licenseService.isAccepted()).isFalse();
    }

    @Test
    void acceptingStoresVersionAndTime() {
        licenseService.accept();

        verify(settingService).setValue(LicenseService.ACCEPTED_VERSION_KEY, "1.0.0");
        verify(settingService).setValue(eq(LicenseService.ACCEPTED_AT_KEY), anyString());
    }

    @Test
    void readsTheLicenceShippedWithTheApp() {
        assertThat(licenseService.licenseText())
                .contains("Muhammet Akduman")
                .contains("Tüm hakları saklıdır")
                .contains("yedeklenmesinden");
    }
}
