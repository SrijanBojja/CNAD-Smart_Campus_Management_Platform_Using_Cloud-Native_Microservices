package com.smartcampus.academic.service.impl;

import com.smartcampus.academic.dto.request.CreateCourseRequest;
import com.smartcampus.academic.dto.request.UpdateCourseRequest;
import com.smartcampus.academic.dto.request.UpdateCourseStatusRequest;
import com.smartcampus.academic.dto.response.CourseResponse;
import com.smartcampus.academic.entity.Course;
import com.smartcampus.academic.entity.CourseStatus;
import com.smartcampus.academic.exception.BadRequestException;
import com.smartcampus.academic.exception.DuplicateResourceException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.repository.CourseRepository;
import com.smartcampus.academic.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;

    public CourseServiceImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        log.info("Attempting to create course with code: {}", request.getCourseCode());

        if (courseRepository.existsByCourseCode(request.getCourseCode().trim())) {
            log.warn("Course creation failed: courseCode '{}' already exists", request.getCourseCode());
            throw new DuplicateResourceException("Course with code '" + request.getCourseCode() + "' already exists");
        }

        Course course = new Course(
                request.getCourseCode().trim().toUpperCase(),
                request.getCourseName().trim(),
                request.getDescription(),
                request.getDepartment(),
                request.getDurationYears(),
                CourseStatus.ACTIVE
        );

        Course saved = courseRepository.save(course);
        log.info("Course successfully created with ID: {}, code: {}", saved.getId(), saved.getCourseCode());

        return CourseResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        log.debug("Retrieving course by ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));
        return CourseResponse.fromEntity(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> listCourses(String department, CourseStatus status, Pageable pageable) {
        log.debug("Listing courses with department: {}, status: {}", department, status);
        Page<Course> page;
        if (department != null && !department.isBlank() && status != null) {
            page = courseRepository.findByDepartmentAndStatus(department.trim(), status, pageable);
        } else if (department != null && !department.isBlank()) {
            page = courseRepository.findByDepartment(department.trim(), pageable);
        } else if (status != null) {
            page = courseRepository.findByStatus(status, pageable);
        } else {
            page = courseRepository.findAll(pageable);
        }
        return page.map(CourseResponse::fromEntity);
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Long id, UpdateCourseRequest request) {
        log.info("Updating course with ID: {}", id);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));

        course.setCourseName(request.getCourseName().trim());
        course.setDescription(request.getDescription());
        course.setDepartment(request.getDepartment());
        course.setDurationYears(request.getDurationYears());

        Course updated = courseRepository.save(course);
        log.info("Course successfully updated with ID: {}", updated.getId());

        return CourseResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public CourseResponse updateCourseStatus(Long id, UpdateCourseStatusRequest request) {
        log.info("Updating course status for ID: {} to {}", id, request.getStatus());

        if (request.getStatus() == null) {
            throw new BadRequestException("Status is required");
        }

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));

        course.setStatus(request.getStatus());

        Course updated = courseRepository.save(course);
        log.info("Course status successfully updated for ID: {} to {}", updated.getId(), updated.getStatus());

        return CourseResponse.fromEntity(updated);
    }
}
