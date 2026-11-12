package com.sms.document.service;

import com.sms.common.client.StudentServiceClient;
import com.sms.common.contract.OnboardingStatus;
import com.sms.common.security.AuthUser;
import com.sms.common.web.BadRequestException;
import com.sms.common.web.ForbiddenException;
import com.sms.common.web.NotFoundException;
import com.sms.common.web.UpstreamException;
import com.sms.document.config.DocumentProperties;
import com.sms.document.dto.DocumentResponse;
import com.sms.document.model.Document;
import com.sms.document.model.DocumentStatus;
import com.sms.document.model.DocumentType;
import com.sms.document.repository.DocumentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final String CACHE = "documents";

    private final DocumentRepository repository;
    private final BlobStorageService blobs;
    private final StudentServiceClient students;
    private final DocumentProperties props;

    public DocumentService(DocumentRepository repository, BlobStorageService blobs,
                           StudentServiceClient students, DocumentProperties props) {
        this.repository = repository;
        this.blobs = blobs;
        this.students = students;
        this.props = props;
    }

    // ---------- upload ----------

    public DocumentResponse upload(UUID studentId, DocumentType type, MultipartFile file) {
        validate(file);
        students.getOnboardingStatus(studentId);   // 404 if the student has not created a profile yet

        UUID id = UUID.randomUUID();
        String fileName = sanitize(file.getOriginalFilename());
        String blobName = studentId + "/" + id + "/" + fileName;

        try (InputStream in = file.getInputStream()) {
            blobs.upload(blobName, in, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new UpstreamException("Could not read the uploaded file", e);
        }

        Document d = new Document();
        d.setId(id);
        d.setStudentId(studentId);
        d.setType(type);
        d.setFileName(fileName);
        d.setContentType(file.getContentType());
        d.setSizeBytes(file.getSize());
        d.setBlobName(blobName);
        d.setStatus(DocumentStatus.UPLOADED);
        d.setUploadedAt(Instant.now());
        try {
            return DocumentResponse.from(repository.save(d));
        } catch (RuntimeException e) {
            blobs.deleteQuietly(blobName);          // don't leave orphaned blobs behind
            throw e;
        }
    }

    // ---------- reads ----------

    public List<DocumentResponse> listForStudent(UUID studentId) {
        return repository.findByStudentIdOrderByUploadedAtDesc(studentId).stream()
                .map(DocumentResponse::from).toList();
    }

    @Cacheable(value = CACHE, key = "#id")
    public DocumentResponse get(UUID id) {
        return DocumentResponse.from(find(id));
    }

    public DocumentContent download(UUID id, UUID requesterId, boolean admin) {
        Document d = find(id);
        if (!admin && !d.getStudentId().equals(requesterId)) {
            throw new ForbiddenException("You are not allowed to access this document");
        }
        return new DocumentContent(DocumentResponse.from(d), blobs.open(d.getBlobName()));
    }

    // ---------- review (admin) ----------

    @CacheEvict(value = CACHE, key = "#id")
    public DocumentResponse verify(UUID id, UUID adminId) {
        Document d = find(id);
        if (d.getStatus() != DocumentStatus.VERIFIED) {
            d.setStatus(DocumentStatus.VERIFIED);
            d.setRejectionReason(null);
            d.setVerifiedBy(adminId);
            d.setVerifiedAt(Instant.now());
            repository.save(d);
        }
        // Always re-check, so an admin can safely retry if the student-service call failed earlier.
        promoteStudentIfComplete(d.getStudentId());
        return DocumentResponse.from(d);
    }

    @CacheEvict(value = CACHE, key = "#id")
    public DocumentResponse reject(UUID id, UUID adminId, String reason) {
        Document d = find(id);
        d.setStatus(DocumentStatus.REJECTED);
        d.setRejectionReason(reason);
        d.setVerifiedBy(adminId);
        d.setVerifiedAt(Instant.now());
        return DocumentResponse.from(repository.save(d));
    }

    // ---------- helpers ----------

    private void promoteStudentIfComplete(UUID studentId) {
        Set<DocumentType> verified = new HashSet<>(repository.findDistinctTypes(studentId, DocumentStatus.VERIFIED));
        if (verified.containsAll(props.requiredTypes())
                && students.getOnboardingStatus(studentId) == OnboardingStatus.PENDING_DOCUMENTS) {
            students.updateOnboardingStatus(studentId, OnboardingStatus.DOCUMENTS_VERIFIED);
        }
    }

    private Document find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Document not found"));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > props.maxSizeBytes()) {
            throw new BadRequestException("File is larger than " + props.maxSizeBytes() + " bytes");
        }
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!props.allowedContentTypes().contains(ct)) {
            throw new BadRequestException("Unsupported file type. Allowed: " + props.allowedContentTypes());
        }
    }

    private static String sanitize(String original) {
        if (original == null || original.isBlank()) {
            return "file";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.length() > 120) {
            name = name.substring(name.length() - 120);
        }
        return name.isBlank() ? "file" : name;
    }
}
