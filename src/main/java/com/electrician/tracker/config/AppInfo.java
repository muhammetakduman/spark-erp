package com.electrician.tracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application identity from the {@code app} section of application.yml. The
 * version is filled in from pom.xml at build time, so it is written in one
 * place only.
 */
@ConfigurationProperties(prefix = "app")
public record AppInfo(String version, String developer, int copyrightYear) {
}
