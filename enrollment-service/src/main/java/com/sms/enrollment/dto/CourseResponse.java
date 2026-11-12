package com.sms.enrollment.dto;

import com.sms.enrollment.model.Course;

import java.io.Serializable;
import java.util.UUID;

/** Serializable because it is stored in the Redis cache. */
public record CourseResponse(
        UUID id,
        String code,
        String title,
        String description,
        int capacity,
        int enrolledCount,
        int seatsAvailable) implements Serializable {

    public static CourseResponse from(Course c) {
        return new CourseResponse(c.getId(), c.getCode(), c.getTitle(), c.getDescription(),
                c.getCapacity(), c.getEnrolledCount(), c.getCapacity() - c.getEnrolledCount());
    }
}
