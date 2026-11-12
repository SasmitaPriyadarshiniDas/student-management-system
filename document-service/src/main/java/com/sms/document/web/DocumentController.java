package com.sms.document.web;

import com.sms.common.security.AuthUser;
import com.sms.document.dto.DocumentResponse;
import com.sms.document.dto.RejectDocumentRequest;
import com.sms.document.model.DocumentType;
import com.sms.document.service.DocumentContent;
import com.sms.document.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    /** multipart/form-data: type=ID_PROOF|TRANSCRIPT|PHOTO|ADDRESS_PROOF, file=<binary> */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam DocumentType type,
                                   @RequestParam("file") MultipartFile file) {
        return service.upload(AuthUser.id(jwt), type, file);
    }

    @GetMapping("/me")
    public List<DocumentResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.listForStudent(AuthUser.id(jwt));
    }

    @GetMapping("/student/{studentId}")
    public List<DocumentResponse> forStudent(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID studentId) {
        AuthUser.requireSelfOrAdmin(jwt, studentId);
        return service.listForStudent(studentId);
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        DocumentResponse doc = service.get(id);
        AuthUser.requireSelfOrAdmin(jwt, doc.studentId());
        return doc;
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> content(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        DocumentContent content = service.download(id, AuthUser.id(jwt), AuthUser.isAdmin(jwt));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(content.metadata().contentType()));
        headers.setContentLength(content.metadata().sizeBytes());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(content.metadata().fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(content.stream()));
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentResponse verify(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.verify(id, AuthUser.id(jwt));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Valid @RequestBody RejectDocumentRequest req) {
        return service.reject(id, AuthUser.id(jwt), req.reason());
    }
}
