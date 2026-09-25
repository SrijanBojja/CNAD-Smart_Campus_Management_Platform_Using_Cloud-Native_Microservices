package com.smartcampus.attendance.service.impl;

import com.smartcampus.attendance.client.StudentServiceClient;
import com.smartcampus.attendance.client.dto.StudentResponseDto;
import com.smartcampus.attendance.dto.request.CreateAttendanceRecordRequest;
import com.smartcampus.attendance.dto.request.UpdateAttendanceRecordRequest;
import com.smartcampus.attendance.dto.response.AttendanceRecordResponse;
import com.smartcampus.attendance.entity.AttendanceRecord;
import com.smartcampus.attendance.entity.AttendanceSession;
import com.smartcampus.attendance.entity.AttendanceStatus;
import com.smartcampus.attendance.exception.DuplicateResourceException;
import com.smartcampus.attendance.exception.ResourceNotFoundException;
import com.smartcampus.attendance.repository.AttendanceRecordRepository;
import com.smartcampus.attendance.repository.AttendanceSessionRepository;
import com.smartcampus.attendance.security.UserPrincipal;
import com.smartcampus.attendance.service.AttendanceRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceRecordServiceImpl implements AttendanceRecordService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceRecordServiceImpl.class);

    private final AttendanceRecordRepository recordRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final StudentServiceClient studentServiceClient;

    public AttendanceRecordServiceImpl(AttendanceRecordRepository recordRepository,
                                       AttendanceSessionRepository sessionRepository,
                                       StudentServiceClient studentServiceClient) {
        this.recordRepository = recordRepository;
        this.sessionRepository = sessionRepository;
        this.studentServiceClient = studentServiceClient;
    }

    @Override
    @Transactional
    public AttendanceRecordResponse createRecord(CreateAttendanceRecordRequest request, String bearerToken) {
        log.info("Attempting to create attendance record for sessionId: {}, studentId: {}, status: {}",
                request.getSessionId(), request.getStudentId(), request.getStatus());

        AttendanceSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Attendance session not found with ID: " + request.getSessionId()));

        // Cross-service validation: verify student exists in Student Service
        StudentResponseDto student = studentServiceClient.getStudentById(request.getStudentId(), bearerToken);
        if (student == null) {
            throw new ResourceNotFoundException("Student not found with ID: " + request.getStudentId());
        }

        if (recordRepository.existsBySessionIdAndStudentId(request.getSessionId(), request.getStudentId())) {
            log.warn("Attendance record creation failed: duplicate record for studentId {} in sessionId {}",
                    request.getStudentId(), request.getSessionId());
            throw new DuplicateResourceException("Attendance record already exists for student ID "
                    + request.getStudentId() + " in session ID " + request.getSessionId());
        }

        AttendanceRecord record = new AttendanceRecord(
                session,
                request.getStudentId(),
                request.getStatus(),
                request.getRemarks() != null ? request.getRemarks().trim() : null
        );

        AttendanceRecord saved = recordRepository.save(record);
        log.info("Attendance record successfully created with ID: {}", saved.getId());

        return AttendanceRecordResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceRecordResponse getRecordById(Long id, UserPrincipal principal, String bearerToken) {
        log.debug("Retrieving attendance record by ID: {}", id);

        AttendanceRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with ID: " + id));

        // Object-level authorization for STUDENT role
        if (principal != null) {
            boolean isAdminOrFaculty = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_FACULTY".equals(role));

            if (!isAdminOrFaculty) {
                // Caller is a STUDENT - verify ownership through Student Service
                StudentResponseDto student = studentServiceClient.getStudentById(record.getStudentId(), bearerToken);
                if (student == null || !student.getUserId().equals(principal.getUserId())) {
                    log.warn("Access denied for student userId {} attempting to access attendance record ID {} of studentId {}",
                            principal.getUserId(), record.getId(), record.getStudentId());
                    throw new AccessDeniedException("Access denied: You are only permitted to access your own attendance record");
                }
            }
        }

        return AttendanceRecordResponse.fromEntity(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceRecordResponse> listRecords(Long sessionId, Long studentId, AttendanceStatus status, Pageable pageable) {
        log.debug("Listing attendance records with sessionId: {}, studentId: {}, status: {}", sessionId, studentId, status);

        Specification<AttendanceRecord> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (sessionId != null) {
                predicates.add(cb.equal(root.get("session").get("id"), sessionId));
            }
            if (studentId != null) {
                predicates.add(cb.equal(root.get("studentId"), studentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<AttendanceRecord> page = recordRepository.findAll(spec, pageable);
        return page.map(AttendanceRecordResponse::fromEntity);
    }

    @Override
    @Transactional
    public AttendanceRecordResponse updateRecord(Long id, UpdateAttendanceRecordRequest request) {
        log.info("Updating attendance record with ID: {}", id);

        AttendanceRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with ID: " + id));

        record.setStatus(request.getStatus());
        record.setRemarks(request.getRemarks() != null ? request.getRemarks().trim() : null);

        AttendanceRecord updated = recordRepository.save(record);
        log.info("Attendance record successfully updated with ID: {}", updated.getId());

        return AttendanceRecordResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteRecord(Long id) {
        log.info("Deleting attendance record with ID: {}", id);
        AttendanceRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with ID: " + id));
        recordRepository.delete(record);
        log.info("Attendance record successfully deleted with ID: {}", id);
    }
}
