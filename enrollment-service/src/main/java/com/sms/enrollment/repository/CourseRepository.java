package com.sms.enrollment.repository;

import com.sms.enrollment.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    /**
     * Atomically takes a seat. Returns 0 when the course is full, so two students can never
     * grab the last seat at the same time (no lost updates, no overselling).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Course c set c.enrolledCount = c.enrolledCount + 1 " +
            "where c.id = :id and c.enrolledCount < c.capacity")
    int reserveSeat(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Course c set c.enrolledCount = c.enrolledCount - 1 " +
            "where c.id = :id and c.enrolledCount > 0")
    int releaseSeat(@Param("id") UUID id);
}
