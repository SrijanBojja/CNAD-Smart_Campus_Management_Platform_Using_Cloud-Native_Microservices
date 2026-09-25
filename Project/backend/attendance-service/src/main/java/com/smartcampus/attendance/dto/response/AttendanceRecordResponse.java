package com.smartcampus.attendance.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartcampus.attendance.entity.AttendanceRecord;
import com.smartcampus.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Attendance record details response")
public class AttendanceRecordResponse {

    @Schema(description = "Attendance record ID", example = "1")
    private Long id;

    @Schema(description = "Attendance session ID", example = "1")
    private Long sessionId;

    @Schema(description = "Student ID referenced from Student Service", example = "50")
    private Long studentId;

    @Schema(description = "Attendance status", example = "PRESENT")
    private AttendanceStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Timestamp when attendance was marked")
    private LocalDateTime markedAt;

    @Schema(description = "Remarks or reason for absence/leave", example = "On-time arrival")
    private String remarks;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Record last update timestamp")
    private LocalDateTime updatedAt;

    public AttendanceRecordResponse() {
    }

    public static AttendanceRecordResponse fromEntity(AttendanceRecord entity) {
        if (entity == null) {
            return null;
        }
        AttendanceRecordResponse dto = new AttendanceRecordResponse();
        dto.setId(entity.getId());
        dto.setSessionId(entity.getSession() != null ? entity.getSession().getId() : null);
        dto.setStudentId(entity.getStudentId());
        dto.setStatus(entity.getStatus());
        dto.setMarkedAt(entity.getMarkedAt());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
