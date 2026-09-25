package com.smartcampus.facility.dto.request;

import com.smartcampus.facility.entity.MaintenanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to update maintenance status")
public class UpdateMaintenanceStatusRequest {

    @NotNull(message = "Maintenance status is required")
    @Schema(description = "New maintenance status", example = "RESOLVED")
    private MaintenanceStatus status;

    @Schema(description = "Assigned technician user ID", example = "15")
    private Long assignedToUserId;

    @Size(max = 2000, message = "Resolution notes must not exceed 2000 characters")
    @Schema(description = "Resolution notes when resolving or cancelling", example = "Lamp replaced and tested successfully.")
    private String resolutionNotes;

    public UpdateMaintenanceStatusRequest() {
    }

    public UpdateMaintenanceStatusRequest(MaintenanceStatus status, Long assignedToUserId, String resolutionNotes) {
        this.status = status;
        this.assignedToUserId = assignedToUserId;
        this.resolutionNotes = resolutionNotes;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
    }

    public Long getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
