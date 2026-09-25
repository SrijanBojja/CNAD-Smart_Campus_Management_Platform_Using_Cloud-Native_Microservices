package com.smartcampus.attendance.service;

import com.smartcampus.attendance.dto.request.CreateAttendanceRecordRequest;
import com.smartcampus.attendance.dto.request.UpdateAttendanceRecordRequest;
import com.smartcampus.attendance.dto.response.AttendanceRecordResponse;
import com.smartcampus.attendance.entity.AttendanceStatus;
import com.smartcampus.attendance.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AttendanceRecordService {

    AttendanceRecordResponse createRecord(CreateAttendanceRecordRequest request, String bearerToken);

    AttendanceRecordResponse getRecordById(Long id, UserPrincipal principal, String bearerToken);

    Page<AttendanceRecordResponse> listRecords(Long sessionId, Long studentId, AttendanceStatus status, Pageable pageable);

    AttendanceRecordResponse updateRecord(Long id, UpdateAttendanceRecordRequest request);

    void deleteRecord(Long id);
}
