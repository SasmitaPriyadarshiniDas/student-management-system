package com.sms.document.config;

import com.sms.document.model.DocumentType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "sms.documents")
public record DocumentProperties(List<DocumentType> requiredTypes, Set<String> allowedContentTypes, long maxSizeBytes) {
}
