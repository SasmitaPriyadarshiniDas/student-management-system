package com.sms.student.service;

import com.sms.common.contract.OnboardingStatus;
import com.sms.common.web.ConflictException;
import com.sms.common.web.NotFoundException;
import com.sms.student.dto.CreateStudentRequest;
import com.sms.student.dto.StudentResponse;
import com.sms.student.dto.UpdateStudentRequest;
import com.sms.student.model.Student;
import com.sms.student.repository.StudentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StudentService {

    private static final String CACHE = "students";

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StudentResponse create(UUID id, String email, CreateStudentRequest req) {
        if (repository.existsById(id)) {
            throw new ConflictException("Student profile already exists");
        }
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A student with this email already exists");
        }
        Student s = new Student();
        s.setId(id);
        s.setEmail(email);
        s.setFirstName(req.firstName().trim());
        s.setLastName(req.lastName().trim());
        s.setDateOfBirth(req.dateOfBirth());
        s.setPhone(req.phone());
        s.setOnboardingStatus(OnboardingStatus.PENDING_DOCUMENTS);
        return StudentResponse.from(repository.save(s));
    }

    @Cacheable(value = CACHE, key = "#id")
    @Transactional(readOnly = true)
    public StudentResponse get(UUID id) {
        return StudentResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(StudentResponse::from);
    }

    @CacheEvict(value = CACHE, key = "#id")
    @Transactional
    public StudentResponse update(UUID id, UpdateStudentRequest req) {
        Student s = find(id);
        s.setFirstName(req.firstName().trim());
        s.setLastName(req.lastName().trim());
        s.setDateOfBirth(req.dateOfBirth());
        s.setPhone(req.phone());
        return StudentResponse.from(s);
    }

    @CacheEvict(value = CACHE, key = "#id")
    @Transactional
    public StudentResponse updateOnboardingStatus(UUID id, OnboardingStatus status) {
        Student s = find(id);
        s.setOnboardingStatus(status);
        return StudentResponse.from(s);
    }

    private Student find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Student not found"));
    }
}
