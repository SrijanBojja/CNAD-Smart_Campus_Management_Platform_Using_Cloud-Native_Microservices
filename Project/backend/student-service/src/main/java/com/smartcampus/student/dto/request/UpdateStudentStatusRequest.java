package com.smartcampus.student.dto.request;

import com.smartcampus.student.entity.StudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for updating student lifecycle status")
public class UpdateStudentStatusRequest {

    @Schema(description = "Updated student status (ACTIVE, INACTIVE, SUSPENDED, GRADUATED)", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "status is required")
    private StudentStatus status;

    public UpdateStudentStatusRequest() {
    }

    public UpdateStudentStatusRequest(StudentStatus status) {
        this.status = status;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }
}
