package com.sms.student.web;

import com.sms.common.contract.OnboardingStatusResponse;
import com.sms.common.contract.UpdateOnboardingStatusRequest;
import com.sms.common.security.AuthUser;
import com.sms.common.web.BadRequestException;
import com.sms.common.web.PageResponse;
import com.sms.student.dto.CreateStudentRequest;
import com.sms.student.dto.StudentResponse;
import com.sms.student.dto.UpdateStudentRequest;
import com.sms.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    /** Creates the caller's own profile; the student id is the caller's Entra object id. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateStudentRequest req) {
        String email = AuthUser.email(jwt);
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Access token has no email / preferred_username claim");
        }
        return service.create(AuthUser.id(jwt), email, req);
    }

    @GetMapping("/me")
    public StudentResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.get(AuthUser.id(jwt));
    }

    @PutMapping("/me")
    public StudentResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateStudentRequest req) {
        return service.update(AuthUser.id(jwt), req);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<StudentResponse> list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("lastName", "firstName"));
        return PageResponse.of(service.list(pageable));
    }

    @GetMapping("/{id}")
    public StudentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        AuthUser.requireSelfOrAdmin(jwt, id);
        return service.get(id);
    }

    /** Used by enrollment-service (caller = the student) and document-service (caller = an admin). */
    @GetMapping("/{id}/onboarding-status")
    public OnboardingStatusResponse onboardingStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        AuthUser.requireSelfOrAdmin(jwt, id);
        return new OnboardingStatusResponse(id, service.get(id).onboardingStatus());
    }

    @PutMapping("/{id}/onboarding-status")
    @PreAuthorize("hasRole('ADMIN')")
    public OnboardingStatusResponse updateOnboardingStatus(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateOnboardingStatusRequest req) {
        return new OnboardingStatusResponse(id, service.updateOnboardingStatus(id, req.status()).onboardingStatus());
    }
}
