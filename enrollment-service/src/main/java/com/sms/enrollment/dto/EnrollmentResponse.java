package com.sms.enrollment.dto;

import com.sms.enrollment.model.Course;
import com.sms.enrollment.model.Enrollment;
import com.sms.enrollment.model.EnrollmentStatus;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID studentId,
        UUID courseId,
        String courseCode,
        String courseTitle,
        EnrollmentStatus status,
        Instant enrolledAt,
        Instant cancelledAt) {

    public static EnrollmentResponse from(Enrollment e, Course c) {
        return new EnrollmentResponse(e.getId(), e.getStudentId(), e.getCourseId(),
                c == null ? null : c.getCode(), c == null ? null : c.getTitle(),
                e.getStatus(), e.getEnrolledAt(), e.getCancelledAt());
    }
}
