package com.smartcampus.academic.service.impl;

import com.smartcampus.academic.client.StudentServiceClient;
import com.smartcampus.academic.client.dto.StudentResponseDto;
import com.smartcampus.academic.dto.request.CreateAcademicRecordRequest;
import com.smartcampus.academic.dto.request.UpdateAcademicRecordRequest;
import com.smartcampus.academic.dto.response.AcademicRecordResponse;
import com.smartcampus.academic.entity.AcademicRecord;
import com.smartcampus.academic.entity.Subject;
import com.smartcampus.academic.exception.DuplicateResourceException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.repository.AcademicRecordRepository;
import com.smartcampus.academic.repository.SubjectRepository;
import com.smartcampus.academic.security.UserPrincipal;
import com.smartcampus.academic.service.AcademicRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicRecordServiceImpl implements AcademicRecordService {

    private static final Logger log = LoggerFactory.getLogger(AcademicRecordServiceImpl.class);

    private final AcademicRecordRepository academicRecordRepository;
    private final SubjectRepository subjectRepository;
    private final StudentServiceClient studentServiceClient;

    public AcademicRecordServiceImpl(AcademicRecordRepository academicRecordRepository,
                                     SubjectRepository subjectRepository,
                                     StudentServiceClient studentServiceClient) {
        this.academicRecordRepository = academicRecordRepository;
        this.subjectRepository = subjectRepository;
        this.studentServiceClient = studentServiceClient;
    }

    @Override
    @Transactional
    public AcademicRecordResponse createAcademicRecord(CreateAcademicRecordRequest request, String bearerToken) {
        log.info("Attempting to create academic record for studentId: {}, subjectId: {}, academicYear: {}, semester: {}",
                request.getStudentId(), request.getSubjectId(), request.getAcademicYear(), request.getSemester());

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));

        // Cross-service validation: verify student exists in Student Service
        StudentResponseDto student = studentServiceClient.getStudentById(request.getStudentId(), bearerToken);
        if (student == null) {
            throw new ResourceNotFoundException("Student not found with ID: " + request.getStudentId());
        }

        if (academicRecordRepository.existsByStudentIdAndSubjectIdAndAcademicYearAndSemester(
                request.getStudentId(), request.getSubjectId(), request.getAcademicYear().trim(), request.getSemester())) {
            log.warn("Academic record creation failed: duplicate record for studentId {} in subjectId {}",
                    request.getStudentId(), request.getSubjectId());
            throw new DuplicateResourceException("Academic record already exists for this student, subject, academic year, and semester");
        }

        AcademicRecord record = new AcademicRecord(
                request.getStudentId(),
                subject,
                request.getAcademicYear().trim(),
                request.getSemester(),
                request.getInternalMarks(),
                request.getExternalMarks(),
                null,
                request.getGrade(),
                request.getGradePoint(),
                request.getResultStatus()
        );

        AcademicRecord saved = academicRecordRepository.save(record);
        log.info("Academic record successfully created with ID: {}", saved.getId());

        return AcademicRecordResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicRecordResponse getAcademicRecordById(Long id, UserPrincipal principal, String bearerToken) {
        log.debug("Retrieving academic record by ID: {}", id);

        AcademicRecord record = academicRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic record not found with ID: " + id));

        // Object-level authorization for STUDENT role
        if (principal != null) {
            boolean isAdminOrFaculty = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_FACULTY".equals(role));

            if (!isAdminOrFaculty) {
                // Caller is a STUDENT
                StudentResponseDto student = studentServiceClient.getStudentById(record.getStudentId(), bearerToken);
                if (student == null || !student.getUserId().equals(principal.getUserId())) {
                    log.warn("Access denied for student userId {} attempting to access record ID {} of studentId {}",
                            principal.getUserId(), record.getId(), record.getStudentId());
                    throw new AccessDeniedException("Access denied: You are only permitted to access your own academic record");
                }
            }
        }

        return AcademicRecordResponse.fromEntity(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AcademicRecordResponse> listAcademicRecords(Long studentId, Long subjectId, Integer semester, String academicYear, Pageable pageable) {
        log.debug("Listing academic records with studentId: {}, subjectId: {}, semester: {}, academicYear: {}",
                studentId, subjectId, semester, academicYear);
        Page<AcademicRecord> page;
        if (studentId != null && semester != null) {
            page = academicRecordRepository.findByStudentIdAndSemester(studentId, semester, pageable);
        } else if (studentId != null) {
            page = academicRecordRepository.findByStudentId(studentId, pageable);
        } else if (subjectId != null) {
            page = academicRecordRepository.findBySubjectId(subjectId, pageable);
        } else {
            page = academicRecordRepository.findAll(pageable);
        }
        return page.map(AcademicRecordResponse::fromEntity);
    }

    @Override
    @Transactional
    public AcademicRecordResponse updateAcademicRecord(Long id, UpdateAcademicRecordRequest request) {
        log.info("Updating academic record with ID: {}", id);

        AcademicRecord record = academicRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic record not found with ID: " + id));

        if (request.getInternalMarks() != null) {
            record.setInternalMarks(request.getInternalMarks());
        }
        if (request.getExternalMarks() != null) {
            record.setExternalMarks(request.getExternalMarks());
        }
        if (request.getGrade() != null) {
            record.setGrade(request.getGrade());
        }
        if (request.getGradePoint() != null) {
            record.setGradePoint(request.getGradePoint());
        }
        if (request.getResultStatus() != null) {
            record.setResultStatus(request.getResultStatus());
        }

        AcademicRecord updated = academicRecordRepository.save(record);
        log.info("Academic record successfully updated with ID: {}", updated.getId());

        return AcademicRecordResponse.fromEntity(updated);
    }
}
