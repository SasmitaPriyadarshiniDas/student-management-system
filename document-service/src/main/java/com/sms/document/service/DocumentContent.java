package com.sms.document.service;

import com.sms.document.dto.DocumentResponse;

import java.io.InputStream;

public record DocumentContent(DocumentResponse metadata, InputStream stream) {
}
