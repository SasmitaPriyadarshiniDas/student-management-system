package com.sms.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sms.storage")
public record StorageProperties(String connectionString, String endpoint, String container, boolean createContainer) {
}
