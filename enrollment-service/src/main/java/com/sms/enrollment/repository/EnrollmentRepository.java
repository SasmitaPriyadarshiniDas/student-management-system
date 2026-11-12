package com.sms.enrollment.repository;

import com.sms.enrollment.model.Enrollment;
import com.sms.enrollment.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByStudentIdAndCourseIdAndStatus(UUID studentId, UUID courseId, EnrollmentStatus status);

    List<Enrollment> findByStudentIdOrderByEnrolledAtDesc(UUID studentId);
}
