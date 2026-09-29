package com.electrician.tracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application identity from the {@code app} section of application.yml. The
 * name, version and description are filled in from pom.xml at build time, so
 * they are written in one place only.
 */
@ConfigurationProperties(prefix = "app")
public record AppInfo(String name, String version, String description, String developer, int copyrightYear) {

    /** "Spark ERP 2.8.3": window title, PDF footers. */
    public String nameAndVersion() {
        return name + " " + version;
    }
}
