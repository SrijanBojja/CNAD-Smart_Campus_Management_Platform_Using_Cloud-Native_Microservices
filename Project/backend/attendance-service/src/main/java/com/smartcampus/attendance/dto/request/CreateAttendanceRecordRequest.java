package com.smartcampus.attendance.dto.request;

import com.smartcampus.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for recording student attendance in a session")
public class CreateAttendanceRecordRequest {

    @NotNull(message = "sessionId is required")
    @Positive(message = "sessionId must be positive")
    @Schema(description = "Attendance session ID", example = "1")
    private Long sessionId;

    @NotNull(message = "studentId is required")
    @Positive(message = "studentId must be positive")
    @Schema(description = "Student ID referenced from Student Service", example = "50")
    private Long studentId;

    @NotNull(message = "status is required")
    @Schema(description = "Attendance status", example = "PRESENT")
    private AttendanceStatus status;

    @Size(max = 255, message = "remarks cannot exceed 255 characters")
    @Schema(description = "Optional remarks or reason for absence", example = "On-time arrival")
    private String remarks;

    public CreateAttendanceRecordRequest() {
    }

    public CreateAttendanceRecordRequest(Long sessionId, Long studentId, AttendanceStatus status, String remarks) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.status = status;
        this.remarks = remarks;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
