package com.sms.enrollment.service;

import com.sms.common.client.StudentServiceClient;
import com.sms.common.contract.OnboardingStatus;
import com.sms.common.web.ConflictException;
import com.sms.common.web.ForbiddenException;
import com.sms.common.web.NotFoundException;
import com.sms.enrollment.dto.EnrollmentResponse;
import com.sms.enrollment.model.Course;
import com.sms.enrollment.model.Enrollment;
import com.sms.enrollment.model.EnrollmentStatus;
import com.sms.enrollment.repository.CourseRepository;
import com.sms.enrollment.repository.EnrollmentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private final StudentServiceClient students;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final TransactionTemplate tx;

    public EnrollmentService(StudentServiceClient students, CourseRepository courses,
                             EnrollmentRepository enrollments, PlatformTransactionManager txManager) {
        this.students = students;
        this.courses = courses;
        this.enrollments = enrollments;
        // The remote call to student-service happens OUTSIDE the DB transaction (no connection held while waiting).
        this.tx = new TransactionTemplate(txManager);
    }

    @CacheEvict(value = "courses", allEntries = true)   // seat counts changed
    public EnrollmentResponse enroll(UUID studentId, UUID courseId) {
        OnboardingStatus status = students.getOnboardingStatus(studentId);
        if (status != OnboardingStatus.DOCUMENTS_VERIFIED && status != OnboardingStatus.ACTIVE) {
            throw new ConflictException("Onboarding is not complete (status: " + status
                    + "). Upload and get your documents verified before enrolling.");
        }

        return tx.execute(txStatus -> {
            Course course = courses.findById(courseId)
                    .orElseThrow(() -> new NotFoundException("Course not found"));
            if (enrollments.existsByStudentIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE)) {
                throw new ConflictException("Already enrolled in this course");
            }
            if (courses.reserveSeat(courseId) == 0) {
                throw new ConflictException("Course is full");
            }
            Enrollment e = new Enrollment();
            e.setId(UUID.randomUUID());
            e.setStudentId(studentId);
            e.setCourseId(courseId);
            e.setStatus(EnrollmentStatus.ACTIVE);
            e.setEnrolledAt(Instant.now());
            return EnrollmentResponse.from(enrollments.save(e), course);
        });
    }

    @CacheEvict(value = "courses", allEntries = true)
    public EnrollmentResponse cancel(UUID enrollmentId, UUID requesterId, boolean admin) {
        return tx.execute(txStatus -> {
            Enrollment e = enrollments.findById(enrollmentId)
                    .orElseThrow(() -> new NotFoundException("Enrollment not found"));
            if (!admin && !e.getStudentId().equals(requesterId)) {
                throw new ForbiddenException("You are not allowed to cancel this enrollment");
            }
            if (e.getStatus() == EnrollmentStatus.ACTIVE) {
                e.setStatus(EnrollmentStatus.CANCELLED);
                e.setCancelledAt(Instant.now());
                enrollments.save(e);
                courses.releaseSeat(e.getCourseId());
            }
            return EnrollmentResponse.from(e, courses.findById(e.getCourseId()).orElse(null));
        });
    }

    public List<EnrollmentResponse> listForStudent(UUID studentId) {
        List<Enrollment> mine = enrollments.findByStudentIdOrderByEnrolledAtDesc(studentId);
        Map<UUID, Course> byId = courses.findAllById(mine.stream().map(Enrollment::getCourseId).distinct().toList())
                .stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        return mine.stream().map(e -> EnrollmentResponse.from(e, byId.get(e.getCourseId()))).toList();
    }
}
