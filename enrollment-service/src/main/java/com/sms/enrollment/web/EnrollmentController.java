package com.sms.enrollment.web;

import com.sms.common.security.AuthUser;
import com.sms.enrollment.dto.EnrollRequest;
import com.sms.enrollment.dto.EnrollmentResponse;
import com.sms.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EnrollRequest req) {
        return service.enroll(AuthUser.id(jwt), req.courseId());
    }

    @GetMapping("/me")
    public List<EnrollmentResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.listForStudent(AuthUser.id(jwt));
    }

    /** Cancels (soft) an enrollment and frees the seat. Owners and admins only. */
    @DeleteMapping("/{id}")
    public EnrollmentResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.cancel(id, AuthUser.id(jwt), AuthUser.isAdmin(jwt));
    }
}
