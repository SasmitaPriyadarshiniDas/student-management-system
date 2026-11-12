package com.sms.enrollment.service;

import com.sms.common.client.StudentServiceClient;
import com.sms.common.contract.OnboardingStatus;
import com.sms.common.web.ConflictException;
import com.sms.enrollment.dto.EnrollmentResponse;
import com.sms.enrollment.model.Course;
import com.sms.enrollment.model.Enrollment;
import com.sms.enrollment.model.EnrollmentStatus;
import com.sms.enrollment.repository.CourseRepository;
import com.sms.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock StudentServiceClient students;
    @Mock CourseRepository courses;
    @Mock EnrollmentRepository enrollments;
    @Mock PlatformTransactionManager txManager;

    EnrollmentService service;

    final UUID studentId = UUID.randomUUID();
    final UUID courseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(students, courses, enrollments, txManager);
    }

    private Course course() {
        Course c = new Course();
        c.setId(courseId);
        c.setCode("CS101");
        c.setTitle("Intro to CS");
        c.setCapacity(30);
        return c;
    }

    @Test
    void rejectsStudentsWhoHaveNotFinishedOnboarding() {
        when(students.getOnboardingStatus(studentId)).thenReturn(OnboardingStatus.PENDING_DOCUMENTS);

        assertThatThrownBy(() -> service.enroll(studentId, courseId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Onboarding");
        verifyNoInteractions(courses, enrollments);
    }

    @Test
    void rejectsWhenCourseIsFull() {
        when(students.getOnboardingStatus(studentId)).thenReturn(OnboardingStatus.DOCUMENTS_VERIFIED);
        when(courses.findById(courseId)).thenReturn(Optional.of(course()));
        when(courses.reserveSeat(courseId)).thenReturn(0);

        assertThatThrownBy(() -> service.enroll(studentId, courseId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("full");
    }

    @Test
    void enrollsVerifiedStudentWhenSeatIsAvailable() {
        when(students.getOnboardingStatus(studentId)).thenReturn(OnboardingStatus.DOCUMENTS_VERIFIED);
        when(courses.findById(courseId)).thenReturn(Optional.of(course()));
        when(courses.reserveSeat(courseId)).thenReturn(1);
        when(enrollments.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse result = service.enroll(studentId, courseId);

        assertThat(result.studentId()).isEqualTo(studentId);
        assertThat(result.courseId()).isEqualTo(courseId);
        assertThat(result.courseCode()).isEqualTo("CS101");
        assertThat(result.status()).isEqualTo(EnrollmentStatus.ACTIVE);
    }
}
