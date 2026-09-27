package com.electrician.tracker.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.electrician.tracker")
@EnableConfigurationProperties(AppInfo.class)
@EnableJpaRepositories(basePackages = "com.electrician.tracker.repository")
@EntityScan(basePackages = "com.electrician.tracker.domain")
public class SpringConfig {
}
