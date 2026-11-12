package com.sms.enrollment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private int capacity;

    /** Changed only through CourseRepository.reserveSeat / releaseSeat (atomic UPDATEs). */
    @Column(name = "enrolled_count", nullable = false)
    private int enrolledCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
