package com.smartcampus.attendance.service.impl;

import com.smartcampus.attendance.client.AcademicServiceClient;
import com.smartcampus.attendance.client.AuthServiceClient;
import com.smartcampus.attendance.client.dto.SubjectResponseDto;
import com.smartcampus.attendance.client.dto.UserValidationResponseDto;
import com.smartcampus.attendance.dto.request.CreateAttendanceSessionRequest;
import com.smartcampus.attendance.dto.request.UpdateAttendanceSessionRequest;
import com.smartcampus.attendance.dto.response.AttendanceSessionResponse;
import com.smartcampus.attendance.entity.AttendanceSession;
import com.smartcampus.attendance.exception.BadRequestException;
import com.smartcampus.attendance.exception.ResourceNotFoundException;
import com.smartcampus.attendance.repository.AttendanceSessionRepository;
import com.smartcampus.attendance.service.AttendanceSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AttendanceSessionServiceImpl implements AttendanceSessionService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceSessionServiceImpl.class);

    private final AttendanceSessionRepository sessionRepository;
    private final AcademicServiceClient academicServiceClient;
    private final AuthServiceClient authServiceClient;

    public AttendanceSessionServiceImpl(AttendanceSessionRepository sessionRepository,
                                        AcademicServiceClient academicServiceClient,
                                        AuthServiceClient authServiceClient) {
        this.sessionRepository = sessionRepository;
        this.academicServiceClient = academicServiceClient;
        this.authServiceClient = authServiceClient;
    }

    @Override
    @Transactional
    public AttendanceSessionResponse createSession(CreateAttendanceSessionRequest request, String bearerToken) {
        log.info("Attempting to create attendance session for subjectId: {}, facultyUserId: {}, date: {}",
                request.getSubjectId(), request.getFacultyUserId(), request.getSessionDate());

        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        // Cross-service validation: verify subject exists in Academic Service
        SubjectResponseDto subject = academicServiceClient.getSubjectById(request.getSubjectId(), bearerToken);
        if (subject == null) {
            throw new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId());
        }

        // Cross-service validation: verify faculty user in Auth Service
        UserValidationResponseDto facultyUser = authServiceClient.validateUser(request.getFacultyUserId(), "FACULTY", bearerToken);
        if (facultyUser == null || !facultyUser.isActive() || !facultyUser.isHasRequiredRole()) {
            throw new BadRequestException("Faculty user with ID " + request.getFacultyUserId() + " is invalid, inactive, or lacks FACULTY role");
        }

        AttendanceSession session = new AttendanceSession(
                request.getSubjectId(),
                request.getFacultyUserId(),
                request.getSessionDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getRoomNumber() != null ? request.getRoomNumber().trim() : null,
                request.getAcademicYear().trim(),
                request.getSemester(),
                request.getSection() != null ? request.getSection().trim() : null
        );

        AttendanceSession saved = sessionRepository.save(session);
        log.info("Attendance session successfully created with ID: {}", saved.getId());

        return AttendanceSessionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSessionResponse getSessionById(Long id) {
        log.debug("Retrieving attendance session by ID: {}", id);
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance session not found with ID: " + id));
        return AttendanceSessionResponse.fromEntity(session);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceSessionResponse> listSessions(Long subjectId, Long facultyUserId, LocalDate sessionDate,
                                                       String academicYear, Integer semester, String section, Pageable pageable) {
        log.debug("Listing attendance sessions with subjectId: {}, facultyUserId: {}, sessionDate: {}, academicYear: {}, semester: {}, section: {}",
                subjectId, facultyUserId, sessionDate, academicYear, semester, section);

        Specification<AttendanceSession> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (subjectId != null) {
                predicates.add(cb.equal(root.get("subjectId"), subjectId));
            }
            if (facultyUserId != null) {
                predicates.add(cb.equal(root.get("facultyUserId"), facultyUserId));
            }
            if (sessionDate != null) {
                predicates.add(cb.equal(root.get("sessionDate"), sessionDate));
            }
            if (academicYear != null && !academicYear.isBlank()) {
                predicates.add(cb.equal(root.get("academicYear"), academicYear.trim()));
            }
            if (semester != null) {
                predicates.add(cb.equal(root.get("semester"), semester));
            }
            if (section != null && !section.isBlank()) {
                predicates.add(cb.equal(root.get("section"), section.trim()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<AttendanceSession> page = sessionRepository.findAll(spec, pageable);
        return page.map(AttendanceSessionResponse::fromEntity);
    }

    @Override
    @Transactional
    public AttendanceSessionResponse updateSession(Long id, UpdateAttendanceSessionRequest request, String bearerToken) {
        log.info("Updating attendance session with ID: {}", id);

        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance session not found with ID: " + id));

        // Cross-service validation: verify faculty user in Auth Service
        UserValidationResponseDto facultyUser = authServiceClient.validateUser(request.getFacultyUserId(), "FACULTY", bearerToken);
        if (facultyUser == null || !facultyUser.isActive() || !facultyUser.isHasRequiredRole()) {
            throw new BadRequestException("Faculty user with ID " + request.getFacultyUserId() + " is invalid, inactive, or lacks FACULTY role");
        }

        session.setFacultyUserId(request.getFacultyUserId());
        session.setSessionDate(request.getSessionDate());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setRoomNumber(request.getRoomNumber() != null ? request.getRoomNumber().trim() : null);
        session.setAcademicYear(request.getAcademicYear().trim());
        session.setSemester(request.getSemester());
        session.setSection(request.getSection() != null ? request.getSection().trim() : null);

        AttendanceSession updated = sessionRepository.save(session);
        log.info("Attendance session successfully updated with ID: {}", updated.getId());

        return AttendanceSessionResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteSession(Long id) {
        log.info("Deleting attendance session with ID: {}", id);
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance session not found with ID: " + id));
        sessionRepository.delete(session);
        log.info("Attendance session successfully deleted with ID: {}", id);
    }
}
