package com.smartcampus.academic.service.impl;

import com.smartcampus.academic.client.StudentServiceClient;
import com.smartcampus.academic.client.dto.StudentResponseDto;
import com.smartcampus.academic.dto.request.CreateEnrollmentRequest;
import com.smartcampus.academic.dto.request.UpdateEnrollmentRequest;
import com.smartcampus.academic.dto.response.EnrollmentResponse;
import com.smartcampus.academic.entity.Course;
import com.smartcampus.academic.entity.CourseEnrollment;
import com.smartcampus.academic.entity.EnrollmentStatus;
import com.smartcampus.academic.exception.DuplicateResourceException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.repository.CourseEnrollmentRepository;
import com.smartcampus.academic.repository.CourseRepository;
import com.smartcampus.academic.security.UserPrincipal;
import com.smartcampus.academic.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentServiceClient studentServiceClient;

    public EnrollmentServiceImpl(CourseEnrollmentRepository enrollmentRepository,
                                 CourseRepository courseRepository,
                                 StudentServiceClient studentServiceClient) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.studentServiceClient = studentServiceClient;
    }

    @Override
    @Transactional
    public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request, String bearerToken) {
        log.info("Attempting to create enrollment for courseId: {}, studentId: {}, academicYear: {}, semester: {}",
                request.getCourseId(), request.getStudentId(), request.getAcademicYear(), request.getSemester());

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + request.getCourseId()));

        // Cross-service validation: verify student exists in Student Service
        StudentResponseDto student = studentServiceClient.getStudentById(request.getStudentId(), bearerToken);
        if (student == null) {
            throw new ResourceNotFoundException("Student not found with ID: " + request.getStudentId());
        }

        if (enrollmentRepository.existsByCourseIdAndStudentIdAndAcademicYearAndSemester(
                request.getCourseId(), request.getStudentId(), request.getAcademicYear().trim(), request.getSemester())) {
            log.warn("Enrollment creation failed: duplicate enrollment for studentId {} in courseId {}",
                    request.getStudentId(), request.getCourseId());
            throw new DuplicateResourceException("Student is already enrolled in this course for academic year "
                    + request.getAcademicYear() + ", semester " + request.getSemester());
        }

        CourseEnrollment enrollment = new CourseEnrollment(
                course,
                request.getStudentId(),
                request.getAcademicYear().trim(),
                request.getSemester(),
                request.getStatus() != null ? request.getStatus() : EnrollmentStatus.ACTIVE
        );

        CourseEnrollment saved = enrollmentRepository.save(enrollment);
        log.info("Enrollment successfully created with ID: {}", saved.getId());

        return EnrollmentResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Long id, UserPrincipal principal, String bearerToken) {
        log.debug("Retrieving enrollment by ID: {}", id);

        CourseEnrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course enrollment not found with ID: " + id));

        // Object-level authorization for STUDENT role
        if (principal != null) {
            boolean isAdminOrFaculty = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_FACULTY".equals(role));

            if (!isAdminOrFaculty) {
                // Caller is a STUDENT
                StudentResponseDto student = studentServiceClient.getStudentById(enrollment.getStudentId(), bearerToken);
                if (student == null || !student.getUserId().equals(principal.getUserId())) {
                    log.warn("Access denied for student userId {} attempting to access enrollment ID {} of studentId {}",
                            principal.getUserId(), enrollment.getId(), enrollment.getStudentId());
                    throw new AccessDeniedException("Access denied: You are only permitted to access your own course enrollment");
                }
            }
        }

        return EnrollmentResponse.fromEntity(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> listEnrollments(Long studentId, Long courseId, String academicYear, Integer semester, Pageable pageable) {
        log.debug("Listing enrollments with studentId: {}, courseId: {}, academicYear: {}, semester: {}",
                studentId, courseId, academicYear, semester);
        Page<CourseEnrollment> page;
        if (studentId != null) {
            page = enrollmentRepository.findByStudentId(studentId, pageable);
        } else if (courseId != null && academicYear != null && semester != null) {
            page = enrollmentRepository.findByCourseIdAndAcademicYearAndSemester(courseId, academicYear.trim(), semester, pageable);
        } else if (courseId != null) {
            page = enrollmentRepository.findByCourseId(courseId, pageable);
        } else if (academicYear != null && semester != null) {
            page = enrollmentRepository.findByAcademicYearAndSemester(academicYear.trim(), semester, pageable);
        } else {
            page = enrollmentRepository.findAll(pageable);
        }
        return page.map(EnrollmentResponse::fromEntity);
    }

    @Override
    @Transactional
    public EnrollmentResponse updateEnrollment(Long id, UpdateEnrollmentRequest request) {
        log.info("Updating enrollment with ID: {}", id);

        CourseEnrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course enrollment not found with ID: " + id));

        enrollment.setAcademicYear(request.getAcademicYear().trim());
        enrollment.setSemester(request.getSemester());
        enrollment.setStatus(request.getStatus());

        CourseEnrollment updated = enrollmentRepository.save(enrollment);
        log.info("Enrollment successfully updated with ID: {}", updated.getId());

        return EnrollmentResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteEnrollment(Long id) {
        log.info("Deleting enrollment with ID: {}", id);
        CourseEnrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course enrollment not found with ID: " + id));
        enrollmentRepository.delete(enrollment);
        log.info("Enrollment successfully deleted with ID: {}", id);
    }
}
