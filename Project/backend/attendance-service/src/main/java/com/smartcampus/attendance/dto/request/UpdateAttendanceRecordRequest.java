package com.smartcampus.attendance.dto.request;

import com.smartcampus.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for updating an attendance record")
public class UpdateAttendanceRecordRequest {

    @NotNull(message = "status is required")
    @Schema(description = "Attendance status", example = "EXCUSED")
    private AttendanceStatus status;

    @Size(max = 255, message = "remarks cannot exceed 255 characters")
    @Schema(description = "Updated remarks or reason", example = "Medical leave approved")
    private String remarks;

    public UpdateAttendanceRecordRequest() {
    }

    public UpdateAttendanceRecordRequest(AttendanceStatus status, String remarks) {
        this.status = status;
        this.remarks = remarks;
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
