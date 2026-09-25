package com.smartcampus.attendance.service;

import com.smartcampus.attendance.dto.request.CreateAttendanceSessionRequest;
import com.smartcampus.attendance.dto.request.UpdateAttendanceSessionRequest;
import com.smartcampus.attendance.dto.response.AttendanceSessionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AttendanceSessionService {

    AttendanceSessionResponse createSession(CreateAttendanceSessionRequest request, String bearerToken);

    AttendanceSessionResponse getSessionById(Long id);

    Page<AttendanceSessionResponse> listSessions(Long subjectId, Long facultyUserId, LocalDate sessionDate,
                                                String academicYear, Integer semester, String section, Pageable pageable);

    AttendanceSessionResponse updateSession(Long id, UpdateAttendanceSessionRequest request, String bearerToken);

    void deleteSession(Long id);
}
