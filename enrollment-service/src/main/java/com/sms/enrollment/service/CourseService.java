package com.sms.enrollment.service;

import com.sms.common.web.BadRequestException;
import com.sms.common.web.ConflictException;
import com.sms.common.web.NotFoundException;
import com.sms.enrollment.dto.CourseRequest;
import com.sms.enrollment.dto.CourseResponse;
import com.sms.enrollment.model.Course;
import com.sms.enrollment.repository.CourseRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CourseService {

    private static final String CACHE = "courses";

    private final CourseRepository repository;

    public CourseService(CourseRepository repository) {
        this.repository = repository;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional
    public CourseResponse create(CourseRequest req) {
        String code = req.code().trim().toUpperCase();
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Course code already exists");
        }
        Course c = new Course();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        c.setTitle(req.title().trim());
        c.setDescription(req.description());
        c.setCapacity(req.capacity());
        c.setEnrolledCount(0);
        c.setCreatedAt(Instant.now());
        return CourseResponse.from(repository.save(c));
    }

    @Cacheable(value = CACHE, key = "'all'")
    @Transactional(readOnly = true)
    public List<CourseResponse> list() {
        List<CourseResponse> result = new ArrayList<>();
        repository.findAll().stream()
                .sorted(Comparator.comparing(Course::getCode))
                .forEach(c -> result.add(CourseResponse.from(c)));
        return result;
    }

    @Cacheable(value = CACHE, key = "#id")
    @Transactional(readOnly = true)
    public CourseResponse get(UUID id) {
        return CourseResponse.from(find(id));
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional
    public CourseResponse update(UUID id, CourseRequest req) {
        Course c = find(id);
        if (req.capacity() < c.getEnrolledCount()) {
            throw new BadRequestException("Capacity cannot be lower than the " + c.getEnrolledCount()
                    + " students already enrolled");
        }
        c.setTitle(req.title().trim());
        c.setDescription(req.description());
        c.setCapacity(req.capacity());
        return CourseResponse.from(c);
    }

    private Course find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Course not found"));
    }
}
