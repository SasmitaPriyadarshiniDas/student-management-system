package com.sms.document.dto;

import com.sms.document.model.Document;
import com.sms.document.model.DocumentStatus;
import com.sms.document.model.DocumentType;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** Serializable because it is stored in the Redis cache. The blob name is deliberately not exposed. */
public record DocumentResponse(
        UUID id,
        UUID studentId,
        DocumentType type,
        String fileName,
        String contentType,
        long sizeBytes,
        DocumentStatus status,
        String rejectionReason,
        Instant uploadedAt,
        Instant verifiedAt) implements Serializable {

    public static DocumentResponse from(Document d) {
        return new DocumentResponse(d.getId(), d.getStudentId(), d.getType(), d.getFileName(), d.getContentType(),
                d.getSizeBytes(), d.getStatus(), d.getRejectionReason(), d.getUploadedAt(), d.getVerifiedAt());
    }
}
