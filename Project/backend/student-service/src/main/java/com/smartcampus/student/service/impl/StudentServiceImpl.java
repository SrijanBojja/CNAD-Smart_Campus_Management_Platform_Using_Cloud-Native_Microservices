package com.smartcampus.student.service.impl;

import com.smartcampus.student.client.AuthServiceClient;
import com.smartcampus.student.client.dto.UserValidationResponse;
import com.smartcampus.student.dto.request.CreateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentStatusRequest;
import com.smartcampus.student.dto.response.StudentResponse;
import com.smartcampus.student.entity.Student;
import com.smartcampus.student.entity.StudentStatus;
import com.smartcampus.student.exception.BadRequestException;
import com.smartcampus.student.exception.DuplicateResourceException;
import com.smartcampus.student.exception.ResourceNotFoundException;
import com.smartcampus.student.exception.UserValidationFailedException;
import com.smartcampus.student.repository.StudentRepository;
import com.smartcampus.student.security.UserPrincipal;
import com.smartcampus.student.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceImpl.class);

    private final StudentRepository studentRepository;
    private final AuthServiceClient authServiceClient;

    public StudentServiceImpl(StudentRepository studentRepository, AuthServiceClient authServiceClient) {
        this.studentRepository = studentRepository;
        this.authServiceClient = authServiceClient;
    }

    @Override
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request, String bearerToken) {
        log.info("Attempting to create student profile for userId: {}, studentNumber: {}",
                request.getUserId(), request.getStudentNumber());

        if (studentRepository.existsByUserId(request.getUserId())) {
            log.warn("Student profile creation failed: userId {} already has a student record", request.getUserId());
            throw new DuplicateResourceException("Student profile already exists for user ID: " + request.getUserId());
        }

        if (studentRepository.existsByStudentNumber(request.getStudentNumber())) {
            log.warn("Student profile creation failed: studentNumber '{}' already exists", request.getStudentNumber());
            throw new DuplicateResourceException("Student number '" + request.getStudentNumber() + "' is already registered");
        }

        // Validate user existence, active status, and STUDENT role via Auth Service
        UserValidationResponse validation = authServiceClient.validateUser(request.getUserId(), "STUDENT", bearerToken);

        if (validation == null) {
            log.error("Auth validation returned null response for userId: {}", request.getUserId());
            throw new UserValidationFailedException("Authentication service failed to validate user identity");
        }

        if (!validation.isActive()) {
            log.warn("Auth user ID {} is not active. Rejecting student creation.", request.getUserId());
            throw new UserValidationFailedException("Cannot create student profile for inactive Auth user ID: " + request.getUserId());
        }

        if (!validation.isHasRequiredRole()) {
            log.warn("Auth user ID {} does not possess the STUDENT role. Rejecting student creation.", request.getUserId());
            throw new UserValidationFailedException("Auth user ID " + request.getUserId() + " does not have the required STUDENT role");
        }

        Student student = new Student(
                request.getUserId(),
                request.getStudentNumber().trim(),
                request.getFirstName().trim(),
                request.getLastName().trim(),
                request.getDateOfBirth(),
                request.getPhone(),
                request.getDepartment(),
                request.getProgram(),
                request.getYearOfStudy(),
                request.getSection(),
                StudentStatus.ACTIVE
        );

        Student saved = studentRepository.save(student);
        log.info("Student profile successfully created with ID: {}, userId: {}, studentNumber: {}",
                saved.getId(), saved.getUserId(), saved.getStudentNumber());

        return StudentResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id, UserPrincipal principal) {
        log.debug("Retrieving student profile by ID: {}", id);

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        // Object-level authorization: STUDENT role can only view their own student record
        if (principal != null) {
            boolean isAdminOrFaculty = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_FACULTY".equals(role));

            if (!isAdminOrFaculty) {
                // Caller is a STUDENT (or other non-privileged user)
                Long authenticatedUserId = principal.getUserId();
                if (authenticatedUserId == null || !student.getUserId().equals(authenticatedUserId)) {
                    log.warn("Access denied for caller userId {} attempting to access student record {} belonging to userId {}",
                            authenticatedUserId, student.getId(), student.getUserId());
                    throw new AccessDeniedException("Access denied: You are only permitted to access your own student profile");
                }
            }
        }

        return StudentResponse.fromEntity(student);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentResponse> listStudents(Pageable pageable) {
        log.debug("Listing students page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return studentRepository.findAll(pageable).map(StudentResponse::fromEntity);
    }

    @Override
    @Transactional
    public StudentResponse updateStudent(Long id, UpdateStudentRequest request) {
        log.info("Updating student profile for ID: {}", id);

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        student.setFirstName(request.getFirstName().trim());
        student.setLastName(request.getLastName().trim());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setPhone(request.getPhone());
        student.setDepartment(request.getDepartment());
        student.setProgram(request.getProgram());
        student.setYearOfStudy(request.getYearOfStudy());
        student.setSection(request.getSection());

        Student updated = studentRepository.save(student);
        log.info("Student profile successfully updated for ID: {}", updated.getId());

        return StudentResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public StudentResponse updateStudentStatus(Long id, UpdateStudentStatusRequest request) {
        log.info("Updating student status for ID: {} to {}", id, request.getStatus());

        if (request.getStatus() == null) {
            throw new BadRequestException("Status is required");
        }

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        student.setStatus(request.getStatus());

        Student updated = studentRepository.save(student);
        log.info("Student status successfully updated for ID: {} to {}", updated.getId(), updated.getStatus());

        return StudentResponse.fromEntity(updated);
    }
}
