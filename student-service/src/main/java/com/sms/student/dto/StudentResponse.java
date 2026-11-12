package com.sms.student.dto;

import com.sms.common.contract.OnboardingStatus;
import com.sms.student.model.Student;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Serializable because it is stored in the Redis cache. */
public record StudentResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phone,
        OnboardingStatus onboardingStatus,
        Instant createdAt,
        Instant updatedAt) implements Serializable {

    public static StudentResponse from(Student s) {
        return new StudentResponse(s.getId(), s.getEmail(), s.getFirstName(), s.getLastName(),
                s.getDateOfBirth(), s.getPhone(), s.getOnboardingStatus(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
