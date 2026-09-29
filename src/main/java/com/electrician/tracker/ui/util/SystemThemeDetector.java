package com.electrician.tracker.ui.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

/**
 * Reads the Windows "app mode" (Settings → Personalization → Colors): the
 * registry value AppsUseLightTheme is 0 in dark mode. On other systems, or if
 * the value cannot be read, the light theme is assumed.
 */
@Component
public class SystemThemeDetector {

    private static final String[] QUERY = {
            "reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "/v", "AppsUseLightTheme"
    };
    private static final String DARK_VALUE = "0x0";
    private static final long TIMEOUT_SECONDS = 3;

    public boolean isDark() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return false;
        }
        try {
            Process process = new ProcessBuilder(QUERY).redirectErrorStream(true).start();
            boolean dark = readsDark(process);
            process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return dark;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean readsDark(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().filter(line -> line.contains("AppsUseLightTheme"))
                    .anyMatch(line -> line.trim().endsWith(DARK_VALUE));
        }
    }
}
