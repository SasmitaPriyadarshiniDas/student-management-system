package com.sms.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "sms.security")
public record SecurityProperties(String issuer, String jwkSetUri, List<String> audiences) {
}
